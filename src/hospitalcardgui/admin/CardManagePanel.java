package hospitalcardgui.admin;

import hospitalcardgui.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class CardManagePanel extends JPanel {

    private JTextField txtKeyword;
    private JComboBox<String> cbField;
    private JTable table;
    private DefaultTableModel model;

    // Thông tin bệnh nhân (có thể sửa, trừ mã)
    private JTextField txtEditPatientId;    // patient_id (read-only)
    private JTextField txtEditFullName;     // full_name
    private JTextField txtEditHealthId;     // health_insurance_id
    private JTextField txtEditBloodType;    // blood_type
    private JTextField txtEditAllergies;    // allergies
    private JTextField txtEditChronic;      // chronic_illness
    private JTextField txtEditDob;          // date_of_birth (YYYY-MM-DD)
    private JTextField txtEditAddress;      // home_address

    // Thông tin thẻ
    private JTextField txtEditCardId;       // card_id (read-only)
    private JTextField txtEditStatus;       // card_status

    // Lưu patient_id đang chọn
    private String selectedPatientId = null;

    public CardManagePanel() {
        initUI();
    }

    // Gọi từ AdminFrame khi chuyển sang tab này để reset + load lại
   public void resetAndLoad() {
    table.clearSelection();
    selectedPatientId = null;
    txtKeyword.setText("");
    cbField.setSelectedIndex(0);
    model.setRowCount(0);   // chỉ xóa bảng, KHÔNG search
    clearEditFields();
}


    private void initUI() {
        setBorder(BorderFactory.createTitledBorder("Quản Lý Bệnh Nhân"));
        setLayout(new BorderLayout(10, 10));

        // ===== THANH TÌM KIẾM =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cbField = new JComboBox<>(new String[]{
                "Tên bệnh nhân", "Mã bệnh nhân", "Mã thẻ", "Mã bảo hiểm"
        });
        txtKeyword = new JTextField(20);
        JButton btnSearch = new JButton("Tìm");
        btnSearch.addActionListener(e -> search());

        top.add(new JLabel("Tìm theo:"));
        top.add(cbField);
        top.add(txtKeyword);
        top.add(btnSearch);
        add(top, BorderLayout.NORTH);

        // ===== BẢNG KẾT QUẢ =====
        model = new DefaultTableModel(
                new Object[]{"Mã thẻ", "Mã BN", "Họ tên",
                        "Mã BHYT", "Trạng thái", "Ngày phát hành"}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== KHU SỬA HỒ SƠ & THẺ =====
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Cập nhật thẻ & hồ sơ bệnh nhân"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Cột 1: thông tin thẻ
        c.gridx = 0; c.gridy = row;
        bottom.add(new JLabel("Mã thẻ (read-only):"), c);
        c.gridx = 1;
        txtEditCardId = new JTextField(15);
        txtEditCardId.setEditable(false);
        bottom.add(txtEditCardId, c);
        row++;

        c.gridx = 0; c.gridy = row;
        bottom.add(new JLabel("Trạng thái thẻ:"), c);
        c.gridx = 1;
        txtEditStatus = new JTextField(15);
        bottom.add(txtEditStatus, c);
        row++;

        // Cột 2: thông tin bệnh nhân
        row = 0;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Mã BN (read-only):"), c);
        c.gridx = 3;
        txtEditPatientId = new JTextField(15);
        txtEditPatientId.setEditable(false);
        bottom.add(txtEditPatientId, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Họ tên:"), c);
        c.gridx = 3;
        txtEditFullName = new JTextField(15);
        bottom.add(txtEditFullName, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Mã BHYT:"), c);
        c.gridx = 3;
        txtEditHealthId = new JTextField(15);
        bottom.add(txtEditHealthId, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Nhóm máu:"), c);
        c.gridx = 3;
        txtEditBloodType = new JTextField(15);
        bottom.add(txtEditBloodType, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Dị ứng:"), c);
        c.gridx = 3;
        txtEditAllergies = new JTextField(15);
        bottom.add(txtEditAllergies, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Bệnh mãn tính:"), c);
        c.gridx = 3;
        txtEditChronic = new JTextField(15);
        bottom.add(txtEditChronic, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Ngày sinh (YYYY-MM-DD):"), c);
        c.gridx = 3;
        txtEditDob = new JTextField(15);
        bottom.add(txtEditDob, c);
        row++;

        c.gridx = 2; c.gridy = row;
        bottom.add(new JLabel("Quê quán / Địa chỉ:"), c);
        c.gridx = 3;
        txtEditAddress = new JTextField(15);
        bottom.add(txtEditAddress, c);
        row++;

        // Nút
        JButton btnUpdate = new JButton("Cập nhật tất cả");
        btnUpdate.addActionListener(e -> updateAllData());
        JButton btnDelete = new JButton("Xóa thẻ");
        btnDelete.addActionListener(e -> deleteCard());

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        bottom.add(btnUpdate, c);
        c.gridx = 2; c.gridy = row; c.gridwidth = 2;
        bottom.add(btnDelete, c);

        add(bottom, BorderLayout.SOUTH);
    }

    // ===== TÌM KIẾM =====
    private void search() {
        String keyword = txtKeyword.getText().trim();
        String field;
        switch (cbField.getSelectedIndex()) {
            case 0: field = "p.full_name"; break;
            case 1: field = "p.patient_id"; break;
            case 2: field = "s.card_id"; break;
            case 3: field = "p.health_insurance_id"; break;
            default: field = "p.full_name";
        }

        String sql =
                "SELECT s.card_id, s.patient_id, p.full_name, " +
                "       p.health_insurance_id, s.card_status, s.date_issued " +
                "FROM smartcards s " +
                "JOIN patients p ON s.patient_id = p.patient_id " +
                "WHERE " + field + " ILIKE ? " +
                "ORDER BY s.date_issued DESC";

        model.setRowCount(0);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, "%" + keyword + "%");

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date_issued");
                    String dateStr = "";
                    if (ts != null) {
                        LocalDateTime ldt = ts.toLocalDateTime();
                        dateStr = ldt.toLocalDate().toString(); // YYYY-MM-DD
                    }

                    model.addRow(new Object[]{
                            rs.getString("card_id"),
                            rs.getString("patient_id"),
                            rs.getString("full_name"),
                            rs.getString("health_insurance_id"),
                            rs.getString("card_status"),
                            dateStr
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tìm kiếm: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        table.clearSelection();
        clearEditFields();
    }

    private void clearEditFields() {
        txtEditCardId.setText("");
        txtEditStatus.setText("");
        txtEditPatientId.setText("");
        txtEditFullName.setText("");
        txtEditHealthId.setText("");
        txtEditBloodType.setText("");
        txtEditAllergies.setText("");
        txtEditChronic.setText("");
        txtEditDob.setText("");
        txtEditAddress.setText("");
        selectedPatientId = null;
    }

    // ===== KHI CHỌN 1 DÒNG =====
    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        String cardId = String.valueOf(model.getValueAt(row, 0));
        String patientId = String.valueOf(model.getValueAt(row, 1));

        txtEditCardId.setText(cardId);
        txtEditStatus.setText(String.valueOf(model.getValueAt(row, 4)));
        txtEditPatientId.setText(patientId);
        selectedPatientId = patientId;

        loadPatientDetails(patientId);
    }

    // ===== LOAD CHI TIẾT BỆNH NHÂN =====
    private void loadPatientDetails(String patientId) {
        String sql = "SELECT full_name, health_insurance_id, " +
                     "blood_type, allergies, chronic_illness, " +
                     "date_of_birth, home_address " +
                     "FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, patientId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    txtEditFullName.setText(rs.getString("full_name"));
                    txtEditHealthId.setText(rs.getString("health_insurance_id"));
                    txtEditBloodType.setText(rs.getString("blood_type"));
                    txtEditAllergies.setText(rs.getString("allergies"));
                    txtEditChronic.setText(rs.getString("chronic_illness"));

                    java.sql.Date dob = rs.getDate("date_of_birth");
                    txtEditDob.setText(dob != null ? dob.toString() : "");
                    txtEditAddress.setText(rs.getString("home_address"));
                } else {
                    clearEditFields();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            clearEditFields();
            JOptionPane.showMessageDialog(this,
                    "Lỗi đọc chi tiết bệnh nhân: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== CẬP NHẬT CẢ THẺ VÀ HỒ SƠ =====
    private void updateAllData() {
        if (selectedPatientId == null || selectedPatientId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Chưa chọn bệnh nhân để cập nhật.");
            return;
        }

        String newStatus    = txtEditStatus.getText().trim();
        String newFullName  = txtEditFullName.getText().trim();
        String newHealthId  = txtEditHealthId.getText().trim();
        String newBloodType = txtEditBloodType.getText().trim();
        String newAllergies = txtEditAllergies.getText().trim();
        String newChronic   = txtEditChronic.getText().trim();
        String newDob       = txtEditDob.getText().trim();
        String newAddress   = txtEditAddress.getText().trim();

        if (newStatus.isEmpty() || newFullName.isEmpty() || newHealthId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Trạng thái, Họ tên và Mã BHYT không được để trống.");
            return;
        }

        boolean okCard = updateCardStatus(txtEditCardId.getText().trim(), newStatus);
        boolean okPatient = updatePatientDetails(
                selectedPatientId, newFullName, newHealthId,
                newBloodType, newAllergies, newChronic,
                newDob, newAddress
        );

        if (okCard || okPatient) {
            JOptionPane.showMessageDialog(this, "Cập nhật thành công.");
            resetAndLoad();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Không có bản ghi nào được cập nhật.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean updateCardStatus(String cardId, String status) {
        String sql = "UPDATE smartcards SET card_status = ? WHERE card_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, status);
            pst.setString(2, cardId);
            return pst.executeUpdate() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi cập nhật trạng thái thẻ: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean updatePatientDetails(String patientId,
                                         String fullName,
                                         String healthId,
                                         String bloodType,
                                         String allergies,
                                         String chronic,
                                         String dobStr,
                                         String address) {
        String sql = "UPDATE patients SET full_name = ?, " +
                     "health_insurance_id = ?, blood_type = ?, " +
                     "allergies = ?, chronic_illness = ?, " +
                     "date_of_birth = ?, home_address = ? " +
                     "WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, fullName);
            pst.setString(2, healthId);
            pst.setString(3, bloodType);
            pst.setString(4, allergies);
            pst.setString(5, chronic);

            if (dobStr == null || dobStr.isEmpty()) {
                pst.setNull(6, java.sql.Types.DATE);
            } else {
                pst.setDate(6, java.sql.Date.valueOf(dobStr)); // YYYY-MM-DD
            }

            pst.setString(7, address);
            pst.setString(8, patientId);

            return pst.executeUpdate() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi cập nhật hồ sơ bệnh nhân: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ===== XÓA THẺ =====
    private void deleteCard() {
        String cardId = txtEditCardId.getText().trim();
        if (cardId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Chưa chọn thẻ để xóa.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa thẻ " + cardId + " khỏi hệ thống?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM smartcards WHERE card_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, cardId);
            int n = pst.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "Đã xóa thẻ.");
                resetAndLoad();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thẻ để xóa.",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi xóa thẻ: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
