package hospitalcardgui.admin;

import hospitalcardgui.admin.AdminKeyManager;
import hospitalcardgui.CryptoUtils;
import hospitalcardgui.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;

public class CardManagePanel extends JPanel {

    private JTextField txtKeyword;
    private JComboBox<String> cbField;
    private JTable table;
    private DefaultTableModel model;

    // Thông tin chi tiết
    private JTextField txtEditPatientId, txtEditFullName, txtEditHealthId, 
                       txtEditBloodType, txtEditAllergies, txtEditChronic, 
                       txtEditDob, txtEditAddress;
    private JTextField txtEditCardId, txtEditStatus;

    private String selectedPatientId = null;
    private boolean isEditing = false;

    public CardManagePanel() { initUI(); }

    public void resetAndLoad() {
        table.clearSelection();
        selectedPatientId = null;
        txtKeyword.setText("");
        cbField.setSelectedIndex(0);
        model.setRowCount(0);
        clearEditFields();
        setFormEditable(false);
        isEditing = false;
    }

    private void initUI() {
        TitledBorder border = BorderFactory.createTitledBorder("Quản Lý & Tra Cứu");
        border.setTitleFont(AdminTheme.FONT_BUTTON);
        setBorder(border);
        AdminTheme.applyMainBackground(this);
        setLayout(new BorderLayout(10, 10));

        // 1. TOP
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        cbField = new JComboBox<>(new String[]{"Mã bệnh nhân", "Mã thẻ"});
        txtKeyword = new JTextField(20);
        JButton btnSearch = new JButton("Tìm");
        AdminTheme.stylePrimaryButton(btnSearch);
        btnSearch.addActionListener(e -> search());
        top.add(AdminTheme.label("Tìm theo:")); top.add(cbField); top.add(txtKeyword); top.add(btnSearch);
        add(top, BorderLayout.NORTH);

        // 2. CENTER
        model = new DefaultTableModel(new Object[]{"Mã thẻ", "Mã BN", "Họ tên", "Trạng thái", "Ngày cấp"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> onRowSelected());
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(AdminTheme.BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(AdminTheme.BORDER_SOFT));
        add(scroll, BorderLayout.CENTER);

        // 3. BOTTOM
        JPanel bottom = new JPanel(new GridBagLayout());
        TitledBorder detailBorder = BorderFactory.createTitledBorder("Chi tiết hồ sơ");
        detailBorder.setTitleFont(AdminTheme.FONT_BUTTON);
        bottom.setBorder(detailBorder);
        AdminTheme.applyCardStyle(bottom);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        c.gridx = 0; c.gridy = row; bottom.add(AdminTheme.label("Mã thẻ:"), c);
        txtEditCardId = new JTextField(15); txtEditCardId.setEditable(false);
        c.gridx = 1; bottom.add(txtEditCardId, c); row++;

        c.gridx = 0; c.gridy = row; bottom.add(AdminTheme.label("Trạng thái:"), c);
        txtEditStatus = new JTextField(15); txtEditStatus.setEditable(false);
        c.gridx = 1; bottom.add(txtEditStatus, c); row++;

        c.gridx = 0; c.gridy = row; bottom.add(AdminTheme.label("Mã BN:"), c);
        txtEditPatientId = new JTextField(15); txtEditPatientId.setEditable(false);
        c.gridx = 1; bottom.add(txtEditPatientId, c);

        // Cột 2
        row = 0;
        c.gridx = 2; c.gridy = row; bottom.add(AdminTheme.label("Họ tên:"), c);
        txtEditFullName = new JTextField(15); c.gridx = 3; bottom.add(txtEditFullName, c); row++;

        c.gridx = 2; c.gridy = row; bottom.add(AdminTheme.label("Ngày sinh:"), c);
        txtEditDob = new JTextField(15); c.gridx = 3; bottom.add(txtEditDob, c); row++;

        c.gridx = 2; c.gridy = row; bottom.add(AdminTheme.label("BHYT:"), c);
        txtEditHealthId = new JTextField(15); c.gridx = 3; bottom.add(txtEditHealthId, c); row++;

        c.gridx = 2; c.gridy = row; bottom.add(AdminTheme.label("Địa chỉ:"), c);
        txtEditAddress = new JTextField(15); c.gridx = 3; bottom.add(txtEditAddress, c);

        // Cột 3
        row = 0;
        c.gridx = 4; c.gridy = row; bottom.add(AdminTheme.label("Nhóm máu:"), c);
        txtEditBloodType = new JTextField(10); c.gridx = 5; bottom.add(txtEditBloodType, c); row++;

        c.gridx = 4; c.gridy = row; bottom.add(AdminTheme.label("Dị ứng:"), c);
        txtEditAllergies = new JTextField(10); c.gridx = 5; bottom.add(txtEditAllergies, c); row++;

        c.gridx = 4; c.gridy = row; bottom.add(AdminTheme.label("Bệnh nền:"), c);
        txtEditChronic = new JTextField(10); c.gridx = 5; bottom.add(txtEditChronic, c);

        // BUTTONS
        JPanel pnlBtns = new JPanel();
        pnlBtns.setOpaque(false);
        JButton btnEdit = new JButton("Sửa Thông Tin");
        JButton btnSave = new JButton("Lưu Cập Nhật");
        btnSave.setEnabled(false);
        JButton btnDelete = new JButton("Xóa Hồ Sơ");
        AdminTheme.styleSecondaryButton(btnEdit);
        AdminTheme.stylePrimaryButton(btnSave);
        AdminTheme.styleDangerButton(btnDelete);

        btnEdit.addActionListener(e -> {
            if (selectedPatientId == null) {
                JOptionPane.showMessageDialog(this, "Chưa chọn hồ sơ!"); return;
            }
            isEditing = !isEditing;
            setFormEditable(isEditing);
            btnSave.setEnabled(isEditing);
            btnEdit.setText(isEditing ? "Hủy Sửa" : "Sửa Thông Tin");
            if (!isEditing) loadAndDecryptPatient(selectedPatientId); // Revert nếu hủy
        });

        btnSave.addActionListener(e -> saveUpdate(btnEdit, btnSave));
        btnDelete.addActionListener(e -> deleteCard());

        pnlBtns.add(btnEdit); pnlBtns.add(btnSave); pnlBtns.add(btnDelete);
        c.gridx = 0; c.gridy = 10; c.gridwidth = 6;
        bottom.add(pnlBtns, c);
        add(bottom, BorderLayout.SOUTH);
        setFormEditable(false);
    }

    private void search() {
        String keyword = txtKeyword.getText().trim();
        String sql = "SELECT s.card_id, s.patient_id, p.encrypted_data, p.full_name, s.card_status, s.date_issued " +
                     "FROM smartcards s JOIN patients p ON s.patient_id = p.patient_id " +
                     "WHERE s.card_id ILIKE ? OR s.patient_id ILIKE ? ORDER BY s.date_issued DESC";
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, "%" + keyword + "%");
            pst.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String displayName = rs.getString("full_name");
                    String encData = rs.getString("encrypted_data");
                    if (encData != null && !encData.isEmpty()) {
                        try {
                            if (AdminKeyManager.isKeyReady()) {
                                byte[] encBytes = Base64.getDecoder().decode(encData);
                                byte[] decBytes = CryptoUtils.aesDecrypt(encBytes, AdminKeyManager.getKey());
                                displayName = new String(decBytes, StandardCharsets.UTF_8).split("\\|")[0];
                            } else displayName = "Locked";
                        } catch (Exception e) { displayName = "Error"; }
                    }
                    Timestamp ts = rs.getTimestamp("date_issued");
                    String dateStr = (ts != null) ? ts.toLocalDateTime().toLocalDate().toString() : "";
                    model.addRow(new Object[]{rs.getString("card_id"), rs.getString("patient_id"), displayName, rs.getString("card_status"), dateStr});
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        txtEditCardId.setText(String.valueOf(model.getValueAt(row, 0)));
        txtEditPatientId.setText(String.valueOf(model.getValueAt(row, 1)));
        txtEditStatus.setText(String.valueOf(model.getValueAt(row, 3)));
        selectedPatientId = txtEditPatientId.getText();
        isEditing = false;
        setFormEditable(false);
        loadAndDecryptPatient(selectedPatientId);
    }

    private void loadAndDecryptPatient(String pid) {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, pid);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String encData = rs.getString("encrypted_data");
                    if (encData != null && !encData.isEmpty()) {
                        try {
                            if (!AdminKeyManager.isKeyReady()) return;
                            byte[] encBytes = Base64.getDecoder().decode(encData);
                            byte[] decBytes = CryptoUtils.aesDecrypt(encBytes, AdminKeyManager.getKey());
                            String plain = new String(decBytes, StandardCharsets.UTF_8);
                            
                            // MAPPING ĐÚNG CHUẨN 0->6
                            String[] p = plain.split("\\|");
                            if (p.length >= 7) {
                                txtEditFullName.setText(p[0]);
                                txtEditDob.setText(p[1]);
                                txtEditBloodType.setText(p[2]);
                                txtEditAllergies.setText(p[3]);
                                txtEditChronic.setText(p[4]);
                                txtEditHealthId.setText(p[5]);
                                txtEditAddress.setText(p[6]);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    } else {
                        // Load dữ liệu chưa mã hóa
                        txtEditFullName.setText(rs.getString("full_name"));
                        txtEditDob.setText(String.valueOf(rs.getDate("date_of_birth")));
                        txtEditHealthId.setText(rs.getString("health_insurance_id"));
                        txtEditAddress.setText(rs.getString("home_address"));
                        txtEditBloodType.setText(rs.getString("blood_type"));
                        txtEditAllergies.setText(rs.getString("allergies"));
                        txtEditChronic.setText(rs.getString("chronic_illness"));
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void saveUpdate(JButton btnEdit, JButton btnSave) {
        if (selectedPatientId == null || !AdminKeyManager.isKeyReady()) return;
        try {
            // GOM DỮ LIỆU THEO ĐÚNG THỨ TỰ 0->6
            String rawData = txtEditFullName.getText().trim() + "|" +
                             txtEditDob.getText().trim() + "|" +
                             txtEditBloodType.getText().trim() + "|" +
                             txtEditAllergies.getText().trim() + "|" +
                             txtEditChronic.getText().trim() + "|" +
                             txtEditHealthId.getText().trim() + "|" +
                             txtEditAddress.getText().trim();

            byte[] encBytes = CryptoUtils.aesEncrypt(rawData.getBytes(StandardCharsets.UTF_8), AdminKeyManager.getKey());
            String encBase64 = Base64.getEncoder().encodeToString(encBytes);

            String sql = "UPDATE patients SET encrypted_data = ?, full_name = ? WHERE patient_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, encBase64);
                pst.setString(2, encBase64);
                pst.setString(3, selectedPatientId);
                if (pst.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
                    isEditing = false;
                    setFormEditable(false);
                    btnSave.setEnabled(false);
                    btnEdit.setText("Sửa Thông Tin");
                    search();
                }
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage()); }
    }

    private void setFormEditable(boolean editable) {
        txtEditFullName.setEditable(editable);
        txtEditDob.setEditable(editable);
        txtEditHealthId.setEditable(editable);
        txtEditAddress.setEditable(editable);
        txtEditBloodType.setEditable(editable);
        txtEditAllergies.setEditable(editable);
        txtEditChronic.setEditable(editable);
    }

    private void clearEditFields() {
        txtEditCardId.setText(""); txtEditStatus.setText(""); txtEditPatientId.setText("");
        txtEditFullName.setText(""); txtEditDob.setText(""); txtEditHealthId.setText("");
        txtEditAddress.setText(""); txtEditBloodType.setText(""); txtEditAllergies.setText(""); txtEditChronic.setText("");
    }

    private void deleteCard() {
        if (selectedPatientId == null) return;
        if (JOptionPane.showConfirmDialog(this, "Xóa thẻ này?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement("DELETE FROM smartcards WHERE patient_id = ?")) {
                p1.setString(1, selectedPatientId); p1.executeUpdate();
            }
            try (PreparedStatement p2 = conn.prepareStatement("DELETE FROM patients WHERE patient_id = ?")) {
                p2.setString(1, selectedPatientId); p2.executeUpdate();
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Đã xóa.");
            resetAndLoad();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
