package hospitalcardgui;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransactionPanel extends JPanel {

    private final MainFrame parent;

    // Thông tin bệnh nhân
    private JLabel lblName;
    private JLabel lblDob;
    private JLabel lblAddress;
    private JLabel lblBlood;
    private JLabel lblAllergy;
    private JLabel lblChronic;
    private JLabel lblInsurance;

    // Phần ví
    private JLabel lblBalance;
    private JTextField txtAmount;
    private JButton btnGetBalance;
    private JButton btnCredit;
    private JButton btnDebit;
    private JTextArea txtLog;

    public TransactionPanel(MainFrame parent) {
        this.parent = parent;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10,10));

        // ===== THÔNG TIN BỆNH NHÂN =====
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin bệnh nhân"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3,3,3,3);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Họ tên:"), c);
        c.gridx=1;
        lblName = new JLabel("-");
        infoPanel.add(lblName, c);
        row++;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Ngày sinh:"), c);
        c.gridx=1;
        lblDob = new JLabel("-");
        infoPanel.add(lblDob, c);
        row++;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Quê quán:"), c);
        c.gridx=1;
        lblAddress = new JLabel("-");
        infoPanel.add(lblAddress, c);
        row++;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Nhóm máu:"), c);
        c.gridx=1;
        lblBlood = new JLabel("-");
        infoPanel.add(lblBlood, c);
        row++;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Dị ứng:"), c);
        c.gridx=1;
        lblAllergy = new JLabel("-");
        infoPanel.add(lblAllergy, c);
        row++;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Bệnh mãn tính:"), c);
        c.gridx=1;
        lblChronic = new JLabel("-");
        infoPanel.add(lblChronic, c);
        row++;

        c.gridx=0;c.gridy=row;
        infoPanel.add(new JLabel("Mã BHYT:"), c);
        c.gridx=1;
        lblInsurance = new JLabel("-");
        infoPanel.add(lblInsurance, c);

        add(infoPanel, BorderLayout.NORTH);

        // ===== GIAO DỊCH =====
        JPanel mid = new JPanel(new GridBagLayout());
        mid.setBorder(BorderFactory.createTitledBorder("Ví y tế & giao dịch"));
        c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        mid.add(new JLabel("Số dư:"), c);

        c.gridx = 1;
        lblBalance = new JLabel("0");
        lblBalance.setForeground(new Color(0,120,0));
        lblBalance.setFont(lblBalance.getFont().deriveFont(Font.BOLD));
        mid.add(lblBalance, c);

        c.gridx = 2;
        btnGetBalance = new JButton("Lấy số dư");
        btnGetBalance.addActionListener(e -> onGetBalance());
        mid.add(btnGetBalance, c);

        c.gridx = 0; c.gridy = 1;
        mid.add(new JLabel("Số tiền:"), c);

        c.gridx = 1;
        txtAmount = new JTextField(10);
        mid.add(txtAmount, c);

        c.gridx = 0; c.gridy = 2;
        btnCredit = new JButton("Nạp tiền");
        btnCredit.addActionListener(e -> onCredit());
        mid.add(btnCredit, c);

        c.gridx = 1;
        btnDebit = new JButton("Thanh toán");
        btnDebit.addActionListener(e -> onDebit());
        mid.add(btnDebit, c);

        add(mid, BorderLayout.CENTER);

        // ===== LOG =====
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Lịch sử thao tác (log)"));
        txtLog = new JTextArea(8,40);
        txtLog.setEditable(false);
        bottom.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setTransactionEnabled(false);
    }

    public void appendLog(String msg) {
        txtLog.append(msg + "\n");
    }

    public void setTransactionEnabled(boolean enabled) {
        btnGetBalance.setEnabled(enabled);
        btnCredit.setEnabled(enabled);
        btnDebit.setEnabled(enabled);
    }

    // ==== Load thông tin bệnh nhân từ DB bằng patient_id ====
    public void loadPatientInfoFromDb(String patientId) {
        String sql = "SELECT full_name, date_of_birth, home_address, " +
                     "blood_type, allergies, chronic_illness, health_insurance_id " +
                     "FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, patientId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lblName.setText(rs.getString("full_name"));
                    java.sql.Date dob = rs.getDate("date_of_birth");
                    lblDob.setText(dob != null ? dob.toString() : "");
                    lblAddress.setText(rs.getString("home_address"));
                    lblBlood.setText(rs.getString("blood_type"));
                    lblAllergy.setText(rs.getString("allergies"));
                    lblChronic.setText(rs.getString("chronic_illness"));
                    lblInsurance.setText(rs.getString("health_insurance_id"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            appendLog("Lỗi đọc thông tin bệnh nhân: " + ex.getMessage());
        }

        setTransactionEnabled(true);
        onGetBalance(); // đọc luôn số dư khi vừa load info xong
    }

    // ==== Giao dịch với thẻ ====
    private void onGetBalance() {
        APDUCommands apdu = parent.getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối / xác thực thẻ.");
            return;
        }
        int bal = apdu.getBalance();
        if (bal >= 0) {
            lblBalance.setText(String.valueOf(bal));
            appendLog("Số dư hiện tại: " + bal);
        } else {
            appendLog("Lỗi lấy số dư.");
        }
    }

    private void onCredit() {
        APDUCommands apdu = parent.getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối / xác thực thẻ.");
            return;
        }
        try {
            int amount = Integer.parseInt(txtAmount.getText().trim());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Số tiền > 0.");
                return;
            }
            if (apdu.credit(amount)) {
                appendLog("Nạp +" + amount);
                onGetBalance();
                // TODO: ghi thêm vào transactions_history trong DB nếu cần
            } else {
                appendLog("Nạp tiền thất bại.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ.");
        }
    }

    private void onDebit() {
        APDUCommands apdu = parent.getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối / xác thực thẻ.");
            return;
        }
        try {
            int amount = Integer.parseInt(txtAmount.getText().trim());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Số tiền > 0.");
                return;
            }
            if (apdu.debit(amount)) {
                appendLog("Thanh toán -" + amount);
                onGetBalance();
                // TODO: ghi thêm vào transactions_history trong DB nếu cần
            } else {
                appendLog("Thanh toán thất bại (có thể do số dư không đủ).");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ.");
        }
    }
}
