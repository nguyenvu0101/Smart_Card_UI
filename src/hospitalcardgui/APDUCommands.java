package hospitalcardgui;

import javax.smartcardio.*;
import java.nio.ByteBuffer;

public class APDUCommands {

    private final CardChannel channel;

    // CLA phải trùng với applet
  public static final byte CLA_APPLET = (byte)0xB0; 

    // Lệnh PIN
    private static final byte INS_VERIFY_PIN     = 0x20;
    private static final byte INS_CHANGE_PIN     = 0x21;
    private static final byte INS_SET_PIN        = 0x22; // Admin đặt PIN mới

    // Lệnh ví
    private static final byte INS_GET_BALANCE    = 0x30;
    private static final byte INS_CREDIT         = 0x31;
    private static final byte INS_DEBIT          = 0x32;

    // Lệnh hồ sơ bệnh nhân
    private static final byte INS_SET_PATIENT_ID = 0x40;
    private static final byte INS_GET_PATIENT_ID = 0x41;
    private static final byte INS_SET_PROFILE    = 0x42;

    // Lệnh RSA (MỚI THÊM)
    public static final byte INS_SET_RSA_KEY    = 0x50;
    public static final byte INS_SIGN_CHALLENGE = 0x51;

    public APDUCommands(CardChannel channel) {
        this.channel = channel;
    }

    /* ================== HÀM GỬI CHUNG ================== */

    private ResponseAPDU send(byte ins, byte p1, byte p2,
                              byte[] data, int le) throws CardException {
        CommandAPDU cmd;
        if (data != null) {
            if (le >= 0) {
                cmd = new CommandAPDU(CLA_APPLET, ins, p1, p2, data, le);
            } else {
                cmd = new CommandAPDU(CLA_APPLET, ins, p1, p2, data);
            }
        } else {
            if (le >= 0) {
                cmd = new CommandAPDU(CLA_APPLET, ins, p1, p2, le);
            } else {
                cmd = new CommandAPDU(CLA_APPLET, ins, p1, p2);
            }
        }
        return channel.transmit(cmd);
    }

    /* ================== PIN & XÁC THỰC ================== */

    public boolean verifyPIN(String pin) {
        try {
            ResponseAPDU resp = send(INS_VERIFY_PIN, (byte)0, (byte)0,
                    pin.getBytes(), -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean changePIN(String oldPinIgnored, String newPin) {
        try {
            // Applet chỉ cần PIN mới vì đã verify trước đó
            ResponseAPDU resp = send(INS_CHANGE_PIN, (byte)0, (byte)0,
                    newPin.getBytes(), -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Admin đặt PIN mới trực tiếp (không cần verify)
    public boolean setAdminPin(String pinStr) {
        try {
            byte[] data = pinStr.getBytes();
            ResponseAPDU resp = send(INS_SET_PIN, (byte)0, (byte)0, data, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* ================== VÍ / SỐ DƯ ================== */

    public int getBalance() {
        try {
            ResponseAPDU resp = send(INS_GET_BALANCE,
                    (byte)0, (byte)0, null, 2);
            if (resp.getSW() != 0x9000) return -1;
            byte[] data = resp.getData();
            short val = ByteBuffer.wrap(data).getShort();
            return val & 0xFFFF;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean credit(int amount) {
        try {
            byte[] amt = ByteBuffer.allocate(2)
                    .putShort((short)amount).array();
            ResponseAPDU resp = send(INS_CREDIT, (byte)0, (byte)0, amt, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean debit(int amount) {
        try {
            byte[] amt = ByteBuffer.allocate(2)
                    .putShort((short)amount).array();
            ResponseAPDU resp = send(INS_DEBIT, (byte)0, (byte)0, amt, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* ================== PATIENT_ID ================== */

    public boolean setPatientId(String pid) {
        try {
            byte[] data = pid.getBytes();
            ResponseAPDU resp = send(INS_SET_PATIENT_ID,
                    (byte)0, (byte)0, data, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getPatientId() {
        try {
            ResponseAPDU resp = send(INS_GET_PATIENT_ID,
                    (byte)0, (byte)0, null, 20);
            if (resp.getSW() != 0x9000) return null;
            byte[] data = resp.getData();
            return new String(data).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* ================== HỒ SƠ TÓM TẮT ================== */

    public boolean setProfile(String fullName,
                              String dob,
                              String bloodType,
                              String allergies,
                              String chronic,
                              String healthId) {
        try {
            byte[] nameBytes  = safeBytes(fullName);
            byte[] dobBytes   = safeBytes(dob);
            byte[] bloodBytes = safeBytes(bloodType);
            byte[] algBytes   = safeBytes(allergies);
            byte[] chrBytes   = safeBytes(chronic);
            byte[] hidBytes   = safeBytes(healthId);

            int totalLen = 1 + nameBytes.length
                         + 1 + dobBytes.length
                         + 1 + bloodBytes.length
                         + 1 + algBytes.length
                         + 1 + chrBytes.length
                         + 1 + hidBytes.length;

            byte[] data = new byte[totalLen];
            int offset = 0;

            data[offset++] = (byte) nameBytes.length;
            System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
            offset += nameBytes.length;

            data[offset++] = (byte) dobBytes.length;
            System.arraycopy(dobBytes, 0, data, offset, dobBytes.length);
            offset += dobBytes.length;

            data[offset++] = (byte) bloodBytes.length;
            System.arraycopy(bloodBytes, 0, data, offset, bloodBytes.length);
            offset += bloodBytes.length;

            data[offset++] = (byte) algBytes.length;
            System.arraycopy(algBytes, 0, data, offset, algBytes.length);
            offset += algBytes.length;

            data[offset++] = (byte) chrBytes.length;
            System.arraycopy(chrBytes, 0, data, offset, chrBytes.length);
            offset += chrBytes.length;

            data[offset++] = (byte) hidBytes.length;
            System.arraycopy(hidBytes, 0, data, offset, hidBytes.length);
            offset += hidBytes.length;

            ResponseAPDU resp = send(INS_SET_PROFILE,
                    (byte)0, (byte)0, data, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* ================== RSA SECURITY (MỚI) ================== */

    /**
     * Gửi Private Key xuống thẻ (gọi từ CardIssuePanel)
     * data format: [LenMod][Modulus...][LenExp][Exponent...]
     */
    public boolean setRsaPrivateKey(byte[] keyData) {
        try {
            // INS_SET_RSA_KEY = 0x50
            ResponseAPDU resp = send(INS_SET_RSA_KEY, (byte)0, (byte)0, keyData, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gửi Challenge để thẻ ký xác thực
     * Trả về chữ ký (byte array) hoặc null nếu lỗi
     */
    public byte[] signChallenge(byte[] challenge) {
        try {
            // INS_SIGN_CHALLENGE = 0x51
            // Giả sử chữ ký RSA 1024 bit dài 128 bytes
            ResponseAPDU resp = send(INS_SIGN_CHALLENGE, (byte)0, (byte)0, challenge, 128);
            
            if (resp.getSW() == 0x9000) {
                return resp.getData();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] safeBytes(String s) {
        if (s == null) return new byte[0];
        return s.getBytes();
    }
}
