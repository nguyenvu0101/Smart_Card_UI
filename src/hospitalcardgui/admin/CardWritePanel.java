package hospitalcardgui.admin;

import hospitalcardgui.APDUCommands;
import hospitalcardgui.CardManager;
import hospitalcardgui.DatabaseConnection;

import javax.smartcardio.CardChannel;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CardWritePanel extends JPanel {

    private JTextField txtPatientId;
    private JPasswordField txtPin; // PIN 6 số
    private JButton btnWrite;
    private JLabel lblStatus;
    private JTextArea txtInfoPreview;

    private final CardManager cardManager = new CardManager();
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

        // patient_id
        c.gridx = 0; c.gridy = row;
        add(new JLabel("Mã bệnh nhân (patient_id):"), c);
        c.gridx = 1;
        txtPatientId = new JTextField(15);
        add(txtPatientId, c);
        row++;

        // PIN 6 số
        c.gridx = 0; c.gridy = row;
        add(new JLabel("PIN cấp cho thẻ (6 số):"), c);
        c.gridx = 1;
        txtPin = new JPasswordField(15);
        add(txtPin, c);
        row++;

        // Nút ghi thẻ
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        btnWrite = new JButton("Kết nối & Ghi thẻ");
        btnWrite.addActionListener(e -> onWrite());
        add(btnWrite, c);
        row++;

        // Trạng thái
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        lblStatus = new JLabel("Chưa kết nối thẻ.");
        lblStatus.setForeground(Color.RED);
        add(lblStatus, c);
        row++;

        // Preview thông tin
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        txtInfoPreview = new JTextArea(6, 40);
        txtInfoPreview.setEditable(false);
        txtInfoPreview.setBorder(BorderFactory.createTitledBorder("Thông tin sẽ ghi lên thẻ (từ DB)"));
        add(new JScrollPane(txtInfoPreview), c);
    }

    private void onWrite() {
        String pid = txtPatientId.getText().trim();
        String pin = new String(txtPin.getPassword()).trim();

        if (pid.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nhập patient_id trước khi ghi thẻ.");
            return;
        }
        if (!isValidPin(pin)) {
            JOptionPane.showMessageDialog(this,
                    "PIN phải gồm đúng 6 chữ số.",
                    "PIN không hợp lệ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1. Lấy thông tin bệnh nhân từ DB
        PatientProfile profile = loadPatientFromDb(pid);
        if (profile == null) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy bệnh nhân với patient_id = " + pid,
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Preview cho admin xem
        txtInfoPreview.setText(
                "Họ tên: " + profile.fullName + "\n" +
                "Ngày sinh: " + profile.dob + "\n" +
                "Nhóm máu: " + profile.bloodType + "\n" +
                "Dị ứng: " + profile.allergies + "\n" +
                "Bệnh mãn tính: " + profile.chronic + "\n" +
                "Mã BHYT: " + profile.healthId + "\n" +
                "PIN cấp: " + pin + "\n"
        );

        int confirm = JOptionPane.showConfirmDialog(this,
                "Ghi thông tin bệnh nhân và PIN này lên thẻ?\npatient_id = " + pid,
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 2. Kết nối thẻ và ghi dữ liệu
        try {
            if (!cardManager.isConnected()) {
                if (!cardManager.connect()) {
                    JOptionPane.showMessageDialog(this,
                            "Không kết nối được thẻ (kiểm tra JCIDE / reader).",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CardChannel ch = cardManager.getChannel();
                apdu = new APDUCommands(ch);
            }

            boolean okId = apdu.setPatientId(pid);
            boolean okProfile = apdu.setProfile(
                    profile.fullName,
                    profile.dob,
                    profile.bloodType,
                    profile.allergies,
                    profile.chronic,
                    profile.healthId
            );
            boolean okPin = apdu.setAdminPin(pin);

            if (okId && okProfile && okPin) {
                lblStatus.setText("Đã ghi patient_id, hồ sơ và PIN cho bệnh nhân " + pid);
                lblStatus.setForeground(new Color(0,120,0));
                JOptionPane.showMessageDialog(this,
                        "Ghi thông tin và PIN lên thẻ thành công.\nHãy thông báo PIN này cho bệnh nhân để lần đầu đăng nhập.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Ghi thẻ thất bại (kiểm tra lại applet/INS hoặc dữ liệu quá dài).",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi ghi thẻ: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            cardManager.disconnect();
            apdu = null;
        }
    }

    // ===== Validate PIN 6 số =====
    private boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 6) return false;
        for (int i = 0; i < pin.length(); i++) {
            if (!Character.isDigit(pin.charAt(i))) return false;
        }
        return true;
    }

    // ===== LẤY THÔNG TIN BỆNH NHÂN TỪ DB =====
    private PatientProfile loadPatientFromDb(String patientId) {
        String sql = "SELECT full_name, date_of_birth, home_address, " +
                     "blood_type, allergies, chronic_illness, health_insurance_id " +
                     "FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

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
            JOptionPane.showMessageDialog(this,
                    "Lỗi đọc patients từ DB: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    // Struct nhỏ chứa hồ sơ bệnh nhân
    private static class PatientProfile {
        String fullName;
        String dob;
        String address;
        String bloodType;
        String allergies;
        String chronic;
        String healthId;
    }
}
