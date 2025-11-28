package hospitalcardgui.admin;

import hospitalcardgui.DatabaseConnection;

import javax.swing.*;
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
        setBorder(BorderFactory.createTitledBorder("Thông Tin Bệnh Nhân & Cấp Mã Thẻ"));
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints base = new GridBagConstraints();
        base.insets = new Insets(4, 4, 4, 4);
        base.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        form.add(new JLabel("Mã Bệnh Nhân:"), at(base, 0, row));
        form.add(txtPatientId = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Họ tên:"), at(base, 0, row));
        form.add(txtFullName = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Ngày sinh (YYYY-MM-DD):"), at(base, 0, row));
        form.add(txtDob = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Địa chỉ:"), at(base, 0, row));
        form.add(txtAddress = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Nhóm máu:"), at(base, 0, row));
        form.add(txtBloodType = new JTextField(5), at(base, 1, row++));

        form.add(new JLabel("Dị ứng:"), at(base, 0, row));
        form.add(txtAllergies = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Bệnh mãn tính:"), at(base, 0, row));
        form.add(txtChronicIllness = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Mã bảo hiểm:"), at(base, 0, row));
        form.add(txtInsuranceId = new JTextField(20), at(base, 1, row++));

        form.add(new JLabel("Mã thẻ (Card ID):"), at(base, 0, row));
        form.add(txtCardId = new JTextField(20), at(base, 1, row++));

        add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Lưu Thông Tin (DB Only)");
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

        if (patientId.isEmpty() || cardId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bắt buộc nhập mã bệnh nhân và mã thẻ.");
            return;
        }

        Connection conn = null; // 1. Khai báo ở đây
        try {
            conn = DatabaseConnection.getConnection(); // 2. Lấy connection
            conn.setAutoCommit(false);

            // 1. Insert vào bảng patients
            String sqlPatient = "INSERT INTO patients (patient_id, full_name, date_of_birth, home_address, " +
                    "blood_type, allergies, chronic_illness, health_insurance_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pst = conn.prepareStatement(sqlPatient)) {
                pst.setString(1, patientId);
                pst.setString(2, txtFullName.getText().trim());
                
                String dobStr = txtDob.getText().trim();
                if (dobStr.isEmpty()) {
                    pst.setNull(3, java.sql.Types.DATE);
                } else {
                    pst.setDate(3, java.sql.Date.valueOf(dobStr));
                }

                pst.setString(4, txtAddress.getText().trim());
                pst.setString(5, txtBloodType.getText().trim());
                pst.setString(6, txtAllergies.getText().trim());
                pst.setString(7, txtChronicIllness.getText().trim());
                pst.setString(8, txtInsuranceId.getText().trim());
                pst.executeUpdate();
            }

            // 2. Insert vào bảng smartcards
            String sqlCard = "INSERT INTO smartcards (card_id, patient_id, card_status, card_public_key) VALUES (?, ?, 'PENDING', NULL)";

            try (PreparedStatement pst2 = conn.prepareStatement(sqlCard)) {
                pst2.setString(1, cardId);
                pst2.setString(2, patientId);
                pst2.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, 
                    "Đã lưu thông tin bệnh nhân vào CSDL thành công!\n" +
                    "Vui lòng chuyển sang tab 'Ghi thẻ' để thực hiện phát hành thẻ vật lý.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            
            clearForm();

        } catch (Exception ex) {
            // 3. Giờ gọi rollback được vì conn khai báo bên ngoài
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu vào CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            // 4. Đóng connection thủ công
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
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
