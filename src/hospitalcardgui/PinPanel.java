package hospitalcardgui;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PinPanel extends JPanel {

    private final MainFrame parent;
    private JPasswordField txtPin;
    private JButton btnLogin;
    private JButton btnChangePin; // dùng cho đổi PIN chủ động (sau này)

    public PinPanel(MainFrame parent) {
        this.parent = parent;
        initUI();
    }

    private void initUI() {
        setBorder(BorderFactory.createTitledBorder("Nhập / đổi mã PIN"));
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        add(new JLabel("PIN:"), c);

        c.gridx = 1;
        txtPin = new JPasswordField(10);
        txtPin.setEnabled(false);
        add(txtPin, c);

        c.gridx = 0; c.gridy = 1;
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setEnabled(false);
        btnLogin.addActionListener(e -> onLogin());
        add(btnLogin, c);

        c.gridx = 1;
        btnChangePin = new JButton("Đổi PIN");
        btnChangePin.setEnabled(false);
        btnChangePin.addActionListener(e -> onChangePinManual());
        add(btnChangePin, c);
    }

    public void enablePinInput(boolean enable) {
        txtPin.setEnabled(enable);
        btnLogin.setEnabled(enable);
        if (!enable) {
            btnChangePin.setEnabled(false);
            txtPin.setText("");
        }
    }

    private void onLogin() {
        APDUCommands apdu = parent.getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối thẻ.");
            return;
        }

        String pin = new String(txtPin.getPassword()).trim();
        if (pin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập PIN trước.");
            return;
        }

        // 1. VERIFY PIN trên thẻ
        if (!apdu.verifyPIN(pin)) {
            parent.log("PIN sai.");
            JOptionPane.showMessageDialog(this, "PIN sai.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        parent.setStatus("PIN đúng, đang kiểm tra thông tin thẻ...", new Color(0,120,0));
        parent.log("PIN đúng.");

        // 2. Đọc patient_id từ thẻ
        String patientId = apdu.getPatientId();
        if (patientId == null || patientId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không đọc được mã bệnh nhân từ thẻ.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        parent.setCurrentPatientId(patientId);

        // 3. Kiểm tra cờ must_change_pin trong DB
        boolean mustChange = mustChangePin(patientId);
        if (mustChange) {
            // Bắt bệnh nhân đổi PIN lần đầu
            boolean ok = forceChangePinFirstTime(pin, apdu, patientId);
            if (!ok) {
                // Nếu đổi PIN thất bại thì không cho đăng nhập tiếp
                return;
            }
        }

        // 4. Load thông tin bệnh nhân & sang màn giao dịch
        parent.loadPatientInfoOnTransactionPanel();
        btnChangePin.setEnabled(true); // cho phép đổi PIN chủ động sau này
        parent.showTransactionPage();
    }

    // Đổi PIN chủ động (không phải lần đầu bắt buộc) – có thể để trống hoặc đơn giản
    private void onChangePinManual() {
        APDUCommands apdu = parent.getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối / xác thực thẻ.");
            return;
        }

        String oldPin = new String(txtPin.getPassword()).trim();
        if (oldPin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập PIN hiện tại trước.");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2,2,5,5));
        JPasswordField txtNew1 = new JPasswordField();
        JPasswordField txtNew2 = new JPasswordField();
        panel.add(new JLabel("PIN mới:"));
        panel.add(txtNew1);
        panel.add(new JLabel("Nhập lại PIN mới:"));
        panel.add(txtNew2);

        int res = JOptionPane.showConfirmDialog(this, panel,
                "Đổi PIN", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        String new1 = new String(txtNew1.getPassword()).trim();
        String new2 = new String(txtNew2.getPassword()).trim();
        if (!isValidPin(new1) || !new1.equals(new2)) {
            JOptionPane.showMessageDialog(this,
                    "PIN mới phải gồm 6 chữ số và nhập trùng khớp.",
                    "PIN không hợp lệ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (apdu.changePIN(oldPin, new1)) {
            JOptionPane.showMessageDialog(this, "Đổi PIN thành công.");
            txtPin.setText(new1); // cập nhật ô PIN trên form
        } else {
            JOptionPane.showMessageDialog(this,
                    "Đổi PIN thất bại (PIN hiện tại có thể không đúng).",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ========== HỖ TRỢ: kiểm tra & bắt đổi PIN lần đầu ========== */

    // Đọc must_change_pin từ bảng smartcards
    private boolean mustChangePin(String patientId) {
        String sql = "SELECT must_change_pin FROM smartcards WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, patientId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("must_change_pin");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            parent.log("Lỗi đọc must_change_pin: " + ex.getMessage());
        }
        // Nếu không đọc được thì coi như không bắt đổi để tránh chặn user
        return false;
    }

    // Bắt buộc đổi PIN lần đầu: oldPin là PIN admin đã cấp
    private boolean forceChangePinFirstTime(String oldPin,
                                            APDUCommands apdu,
                                            String patientId) {
        JPanel panel = new JPanel(new GridLayout(2,2,5,5));
        JPasswordField txtNew1 = new JPasswordField();
        JPasswordField txtNew2 = new JPasswordField();
        panel.add(new JLabel("PIN mới (6 số):"));
        panel.add(txtNew1);
        panel.add(new JLabel("Nhập lại PIN mới:"));
        panel.add(txtNew2);

        int res = JOptionPane.showConfirmDialog(this, panel,
                "Lần đầu sử dụng - yêu cầu đổi PIN", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "Bạn phải đổi PIN để tiếp tục sử dụng thẻ.",
                    "Chưa đổi PIN", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String new1 = new String(txtNew1.getPassword()).trim();
        String new2 = new String(txtNew2.getPassword()).trim();
        if (!isValidPin(new1) || !new1.equals(new2)) {
            JOptionPane.showMessageDialog(this,
                    "PIN mới phải gồm 6 chữ số và nhập trùng khớp.",
                    "PIN không hợp lệ", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Gọi CHANGE_PIN trên thẻ
        if (!apdu.changePIN(oldPin, new1)) {
            JOptionPane.showMessageDialog(this,
                    "Đổi PIN thất bại. Hãy thử lại hoặc liên hệ quầy hỗ trợ.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Đổi PIN thành công -> update cờ must_change_pin = false trong DB
        updateMustChangePinFalse(patientId);

        JOptionPane.showMessageDialog(this,
                "Đổi PIN thành công. Từ lần sau hãy dùng PIN mới để đăng nhập.");
        txtPin.setText(new1);
        return true;
    }

    private void updateMustChangePinFalse(String patientId) {
        String sql = "UPDATE smartcards SET must_change_pin = FALSE WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, patientId);
            pst.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            parent.log("Lỗi cập nhật must_change_pin: " + ex.getMessage());
        }
    }

    // Validate PIN 6 số
    private boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 6) return false;
        for (int i = 0; i < pin.length(); i++) {
            if (!Character.isDigit(pin.charAt(i))) return false;
        }
        return true;
    }
}
