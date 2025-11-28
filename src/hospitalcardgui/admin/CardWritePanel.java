package hospitalcardgui.admin;

import hospitalcardgui.APDUCommands;
import hospitalcardgui.CardManager;
import hospitalcardgui.DatabaseConnection;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class CardWritePanel extends JPanel {

    private JTextField txtPatientId;
    private JPasswordField txtPin; // PIN 6 số
    private JButton btnWrite;
    private JLabel lblStatus;
    private JTextArea txtInfoPreview;

    private final CardManager cardManager = CardManager.getInstance();

    private APDUCommands apdu;

    public CardWritePanel() {
        initUI();
    }

    private void initUI() {
        setBorder(BorderFactory.createTitledBorder("Ghi mã bệnh nhân, PIN & hồ sơ tóm tắt lên thẻ"));
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("Mã bệnh nhân (patient_id):"), c);
        c.gridx = 1;
        txtPatientId = new JTextField(15);
        add(txtPatientId, c);
        row++;

        c.gridx = 0; c.gridy = row;
        add(new JLabel("PIN cấp cho thẻ (6 số):"), c);
        c.gridx = 1;
        txtPin = new JPasswordField(15);
        add(txtPin, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        btnWrite = new JButton("Kết nối & Ghi thẻ");
        btnWrite.addActionListener(e -> onWrite());
        add(btnWrite, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        lblStatus = new JLabel("Chưa kết nối thẻ.");
        lblStatus.setForeground(Color.RED);
        add(lblStatus, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        txtInfoPreview = new JTextArea(8, 40);
        txtInfoPreview.setEditable(false);
        txtInfoPreview.setBorder(BorderFactory.createTitledBorder("Thông tin sẽ ghi lên thẻ (từ DB)"));
        add(new JScrollPane(txtInfoPreview), c);
    }

    private void onWrite() {
        String pid = txtPatientId.getText().trim();
        String pin = new String(txtPin.getPassword()).trim();

        if (pid.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập patient_id trước khi ghi thẻ.");
            return;
        }
        if (!isValidPin(pin)) {
            JOptionPane.showMessageDialog(this, "PIN phải gồm đúng 6 chữ số.", "PIN không hợp lệ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PatientProfile profile = loadPatientFromDb(pid);
        if (profile == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy bệnh nhân với patient_id = " + pid, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        txtInfoPreview.setText(
                "Họ tên: " + profile.fullName + "\n" +
                "Ngày sinh: " + profile.dob + "\n" +
                "Nhóm máu: " + profile.bloodType + "\n" +
                "Dị ứng: " + profile.allergies + "\n" +
                "Bệnh mãn tính: " + profile.chronic + "\n" +
                "Mã BHYT: " + profile.healthId + "\n" +
                "PIN cấp: " + pin + "\n" +
                "[ RSA Key (512-bit) sẽ được sinh tự động ]"
        );

        int confirm = JOptionPane.showConfirmDialog(this,
                "Ghi thông tin bệnh nhân và PIN này lên thẻ?\npatient_id = " + pid,
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 2. Kết nối thẻ
        try {
            cardManager.disconnect(); 
            if (!cardManager.connect()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thẻ (kiểm tra JCIDE / reader).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            CardChannel ch = cardManager.getChannel();

            // === FIX CHUẨN: SELECT APPLET VỚI AID TRONG ẢNH ===
            // AID: 00 11 22 33 44 55 67 11 (Lấy từ ảnh bạn gửi)
            byte[] aid = {
                (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, 
                (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11
            };
            
            ResponseAPDU selectResp = ch.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
            
            if (selectResp.getSW() != 0x9000) {
                // Thử nốt trường hợp AID mới (nếu bạn đã đổi)
                byte[] aidNew = {
                    (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, 
                    (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x00
                };
                selectResp = ch.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aidNew));
                
                if (selectResp.getSW() != 0x9000) {
                     JOptionPane.showMessageDialog(this, 
                        "Lỗi Select Applet (SW=" + Integer.toHexString(selectResp.getSW()) + ").\nĐã thử cả 2 AID (11 và 00) đều không được.\nVui lòng kiểm tra lại AID trong JCIDE.", 
                        "Lỗi Thẻ", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            // ==================================================

            apdu = new APDUCommands(ch);

            // 3. Sinh khóa RSA (512 bit)
            System.out.println("Đang sinh khóa RSA 512 bit...");
            KeyPair keyPair = generateRSAKeyPair();
            RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privKey = (RSAPrivateKey) keyPair.getPrivate();
            
            // 4. Update DB
            boolean updateDbOk = updatePublicKeyToDb(pid, pubKey);
            if (!updateDbOk) {
                JOptionPane.showMessageDialog(this, "Lỗi cập nhật Public Key vào DB.", "Lỗi DB", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 5. Ghi dữ liệu xuống thẻ
            if (!apdu.setPatientId(pid)) {
                JOptionPane.showMessageDialog(this, "Lỗi APDU: Không ghi được Patient ID.", "Ghi thẻ thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!apdu.setProfile(profile.fullName, profile.dob, profile.bloodType, profile.allergies, profile.chronic, profile.healthId)) {
                JOptionPane.showMessageDialog(this, "Lỗi APDU: Không ghi được Profile.", "Ghi thẻ thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!apdu.setAdminPin(pin)) {
                JOptionPane.showMessageDialog(this, "Lỗi APDU: Không set được PIN.", "Ghi thẻ thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // 6. Ghi RSA Key
            byte[] privKeyData = packPrivateKey(privKey);
            if (!apdu.setRsaPrivateKey(privKeyData)) {
                JOptionPane.showMessageDialog(this, "Lỗi APDU: Không ghi được RSA Private Key.", "Ghi thẻ thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }

            lblStatus.setText("Đã ghi xong thẻ cho BN: " + pid);
            lblStatus.setForeground(new Color(0,120,0));
            JOptionPane.showMessageDialog(this, "Ghi thẻ thành công!\n- Hồ sơ đã ghi.\n- PIN đã đặt.\n- RSA Key đã nạp.");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi ngoại lệ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            cardManager.disconnect();
            apdu = null;
        }
    }

    private boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 6) return false;
        for (int i = 0; i < pin.length(); i++) if (!Character.isDigit(pin.charAt(i))) return false;
        return true;
    }
    
    private KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512); 
        return keyGen.generateKeyPair();
    }
    
    private boolean updatePublicKeyToDb(String pid, RSAPublicKey pubKey) {
        String sql = "UPDATE smartcards SET card_public_key = ? WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            String pubBase64 = Base64.getEncoder().encodeToString(pubKey.getEncoded());
            pst.setString(1, pubBase64);
            pst.setString(2, pid);
            return pst.executeUpdate() > 0; 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private byte[] packPrivateKey(RSAPrivateKey privKey) {
        BigInteger mod = privKey.getModulus();
        BigInteger exp = privKey.getPrivateExponent();
        byte[] modBytes = stripLeadingZero(mod.toByteArray());
        byte[] expBytes = stripLeadingZero(exp.toByteArray());
        int len = 1 + modBytes.length + 1 + expBytes.length;
        byte[] data = new byte[len];
        int off = 0;
        data[off++] = (byte) modBytes.length;
        System.arraycopy(modBytes, 0, data, off, modBytes.length);
        off += modBytes.length;
        data[off++] = (byte) expBytes.length;
        System.arraycopy(expBytes, 0, data, off, expBytes.length);
        return data;
    }
    
    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }

    private PatientProfile loadPatientFromDb(String patientId) {
        String sql = "SELECT full_name, date_of_birth, home_address, blood_type, allergies, chronic_illness, health_insurance_id FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, patientId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    PatientProfile p = new PatientProfile();
                    p.fullName = rs.getString("full_name");
                    java.sql.Date dob = rs.getDate("date_of_birth");
                    p.dob = (dob != null) ? dob.toString() : "";
                    p.address = rs.getString("home_address");
                    p.bloodType = rs.getString("blood_type");
                    p.allergies = rs.getString("allergies");
                    p.chronic = rs.getString("chronic_illness");
                    p.healthId = rs.getString("health_insurance_id");
                    return p;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static class PatientProfile {
        String fullName, dob, address, bloodType, allergies, chronic, healthId;
    }
}
