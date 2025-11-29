package hospitalcardgui;

import javax.smartcardio.*;
import java.nio.ByteBuffer;

public class APDUCommands {

    private final CardChannel channel;
    public static final byte CLA_APPLET = (byte)0x80; 

    // ================== BẢNG MÃ LỆNH (INS) ĐỒNG BỘ VỚI APPLET ==================
    
    // 1. Nhóm GHI (Admin)
    private static final byte INS_SET_SALT        = (byte)0x20;
    private static final byte INS_SET_PIN_HASH    = (byte)0x21; 
    private static final byte INS_SET_WRAP_USER   = (byte)0x22; 
    private static final byte INS_SET_WRAP_ADMIN  = (byte)0x23; 
    private static final byte INS_SET_PROFILE_ENC = (byte)0x24; 

    // 2. Nhóm ĐỌC & VERIFY (Client)
    private static final byte INS_GET_SALT        = (byte)0x25; 
    private static final byte INS_VERIFY_PIN_HASH = (byte)0x26; 
    private static final byte INS_GET_DATA_ENC    = (byte)0x27; 

    // 3. Nhóm TRẠNG THÁI THẺ (Cho chức năng đổi PIN lần đầu)
    private static final byte INS_GET_STATUS      = (byte)0x28;
    private static final byte INS_SET_STATUS      = (byte)0x29;

    // 4. Nhóm VÍ ĐIỆN TỬ
    private static final byte INS_GET_BALANCE     = (byte)0x30;
    private static final byte INS_CREDIT          = (byte)0x31;
    private static final byte INS_DEBIT           = (byte)0x32;

    // 5. Nhóm RSA
    public static final byte INS_SIGN_CHALLENGE   = (byte)0x51;
    public static final byte INS_GEN_RSA_KEYPAIR  = (byte)0x52; 
    public static final byte INS_SET_RSA_KEY      = (byte)0x50; // (Ít dùng)

    public APDUCommands(CardChannel channel) {
        this.channel = channel;
    }

    public ResponseAPDU transmit(CommandAPDU cmd) throws CardException {
        return channel.transmit(cmd);
    }
    
    // Hàm gửi lệnh có log debug
    private ResponseAPDU send(byte ins, byte p1, byte p2, byte[] data, int le) throws CardException {
        CommandAPDU cmd;
        if (data != null) {
            cmd = (le >= 0) ? new CommandAPDU(CLA_APPLET, ins, p1, p2, data, le)
                            : new CommandAPDU(CLA_APPLET, ins, p1, p2, data);
        } else {
            cmd = (le >= 0) ? new CommandAPDU(CLA_APPLET, ins, p1, p2, le)
                            : new CommandAPDU(CLA_APPLET, ins, p1, p2);
        }
        
        System.out.println("[APDU] Sending: " + bytesToHex(cmd.getBytes()));
        ResponseAPDU resp = channel.transmit(cmd);
        System.out.println("[APDU] Response SW: " + String.format("0x%04X", resp.getSW()));
        
        return resp;
    }

    // ================== CÁC HÀM BẢO MẬT & LOGIN ==================

    public byte[] getSalt() {
        try {
            System.out.println("[APDU] === GET_SALT (INS=0x25) ===");
            ResponseAPDU resp = send(INS_GET_SALT, (byte)0, (byte)0, null, 16);
            if (resp.getSW() == 0x9000) return resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean verifyPinHash(byte[] hash) {
        try {
            System.out.println("[APDU] === VERIFY_PIN_HASH (INS=0x26) ===");
            ResponseAPDU resp = send(INS_VERIFY_PIN_HASH, (byte)0, (byte)0, hash, -1);
            return resp.getSW() == 0x9000;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public int getPinTriesRemaining(byte[] hash) {
        try {
            ResponseAPDU resp = send(INS_VERIFY_PIN_HASH, (byte)0, (byte)0, hash, -1);
            int sw = resp.getSW();
            if ((sw & 0xFFF0) == 0x63C0) return sw & 0x0F;
            return -1;
        } catch (Exception e) { return -1; }
    }

    public byte[] getWrappedKey() {
        try {
            System.out.println("[APDU] === GET_WRAPPED_KEY (INS=0x27 P1=0x01) ===");
            ResponseAPDU resp = send(INS_GET_DATA_ENC, (byte)0x01, (byte)0, null, 32);
            if (resp.getSW() == 0x9000) return resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public byte[] getEncryptedProfile() {
        try {
            System.out.println("[APDU] === GET_ENCRYPTED_PROFILE (INS=0x27 P1=0x02) ===");
            ResponseAPDU resp = send(INS_GET_DATA_ENC, (byte)0x02, (byte)0, null, 255);
            if (resp.getSW() == 0x9000) return resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ================== QUẢN LÝ TRẠNG THÁI (LOGIN LẦN ĐẦU) ==================

    public int getCardStatus() {
        try {
            System.out.println("[APDU] === GET_CARD_STATUS (INS=0x28) ===");
            ResponseAPDU resp = send(INS_GET_STATUS, (byte)0, (byte)0, null, 1);
            if (resp.getSW() == 0x9000) {
                return resp.getData()[0]; // 1 = First Login, 0 = Normal
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public boolean disableFirstLogin() {
        try {
            System.out.println("[APDU] === DISABLE_FIRST_LOGIN (INS=0x29) ===");
            byte[] data = { 0x00 }; // Set status về 0
            return send(INS_SET_STATUS, (byte)0, (byte)0, data, -1).getSW() == 0x9000;
        } catch (Exception e) { return false; }
    }

    // ================== VÍ ĐIỆN TỬ ==================

    public int getBalance() {
        try {
            ResponseAPDU resp = send(INS_GET_BALANCE, (byte)0, (byte)0, null, 2);
            if (resp.getSW() == 0x9000) {
                return ByteBuffer.wrap(resp.getData()).getShort() & 0xFFFF;
            }
        } catch (Exception e) {}
        return -1;
    }
    
    public boolean credit(int amount) {
        try {
            byte[] amt = ByteBuffer.allocate(2).putShort((short)amount).array();
            return send(INS_CREDIT, (byte)0, (byte)0, amt, -1).getSW() == 0x9000;
        } catch (Exception e) { return false; }
    }

    public boolean debit(int amount) {
        try {
            byte[] amt = ByteBuffer.allocate(2).putShort((short)amount).array();
            return send(INS_DEBIT, (byte)0, (byte)0, amt, -1).getSW() == 0x9000;
        } catch (Exception e) { return false; }
    }

    // ================== RSA & ADMIN SETTERS ==================

    public byte[] generateRsaKeyPair() {
        try {
            ResponseAPDU resp = send(INS_GEN_RSA_KEYPAIR, (byte)0, (byte)0, null, 256);
            if (resp.getSW() == 0x9000) return resp.getData();
        } catch (Exception e) {}
        return null;
    }

    public byte[] signChallenge(byte[] ch) {
        try {
            ResponseAPDU resp = send(INS_SIGN_CHALLENGE, (byte)0, (byte)0, ch, 128);
            if (resp.getSW() == 0x9000) return resp.getData();
        } catch (Exception e) {}
        return null;
    }

    // Setter công khai để Client/Admin dùng
    public boolean setSalt(byte[] d) { return sendData(INS_SET_SALT, d); }
    public boolean setPinHash(byte[] d) { return sendData(INS_SET_PIN_HASH, d); }
    public boolean setWrappedKeyUser(byte[] d) { return sendData(INS_SET_WRAP_USER, d); }
    public boolean setWrappedKeyAdmin(byte[] d) { return sendData(INS_SET_WRAP_ADMIN, d); }
    public boolean setEncryptedProfile(byte[] d) { return sendData(INS_SET_PROFILE_ENC, d); }

    private boolean sendData(byte ins, byte[] data) {
        try { return send(ins, (byte)0, (byte)0, data, -1).getSW() == 0x9000; }
        catch (Exception e) { return false; }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
