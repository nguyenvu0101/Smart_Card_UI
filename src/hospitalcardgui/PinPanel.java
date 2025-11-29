package hospitalcardgui;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PinPanel extends JPanel {

    private JPasswordField txtPin;
    private JButton btnLogin;
    private JLabel lblStatus;
    
    private MainFrame mainFrame; 
    private final CardManager cardManager = CardManager.getInstance();

    public PinPanel(MainFrame frame) {
        this.mainFrame = frame;
        initUI();
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0; c.gridy = 0;

        JLabel title = new JLabel("ĐĂNG NHẬP THẺ BỆNH NHÂN");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        add(title, c);
        
        c.gridy = 1;
        add(new JLabel("Nhập PIN (6 số):"), c);
        
        c.gridy = 2;
        txtPin = new JPasswordField(10);
        txtPin.setHorizontalAlignment(JTextField.CENTER);
        add(txtPin, c);
        
        c.gridy = 3;
        btnLogin = new JButton("Xác nhận PIN");
        btnLogin.addActionListener(e -> onLogin());
        add(btnLogin, c);

        c.gridy = 4;
        lblStatus = new JLabel("Vui lòng kết nối thẻ...");
        lblStatus.setForeground(Color.GRAY);
        add(lblStatus, c);
    }
    
    public void enablePinInput(boolean enable) {
        txtPin.setEnabled(enable);
        btnLogin.setEnabled(enable);
        if (enable) {
            lblStatus.setText("Thẻ đã sẵn sàng. Mời nhập PIN.");
            lblStatus.setForeground(Color.BLUE);
            txtPin.requestFocus();
        } else {
            lblStatus.setText("Chưa kết nối thẻ.");
            lblStatus.setForeground(Color.RED);
            txtPin.setText("");
        }
    }

    private void onLogin() {
        String pin = new String(txtPin.getPassword());
        if (pin.length() != 6) {
            lblStatus.setText("PIN phải có đúng 6 chữ số.");
            lblStatus.setForeground(Color.RED);
            return;
        }

        try {
            // ✅ 1. Kiểm tra kết nối & Tự động kết nối lại
            if (!cardManager.isConnected()) {
                if (!cardManager.connect()) {
                    lblStatus.setText("Mất kết nối thẻ!");
                    lblStatus.setForeground(Color.RED);
                    return;
                }
            }

            // Tạo APDU mới để đảm bảo channel tươi
            CardChannel ch = cardManager.getChannel();
            APDUCommands apdu = new APDUCommands(ch);

            // ✅ 2. SELECT APPLET
            lblStatus.setText("Đang chọn applet...");
            byte[] aid = {(byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11};
            ResponseAPDU rSel = ch.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
            
            if (rSel.getSW() != 0x9000) {
                // Thử connect lại lần cuối
                cardManager.disconnect();
                if (cardManager.connect()) {
                    ch = cardManager.getChannel();
                    apdu = new APDUCommands(ch);
                    rSel = ch.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid));
                }
                if (rSel.getSW() != 0x9000) {
                    lblStatus.setText("Lỗi thẻ (Select failed).");
                    lblStatus.setForeground(Color.RED);
                    return;
                }
            }

            // ✅ 3. LẤY SALT
            lblStatus.setText("Đang lấy Salt...");
            byte[] salt = apdu.getSalt();
            if (salt == null) {
                lblStatus.setText("Lỗi đọc Salt (SW!=9000)");
                lblStatus.setForeground(Color.RED);
                return;
            }

            // ✅ 4. HASH & VERIFY
            lblStatus.setText("Đang xác thực...");
            byte[] pinHash = CryptoUtils.deriveKeyArgon2(pin.toCharArray(), salt, 32);
            boolean verified = apdu.verifyPinHash(pinHash);
            
            if (verified) {
                // --- ĐĂNG NHẬP THÀNH CÔNG ---
                lblStatus.setText("Đăng nhập thành công...");
                lblStatus.setForeground(Color.GREEN);

                // ✅ 5. GIẢI MÃ MASTER KEY
                byte[] wrappedKey = apdu.getWrappedKey();
                if (wrappedKey == null) {
                    lblStatus.setText("Lỗi đọc Wrapped Key"); return;
                }

                byte[] keyUser = CryptoUtils.deriveKeyArgon2(pin.toCharArray(), salt, 16);
                byte[] masterKey = CryptoUtils.aesDecrypt(wrappedKey, keyUser);
                
                if (masterKey == null) {
                    lblStatus.setText("Lỗi giải mã Master Key!"); return;
                }

                // ============================================================
                // ✅ LOGIC MỚI: KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP LẦN ĐẦU
                // ============================================================
                int status = apdu.getCardStatus();
                if (status == 1) {
                    // ==> LẦN ĐẦU TIÊN: BẮT BUỘC ĐỔI PIN
                    JOptionPane.showMessageDialog(this, 
                        "Chào mừng bạn!\nĐây là lần sử dụng thẻ đầu tiên.\nBạn CẦN ĐỔI MÃ PIN MỚI để kích hoạt thẻ.",
                        "Kích hoạt thẻ", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Gọi hàm đổi PIN ngay lập tức
                    performChangePin(apdu, masterKey);
                    
                    // Xóa dữ liệu nhạy cảm và return để user đăng nhập lại
                    Arrays.fill(masterKey, (byte)0);
                    return; 
                }
                // ============================================================

                // ✅ 6. NẾU KHÔNG PHẢI LẦN ĐẦU -> VÀO MÀN HÌNH CHÍNH
                byte[] encProfile = apdu.getEncryptedProfile();
                if (encProfile == null) {
                    lblStatus.setText("Lỗi đọc Profile"); return;
                }

                byte[] profileBytes = CryptoUtils.aesDecrypt(encProfile, masterKey);
                String rawData = new String(profileBytes, StandardCharsets.UTF_8);
                
                Arrays.fill(masterKey, (byte)0);

                if (mainFrame != null) {
                    String[] parts = rawData.split("\\|");
                    String name = (parts.length > 0) ? parts[0] : "Unknown";
                    
                    JOptionPane.showMessageDialog(this, "Xin chào: " + name, "Đăng nhập thành công", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.showTransactionPage();
                    mainFrame.getTransactionPanel().setPatientData(rawData);
                }

            } else {
                // ✅ PIN SAI
                int tries = apdu.getPinTriesRemaining(pinHash);
                lblStatus.setText("Sai PIN! Còn " + tries + " lần thử.");
                lblStatus.setForeground(Color.RED);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            lblStatus.setText("Lỗi: " + ex.getMessage());
            lblStatus.setForeground(Color.RED);
        }
    }

    // === HÀM ĐỔI PIN (Logic: Unwrap -> Rewrap -> Save) ===
    private void performChangePin(APDUCommands apdu, byte[] masterKey) {
        String newPin = JOptionPane.showInputDialog(this, "Nhập PIN MỚI (6 số):");
        
        if (newPin == null || newPin.length() != 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không hợp lệ (Phải đủ 6 số)!");
            return;
        }

        try {
            // 1. Sinh Salt mới
            byte[] newSalt = CryptoUtils.generateSalt();
            
            // 2. Hash PIN mới (để thẻ lưu verify)
            byte[] newPinHash = CryptoUtils.deriveKeyArgon2(newPin.toCharArray(), newSalt, 32);
            
            // 3. Tính Key User Mới (từ PIN mới)
            byte[] newKeyUser = CryptoUtils.deriveKeyArgon2(newPin.toCharArray(), newSalt, 16);
            
            // 4. BỌC LẠI MASTER KEY (Re-wrap)
            byte[] newWrappedKey = CryptoUtils.aesEncrypt(masterKey, newKeyUser);

            // 5. Ghi đè xuống thẻ
            boolean ok1 = apdu.setSalt(newSalt);
            boolean ok2 = apdu.setPinHash(newPinHash);
            boolean ok3 = apdu.setWrappedKeyUser(newWrappedKey);
            
            // 6. Tắt cờ "Lần đầu"
            boolean ok4 = apdu.disableFirstLogin();

            if (ok1 && ok2 && ok3 && ok4) {
                JOptionPane.showMessageDialog(this, "Đổi mật khẩu THÀNH CÔNG!\nVui lòng đăng nhập lại bằng mật khẩu mới.");
                txtPin.setText(""); 
                lblStatus.setText("Đã đổi PIN. Mời đăng nhập lại.");
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi ghi dữ liệu xuống thẻ!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi đổi PIN: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
