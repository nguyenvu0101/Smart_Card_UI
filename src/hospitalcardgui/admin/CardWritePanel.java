package hospitalcardgui.admin;

import hospitalcardgui.admin.AdminKeyManager;
import hospitalcardgui.CardManager;
import hospitalcardgui.CryptoUtils;
import hospitalcardgui.DatabaseConnection;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Base64;

public class CardWritePanel extends JPanel {

    private JTextField txtPatientId;
    private JPasswordField txtPin;
    private JPasswordField txtAdminPass;
    private JButton btnWrite;
    private JLabel lblStatus;
    private JTextArea txtInfoPreview;
    private final CardManager cardManager = CardManager.getInstance();

    private static final int CARD_CLA = 0x80;

    public CardWritePanel() { initUI(); }

    private void initUI() {
        TitledBorder border = BorderFactory.createTitledBorder("Phát hành thẻ (Chuẩn hóa dữ liệu)");
        border.setTitleFont(AdminTheme.FONT_BUTTON);
        setBorder(border);
        AdminTheme.applyMainBackground(this);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        c.gridx = 0; c.gridy = row; add(AdminTheme.label("Mã bệnh nhân:"), c);
        c.gridx = 1; txtPatientId = new JTextField(15); add(txtPatientId, c);
        row++;

        c.gridx = 0; c.gridy = row; add(AdminTheme.label("PIN User (6 số):"), c);
        c.gridx = 1; txtPin = new JPasswordField(15); add(txtPin, c);
        row++;

        c.gridx = 0; c.gridy = row; add(AdminTheme.label("Admin Password:"), c);
        c.gridx = 1; txtAdminPass = new JPasswordField(15); add(txtAdminPass, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        btnWrite = new JButton("Ghi Thẻ & Mã Hóa DB");
        AdminTheme.stylePrimaryButton(btnWrite);
        btnWrite.addActionListener(e -> onWrite());
        add(btnWrite, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        lblStatus = AdminTheme.label("Chưa kết nối");
        lblStatus.setForeground(Color.RED);
        add(lblStatus, c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        txtInfoPreview = new JTextArea(10, 40);
        txtInfoPreview.setEditable(false);
        add(new JScrollPane(txtInfoPreview), c);
    }

    private void onWrite() {
        String pid = txtPatientId.getText().trim();
        char[] pinChars = txtPin.getPassword();
        char[] adminChars = txtAdminPass.getPassword();

        if (pid.isEmpty() || pinChars.length == 0 || adminChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Nhập thiếu thông tin!");
            return;
        }

        if (!DatabaseConnection.verifyAdmin("admin", new String(adminChars))) {
            JOptionPane.showMessageDialog(this, "Sai mật khẩu Admin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PatientProfile profile = loadPatientFromDb(pid);
        if (profile == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy BN ID=" + pid);
            return;
        }

        byte[] masterKey = null;
        byte[] salt = null;
        byte[] keyUser = null;
        byte[] keyAdmin = null;
        byte[] pinHash = null;
        byte[] wrappedMkUser = null;
        byte[] wrappedMkAdmin = null;
        byte[] encryptedProfile = null;

        try {
            txtInfoPreview.setText("Đang xử lý Crypto...\n");

            // 1. CHUẨN BỊ DỮ LIỆU RAW (PLAINTEXT)
            String plainRawData;
            
            if (profile.isEncrypted) {
                // Nếu DB đã mã hóa -> Giải mã để lấy lại Plaintext gốc
                if (!AdminKeyManager.isKeyReady()) throw new Exception("Thiếu Admin Key!");
                byte[] encBytes = Base64.getDecoder().decode(profile.rawData);
                byte[] decBytes = CryptoUtils.aesDecrypt(encBytes, AdminKeyManager.getKey());
                plainRawData = new String(decBytes, StandardCharsets.UTF_8);
            } else {
                // NẾU DB CHƯA MÃ HÓA -> ĐÓNG GÓI THEO THỨ TỰ CHUẨN (0->6)
                plainRawData = profile.fullName + "|" +       // 0
                               profile.dob + "|" +            // 1
                               profile.bloodType + "|" +      // 2
                               profile.allergies + "|" +      // 3
                               profile.chronic + "|" +        // 4
                               profile.healthId + "|" +       // 5
                               profile.address;               // 6
            }

            // 2. SINH KHÓA THẺ
            masterKey = CryptoUtils.generateMasterKey();
            salt = CryptoUtils.generateSalt();
            keyUser = CryptoUtils.deriveKeyArgon2(pinChars, salt, 16);
            keyAdmin = CryptoUtils.deriveKeyArgon2(adminChars, salt, 16);
            pinHash = CryptoUtils.deriveKeyArgon2(pinChars, salt, 32);
            wrappedMkUser = CryptoUtils.aesEncrypt(masterKey, keyUser);
            wrappedMkAdmin = CryptoUtils.aesEncrypt(masterKey, keyAdmin);

            // 3. MÃ HÓA CHO THẺ
            encryptedProfile = CryptoUtils.aesEncrypt(plainRawData.getBytes(StandardCharsets.UTF_8), masterKey);
            
            // 4. MÃ HÓA CHO DB (Nếu chưa mã hóa)
            if (!profile.isEncrypted) {
                if (!AdminKeyManager.isKeyReady()) throw new Exception("Thiếu Admin Key!");
                byte[] dbEncBytes = CryptoUtils.aesEncrypt(plainRawData.getBytes(StandardCharsets.UTF_8), AdminKeyManager.getKey());
                String dbEncBase64 = Base64.getEncoder().encodeToString(dbEncBytes);
                
                if (updateEncryptedDataToDb(pid, dbEncBase64)) {
                    txtInfoPreview.append("- Đã mã hóa dữ liệu và cập nhật DB.\n");
                }
            }

            Arrays.fill(masterKey, (byte)0);

            // 5. GHI THẺ
            lblStatus.setText("Đang kết nối thẻ...");
            if (!cardManager.connect()) {
                JOptionPane.showMessageDialog(this, "Lỗi kết nối thẻ");
                return;
            }
            CardChannel ch = cardManager.getChannel();
            
            byte[] aid = {(byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11};
            ResponseAPDU rSel = ch.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
            if (rSel.getSW() != 0x9000) throw new Exception("Lỗi Select AID: " + formatSW(rSel.getSW()));

            txtInfoPreview.append("- Gửi dữ liệu xuống thẻ...\n");
            sendDataAPDU(ch, CARD_CLA, 0x20, 0x00, 0x00, salt, "Salt");
            sendDataAPDU(ch, CARD_CLA, 0x21, 0x00, 0x00, pinHash, "PinHash");
            sendDataAPDU(ch, CARD_CLA, 0x22, 0x00, 0x00, wrappedMkUser, "WrapUser");
            sendDataAPDU(ch, CARD_CLA, 0x23, 0x00, 0x00, wrappedMkAdmin, "WrapAdmin");
            sendDataAPDU(ch, CARD_CLA, 0x24, 0x00, 0x00, encryptedProfile, "Profile");

            // RSA KeyGen
            txtInfoPreview.append("--- Yêu cầu thẻ sinh khóa RSA ---\n");
            CommandAPDU cmdGen = new CommandAPDU(CARD_CLA, 0x52, 0x00, 0x00, 256); 
            ResponseAPDU respGen = ch.transmit(cmdGen);
            if (respGen.getSW() != 0x9000) throw new Exception("Lỗi sinh RSA: " + formatSW(respGen.getSW()));
            
            byte[] keyData = respGen.getData();
            if (keyData.length > 4) {
                int modLen = ((keyData[0] & 0xFF) << 8) | (keyData[1] & 0xFF);
                byte[] modulus = new byte[modLen];
                System.arraycopy(keyData, 2, modulus, 0, modLen);
                int expOffset = 2 + modLen;
                int expLen = ((keyData[expOffset] & 0xFF) << 8) | (keyData[expOffset+1] & 0xFF);
                byte[] exponent = new byte[expLen];
                System.arraycopy(keyData, expOffset + 2, exponent, 0, expLen);
                
                KeyFactory kf = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
                RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(pubSpec);
                updatePublicKeyToDb(pid, pubKey);
            }

            // Lưu Salt vào database để dùng cho reset PIN sau này
            saveSaltToDb(pid, salt);
            
            updateCardStatusActive(pid);
            lblStatus.setText("Hoàn tất!");
            lblStatus.setForeground(Color.GREEN);
            JOptionPane.showMessageDialog(this, "Phát hành thẻ thành công!");

        } catch (Exception ex) {
            ex.printStackTrace();
            txtInfoPreview.append("LỖI: " + ex.getMessage() + "\n");
            lblStatus.setText("Lỗi!");
        } finally {
            zeroCharArray(pinChars); zeroCharArray(adminChars);
            zeroByteArray(masterKey); zeroByteArray(salt);
        }
    }

    // --- HELPER METHODS ---
    private boolean updateEncryptedDataToDb(String pid, String encryptedData) {
        // Cập nhật encrypted_data VÀ xóa sạch các cột plaintext
        String sql = "UPDATE patients SET encrypted_data = ?, full_name = ?, " +
                     "date_of_birth = NULL, home_address = 'ENC', blood_type = 'ENC', " +
                     "allergies = 'ENC', chronic_illness = 'ENC', health_insurance_id = 'ENC' " +
                     "WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, encryptedData);
            pst.setString(2, encryptedData); // Fallback
            pst.setString(3, pid);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    private PatientProfile loadPatientFromDb(String patientId) {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, patientId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String encData = rs.getString("encrypted_data");
                    if (encData != null && !encData.isEmpty()) {
                        return new PatientProfile(true, encData);
                    }
                    // Load Plaintext theo đúng tên cột
                    return new PatientProfile(
                        false,
                        rs.getString("full_name"),
                        rs.getString("date_of_birth"),
                        rs.getString("blood_type"),
                        rs.getString("allergies"),
                        rs.getString("chronic_illness"),
                        rs.getString("health_insurance_id"),
                        rs.getString("home_address")
                    );
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    private static class PatientProfile {
        boolean isEncrypted;
        String rawData;
        String fullName, dob, bloodType, allergies, chronic, healthId, address;

        public PatientProfile(boolean isEnc, String data) {
            this.isEncrypted = isEnc; this.rawData = data;
        }
        public PatientProfile(boolean isEnc, String f, String d, String b, String a, String c, String h, String addr) {
            this.isEncrypted = isEnc;
            this.fullName = f; this.dob = d; this.bloodType = b;
            this.allergies = a; this.chronic = c; this.healthId = h;
            this.address = addr;
        }
    }
    
    private void sendDataAPDU(CardChannel ch, int cla, int ins, int p1, int p2, byte[] data, String name) throws Exception {
        if (data == null) data = new byte[0];
        if (data.length > 255) throw new Exception("Payload quá lớn: " + data.length);
        CommandAPDU capdu = new CommandAPDU(cla, ins, p1, p2, data);
        ResponseAPDU r = ch.transmit(capdu);
        txtInfoPreview.append(" -> " + name + ": " + formatSW(r.getSW()) + "\n");
        if (r.getSW() != 0x9000) throw new Exception("Lỗi gửi " + name);
    }
    private String formatSW(int sw) { return String.format("0x%04X", sw & 0xFFFF); }
    private void zeroByteArray(byte[] b) { if (b != null) Arrays.fill(b, (byte)0); }
    private void zeroCharArray(char[] c) { if (c != null) Arrays.fill(c, '\0'); }
    private boolean updatePublicKeyToDb(String pid, RSAPublicKey pubKey) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement("UPDATE smartcards SET card_public_key = ? WHERE patient_id = ?")) {
            pst.setString(1, Base64.getEncoder().encodeToString(pubKey.getEncoded()));
            pst.setString(2, pid);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }
    private void updateCardStatusActive(String pid) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement("UPDATE smartcards SET card_status = 'ACTIVE' WHERE patient_id = ?")) {
            pst.setString(1, pid); pst.executeUpdate();
        } catch (Exception e) {}
    }
    
    private void saveSaltToDb(String pid, byte[] salt) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Tạo cột card_salt nếu chưa có
            try {
                conn.createStatement().executeUpdate(
                    "ALTER TABLE smartcards ADD COLUMN IF NOT EXISTS card_salt TEXT"
                );
            } catch (Exception e) {
                // Cột đã tồn tại, bỏ qua
            }
            
            // Lưu Salt vào DB
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            try (PreparedStatement pst = conn.prepareStatement(
                "UPDATE smartcards SET card_salt = ? WHERE patient_id = ?")) {
                pst.setString(1, saltB64);
                pst.setString(2, pid);
                pst.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
