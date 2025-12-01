package hospitalcardgui.admin;

import hospitalcardgui.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class CardIssuePanel extends JPanel {

    private JTextField txtPatientId, txtFullName, txtDob, txtAddress,
            txtBloodType, txtAllergies, txtChronicIllness, txtInsuranceId, txtCardId;

    public CardIssuePanel() {
        initUI();
    }

    private void initUI() {
        TitledBorder border = BorderFactory.createTitledBorder("Thông Tin Bệnh Nhân & Cấp Mã Thẻ");
        border.setTitleFont(AdminTheme.FONT_BUTTON);
        setBorder(border);
        AdminTheme.applyMainBackground(this);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        AdminTheme.applyCardStyle(form);
        GridBagConstraints base = new GridBagConstraints();
        base.insets = new Insets(4, 4, 4, 4);
        base.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        form.add(AdminTheme.label("Mã Bệnh Nhân:"), at(base, 0, row));
        form.add(txtPatientId = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Họ tên:"), at(base, 0, row));
        form.add(txtFullName = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Ngày sinh (YYYY-MM-DD):"), at(base, 0, row));
        form.add(txtDob = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Địa chỉ:"), at(base, 0, row));
        form.add(txtAddress = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Nhóm máu:"), at(base, 0, row));
        form.add(txtBloodType = new JTextField(5), at(base, 1, row++));

        form.add(AdminTheme.label("Dị ứng:"), at(base, 0, row));
        form.add(txtAllergies = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Bệnh mãn tính:"), at(base, 0, row));
        form.add(txtChronicIllness = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Mã bảo hiểm:"), at(base, 0, row));
        form.add(txtInsuranceId = new JTextField(20), at(base, 1, row++));

        form.add(AdminTheme.label("Mã thẻ (Card ID):"), at(base, 0, row));
        form.add(txtCardId = new JTextField(20), at(base, 1, row++));

        add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        JButton btnSave = new JButton("Lưu Thông Tin (DB Only)");
        AdminTheme.stylePrimaryButton(btnSave);
        btnSave.addActionListener(e -> onSave());
        bottom.add(btnSave);

        add(bottom, BorderLayout.SOUTH);
    }

    private GridBagConstraints at(GridBagConstraints base, int x, int y) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridx = x;
        c.gridy = y;
        return c;
    }

       private void onSave() {
        String patientId = txtPatientId.getText().trim();
        String cardId = txtCardId.getText().trim();
        String dobStr = txtDob.getText().trim();

        if (patientId.isEmpty() || cardId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bắt buộc nhập Mã BN và Mã Thẻ.");
            return;
        }
        
        // Kiểm tra Admin Key
        if (!hospitalcardgui.admin.AdminKeyManager.isKeyReady()) {
            JOptionPane.showMessageDialog(this, "Lỗi bảo mật: Chưa có Admin Key. Vui lòng đăng nhập lại.");
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. GOM DỮ LIỆU THÀNH CHUỖI ĐỂ MÃ HÓA
            // Format: FullName|Dob|Address|Blood|Allergies|Chronic|Insurance
            String rawData = txtFullName.getText().trim() + "|" +       // 0
                             txtDob.getText().trim() + "|" +            // 1
                             txtBloodType.getText().trim() + "|" +      // 2
                             txtAllergies.getText().trim() + "|" +      // 3
                             txtChronicIllness.getText().trim() + "|" + // 4
                             txtInsuranceId.getText().trim() + "|" +    // 5
                             txtAddress.getText().trim();               // 6 

            // 2. MÃ HÓA BẰNG ADMIN MASTER KEY
            byte[] encryptedBytes = hospitalcardgui.CryptoUtils.aesEncrypt(
                rawData.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                hospitalcardgui.admin.AdminKeyManager.getKey()
            );
            String encryptedBase64 = java.util.Base64.getEncoder().encodeToString(encryptedBytes);

            // 3. LƯU VÀO DB (Các cột thông tin thật để 'ENC', chỉ lưu cột encrypted_data)
            // Giả sử bạn đã thêm cột 'encrypted_data' vào bảng patients
            String sqlPatient = "INSERT INTO patients (patient_id, encrypted_data, full_name, date_of_birth, home_address, blood_type, allergies, chronic_illness, health_insurance_id) " +
                                "VALUES (?, ?, 'ENC', NULL, 'ENC', 'ENC', 'ENC', 'ENC', 'ENC')";

            try (PreparedStatement pst = conn.prepareStatement(sqlPatient)) {
                pst.setString(1, patientId);
                pst.setString(2, encryptedBase64); // Lưu chuỗi mã hóa
                pst.executeUpdate();
            }

            // 4. INSERT SMARTCARDS (Giữ nguyên)
            String sqlCard = "INSERT INTO smartcards (card_id, patient_id, card_status) VALUES (?, ?, 'PENDING')";
            try (PreparedStatement pst2 = conn.prepareStatement(sqlCard)) {
                pst2.setString(1, cardId);
                pst2.setString(2, patientId);
                pst2.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Lưu thành công (Đã mã hóa DB)!");
            clearForm();

        } catch (Exception ex) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }


    private void clearForm() {
        txtPatientId.setText("");
        txtFullName.setText("");
        txtDob.setText("");
        txtAddress.setText("");
        txtBloodType.setText("");
        txtAllergies.setText("");
        txtChronicIllness.setText("");
        txtInsuranceId.setText("");
        txtCardId.setText("");
    }
}
