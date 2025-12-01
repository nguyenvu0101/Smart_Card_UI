package hospitalcardgui.admin;

import hospitalcardgui.CardManager;
import hospitalcardgui.CryptoUtils;
import hospitalcardgui.DatabaseConnection;
import hospitalcardgui.APDUCommands;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Base64;

public class ResetPinPanel extends JPanel {

    private JTextField txtPatientId;
    private JPasswordField txtAdminPass;
    private JPasswordField txtNewPin;
    private JButton btnReset;
    private JLabel lblStatus;
    private JTextArea txtLog;
    private final CardManager cardManager = CardManager.getInstance();

    private static final int CARD_CLA = 0x80;
    private static final byte[] APPLET_AID = {
        (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, 
        (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11
    };

    public ResetPinPanel() {
        initUI();
    }

    private void initUI() {
        TitledBorder border = BorderFactory.createTitledBorder("Reset PIN User");
        border.setTitleFont(AdminTheme.FONT_BUTTON);
        setBorder(border);
        AdminTheme.applyMainBackground(this);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        // Tạo panel form chứa các input field
        JPanel formPanel = new JPanel(new GridBagLayout());
        AdminTheme.applyCardStyle(formPanel);
        GridBagConstraints fc = new GridBagConstraints();
        fc.insets = new Insets(5, 5, 5, 5);
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.anchor = GridBagConstraints.WEST;
        fc.weightx = 1.0;

        int row = 0;
        fc.gridx = 0; fc.gridy = row;
        fc.anchor = GridBagConstraints.EAST;
        formPanel.add(AdminTheme.label("Mã bệnh nhân:"), fc);
        fc.gridx = 1;
        txtPatientId = new JTextField(25);
        txtPatientId.setFont(AdminTheme.FONT_INPUT);
        txtPatientId.setPreferredSize(new Dimension(280, 32));
        txtPatientId.setMinimumSize(new Dimension(280, 32));
        formPanel.add(txtPatientId, fc);
        row++;

        fc.gridx = 0; fc.gridy = row;
        fc.anchor = GridBagConstraints.EAST;
        formPanel.add(AdminTheme.label("Admin Password:"), fc);
        fc.gridx = 1;
        txtAdminPass = new JPasswordField(25);
        txtAdminPass.setFont(AdminTheme.FONT_INPUT);
        txtAdminPass.setPreferredSize(new Dimension(280, 32));
        txtAdminPass.setMinimumSize(new Dimension(280, 32));
        formPanel.add(txtAdminPass, fc);
        row++;

        fc.gridx = 0; fc.gridy = row;
        fc.anchor = GridBagConstraints.EAST;
        formPanel.add(AdminTheme.label("PIN mới (6 số):"), fc);
        fc.gridx = 1;
        txtNewPin = new JPasswordField(25);
        txtNewPin.setFont(AdminTheme.FONT_INPUT);
        txtNewPin.setPreferredSize(new Dimension(280, 32));
        txtNewPin.setMinimumSize(new Dimension(280, 32));
        formPanel.add(txtNewPin, fc);
        row++;

        fc.gridx = 0; fc.gridy = row; fc.gridwidth = 2;
        fc.anchor = GridBagConstraints.CENTER;
        btnReset = new JButton("Reset PIN");
        AdminTheme.stylePrimaryButton(btnReset);
        btnReset.addActionListener(e -> onResetPin());
        formPanel.add(btnReset, fc);
        row++;

        fc.gridx = 0; fc.gridy = row; fc.gridwidth = 2;
        fc.anchor = GridBagConstraints.CENTER;
        lblStatus = AdminTheme.label("Chưa kết nối");
        lblStatus.setForeground(Color.RED);
        formPanel.add(lblStatus, fc);

        // Căn giữa formPanel trong panel chính
        c.gridx = 0; c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0;
        add(formPanel, c);

        // TextArea log
        c.gridx = 0; c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.CENTER;
        txtLog = new JTextArea(15, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(txtLog), c);
    }

    private void onResetPin() {
        String pid = txtPatientId.getText().trim();
        char[] adminChars = txtAdminPass.getPassword();
        char[] newPinChars = txtNewPin.getPassword();

        if (pid.isEmpty() || adminChars.length == 0 || newPinChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Nhập thiếu thông tin!");
            return;
        }

        if (newPinChars.length != 6) {
            JOptionPane.showMessageDialog(this, "PIN phải có đúng 6 số!");
            return;
        }

        // 1. Verify Admin Password
        log("========================================");
        log("BƯỚC 1: Verify Admin Password...");
        if (!DatabaseConnection.verifyAdmin("admin", new String(adminChars))) {
            log("✗ Sai mật khẩu Admin!");
            JOptionPane.showMessageDialog(this, "Sai mật khẩu Admin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        log("✓ Admin Password đúng.");

        // 2. Kết nối thẻ & SELECT Applet
        log("\nBƯỚC 2: Kết nối thẻ...");
        lblStatus.setText("Đang kết nối thẻ...");
        if (!cardManager.connect()) {
            log("✗ Lỗi kết nối thẻ");
            JOptionPane.showMessageDialog(this, "Lỗi kết nối thẻ");
            return;
        }
        log("✓ Kết nối vật lý thành công.");

        CardChannel ch = cardManager.getChannel();
        if (ch == null) {
            log("✗ Không lấy được CardChannel.");
            cardManager.disconnect();
            return;
        }

        try {
            log("Đang SELECT applet...");
            ResponseAPDU rSel = ch.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APPLET_AID));
            if (rSel.getSW() != 0x9000) {
                log("✗ SELECT APPLET THẤT BẠI! SW: " + formatSW(rSel.getSW()));
                throw new Exception("Lỗi Select AID: " + formatSW(rSel.getSW()));
            }
            log("✓ SELECT APPLET thành công.");

            APDUCommands apdu = new APDUCommands(ch);

            // 3. Lấy Salt từ database (thay vì từ thẻ)
            log("\nBƯỚC 3: Lấy Salt từ database...");
            byte[] oldSalt = getSaltFromDb(pid);
            if (oldSalt == null) {
                log("✗ Không tìm thấy Salt trong database!");
                log("  Thẻ có thể chưa được phát hành hoặc Salt chưa được lưu.");
                throw new Exception("Không tìm thấy Salt trong database!");
            }
            log("✓ Đã lấy Salt từ database.");

            // 4. Tính Key Admin từ Admin Password + Salt (từ DB)
            log("\nBƯỚC 4: Tính Key Admin từ Admin Password + Salt...");
            byte[] keyAdmin = CryptoUtils.deriveKeyArgon2(adminChars, oldSalt, 16);
            log("✓ Đã tính Key Admin.");

            // 5. Lấy Wrapped Key Admin từ thẻ
            log("\nBƯỚC 5: Lấy Wrapped Key Admin từ thẻ...");
            byte[] wrappedMkAdmin = apdu.getWrappedKeyAdmin();
            if (wrappedMkAdmin == null) {
                log("✗ Không lấy được Wrapped Key Admin từ thẻ!");
                throw new Exception("Lỗi lấy Wrapped Key Admin");
            }
            log("✓ Đã lấy Wrapped Key Admin từ thẻ.");

            // 6. Unwrap Master Key bằng Key Admin
            log("\nBƯỚC 6: Unwrap Master Key bằng Key Admin...");
            byte[] masterKey = CryptoUtils.aesDecrypt(wrappedMkAdmin, keyAdmin);
            log("✓ Đã unwrap Master Key.");

            // 7. Sinh Salt mới
            log("\nBƯỚC 7: Sinh Salt mới...");
            byte[] newSalt = CryptoUtils.generateSalt();
            log("✓ Đã sinh Salt mới.");

            // 8. Tính các thành phần mới
            log("\nBƯỚC 8: Tính các thành phần mới...");
            byte[] newPinHash = CryptoUtils.deriveKeyArgon2(newPinChars, newSalt, 32);
            byte[] newKeyUser = CryptoUtils.deriveKeyArgon2(newPinChars, newSalt, 16);
            byte[] newKeyAdmin = CryptoUtils.deriveKeyArgon2(adminChars, newSalt, 16);
            byte[] newWrappedMkUser = CryptoUtils.aesEncrypt(masterKey, newKeyUser);
            byte[] newWrappedMkAdmin = CryptoUtils.aesEncrypt(masterKey, newKeyAdmin);
            log("✓ Đã tính:");
            log("  - PIN Hash mới");
            log("  - Key User mới");
            log("  - Key Admin mới");
            log("  - Wrapped Key User mới");
            log("  - Wrapped Key Admin mới");

            // 9. Ghi lại lên thẻ
            log("\nBƯỚC 9: Ghi dữ liệu mới lên thẻ...");
            sendDataAPDU(ch, CARD_CLA, 0x20, 0x00, 0x00, newSalt, "Salt mới");
            sendDataAPDU(ch, CARD_CLA, 0x21, 0x00, 0x00, newPinHash, "PIN Hash mới");
            sendDataAPDU(ch, CARD_CLA, 0x22, 0x00, 0x00, newWrappedMkUser, "Wrapped Key User mới");
            sendDataAPDU(ch, CARD_CLA, 0x23, 0x00, 0x00, newWrappedMkAdmin, "Wrapped Key Admin mới");
            
            // Set status = First Login (0x01)
            byte[] statusData = { 0x01 };
            sendDataAPDU(ch, CARD_CLA, 0x29, 0x00, 0x00, statusData, "Set Status = First Login");
            log("✓ Đã ghi tất cả dữ liệu lên thẻ.");

            // 10. Cập nhật Salt mới vào database
            log("\nBƯỚC 10: Cập nhật Salt mới vào database...");
            if (saveSaltToDb(pid, newSalt)) {
                log("✓ Đã cập nhật Salt mới vào database.");
            } else {
                log("⚠ Cảnh báo: Không thể cập nhật Salt vào database!");
            }

            // 11. Hoàn tất
            log("\n========================================");
            log("✓ RESET PIN THÀNH CÔNG!");
            log("========================================");
            lblStatus.setText("Reset PIN thành công!");
            lblStatus.setForeground(Color.GREEN);
            JOptionPane.showMessageDialog(this, 
                "Reset PIN thành công!\n" +
                "User cần đăng nhập lại với PIN mới và đổi PIN lần đầu.",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);

            // Xóa dữ liệu nhạy cảm
            Arrays.fill(masterKey, (byte)0);
            Arrays.fill(keyAdmin, (byte)0);
            Arrays.fill(newKeyUser, (byte)0);
            Arrays.fill(newKeyAdmin, (byte)0);

        } catch (Exception ex) {
            ex.printStackTrace();
            log("\n✗ LỖI: " + ex.getMessage());
            lblStatus.setText("Lỗi!");
            lblStatus.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, 
                "Lỗi reset PIN:\n" + ex.getMessage(), 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            cardManager.disconnect();
            zeroCharArray(adminChars);
            zeroCharArray(newPinChars);
        }
    }

    private byte[] getSaltFromDb(String pid) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                 "SELECT card_salt FROM smartcards WHERE patient_id = ?")) {
            pst.setString(1, pid);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String saltB64 = rs.getString("card_salt");
                    if (saltB64 != null && !saltB64.isEmpty()) {
                        return Base64.getDecoder().decode(saltB64);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean saveSaltToDb(String pid, byte[] salt) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Tạo cột card_salt nếu chưa có
            try {
                conn.createStatement().executeUpdate(
                    "ALTER TABLE smartcards ADD COLUMN IF NOT EXISTS card_salt TEXT"
                );
            } catch (Exception e) {
                // Cột đã tồn tại, bỏ qua
            }
            
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            try (PreparedStatement pst = conn.prepareStatement(
                "UPDATE smartcards SET card_salt = ? WHERE patient_id = ?")) {
                pst.setString(1, saltB64);
                pst.setString(2, pid);
                return pst.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendDataAPDU(CardChannel ch, int cla, int ins, int p1, int p2, byte[] data, String name) throws Exception {
        if (data == null) data = new byte[0];
        if (data.length > 255) throw new Exception("Payload quá lớn: " + data.length);
        CommandAPDU capdu = new CommandAPDU(cla, ins, p1, p2, data);
        ResponseAPDU r = ch.transmit(capdu);
        log("  -> " + name + ": " + formatSW(r.getSW()));
        if (r.getSW() != 0x9000) throw new Exception("Lỗi gửi " + name + ": " + formatSW(r.getSW()));
    }

    private String formatSW(int sw) {
        return String.format("0x%04X", sw & 0xFFFF);
    }

    private void log(String msg) {
        txtLog.append(msg + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
        System.out.println("[ResetPin] " + msg);
    }

    private void zeroCharArray(char[] c) {
        if (c != null) Arrays.fill(c, '\0');
    }
}

