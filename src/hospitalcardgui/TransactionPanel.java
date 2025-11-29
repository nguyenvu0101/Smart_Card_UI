package hospitalcardgui;

import javax. swing.*;
import java.awt.*;

public class TransactionPanel extends JPanel {

    private final MainFrame parent;

    private JLabel lblName, lblDob, lblAddress, lblBlood, lblAllergy, lblChronic, lblInsurance;
    private JLabel lblBalance;
    private JTextField txtAmount;
    private JButton btnGetBalance, btnCredit, btnDebit;
    private JTextArea txtLog;
    
    private final CardManager cardManager = CardManager. getInstance();

    public TransactionPanel(MainFrame parent) {
        this.parent = parent;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10,10));

        // ===== THÔNG TIN BỆNH NHÂN =====
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin bệnh nhân (Từ thẻ)"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3,3,3,3);
        c.anchor = GridBagConstraints. WEST;

        int row = 0;

        c.gridx=0; c.gridy=row; infoPanel.add(new JLabel("Họ tên:"), c);
        c.gridx=1; lblName = new JLabel("-"); lblName.setFont(new Font("Arial", Font. BOLD, 14)); lblName.setForeground(Color.BLUE);
        infoPanel.add(lblName, c); row++;

        c.gridx=0; c.gridy=row; infoPanel.add(new JLabel("Ngày sinh:"), c);
        c. gridx=1; lblDob = new JLabel("-"); infoPanel.add(lblDob, c); row++;

        c.gridx=0; c. gridy=row; infoPanel.add(new JLabel("Địa chỉ:"), c);
        c.gridx=1; lblAddress = new JLabel("-"); infoPanel.add(lblAddress, c); row++;

        c.gridx=0; c.gridy=row; infoPanel.add(new JLabel("Nhóm máu:"), c);
        c. gridx=1; lblBlood = new JLabel("-"); infoPanel.add(lblBlood, c); row++;

        c. gridx=0; c.gridy=row; infoPanel. add(new JLabel("Dị ứng:"), c);
        c.gridx=1; lblAllergy = new JLabel("-"); infoPanel.add(lblAllergy, c); row++;

        c.gridx=0; c.gridy=row; infoPanel.add(new JLabel("Bệnh mãn tính:"), c);
        c. gridx=1; lblChronic = new JLabel("-"); infoPanel.add(lblChronic, c); row++;

        c.gridx=0; c.gridy=row; infoPanel.add(new JLabel("Mã BHYT:"), c);
        c.gridx=1; lblInsurance = new JLabel("-"); infoPanel.add(lblInsurance, c);

        add(infoPanel, BorderLayout. NORTH);

        // ===== VÍ =====
        JPanel mid = new JPanel(new GridBagLayout());
        mid.setBorder(BorderFactory.createTitledBorder("Ví y tế & giao dịch"));
        c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints. HORIZONTAL;

        c.gridx=0; c.gridy=0; mid.add(new JLabel("Số dư:"), c);
        c.gridx=1; lblBalance = new JLabel("0"); lblBalance.setForeground(new Color(0,120,0)); lblBalance.setFont(lblBalance.getFont().deriveFont(Font.BOLD));
        mid.add(lblBalance, c);
        c.gridx=2; btnGetBalance = new JButton("Lấy số dư"); btnGetBalance.addActionListener(e -> onGetBalance());
        mid.add(btnGetBalance, c);

        c.gridx=0; c. gridy=1; mid.add(new JLabel("Số tiền:"), c);
        c.gridx=1; txtAmount = new JTextField(10); mid.add(txtAmount, c);

        c.gridx=0; c.gridy=2; btnCredit = new JButton("Nạp tiền"); btnCredit.addActionListener(e -> onCredit());
        mid.add(btnCredit, c);
        c.gridx=1; btnDebit = new JButton("Thanh toán"); btnDebit. addActionListener(e -> onDebit());
        mid.add(btnDebit, c);

        add(mid, BorderLayout.CENTER);

        // ===== LOG =====
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Lịch sử thao tác"));
        txtLog = new JTextArea(8,40);
        txtLog.setEditable(false);
        bottom.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        add(bottom, BorderLayout. SOUTH);

        setTransactionEnabled(false);
    }

    public void appendLog(String msg) {
        txtLog.append(msg + "\n");
    }

    public void setTransactionEnabled(boolean enabled) {
        btnGetBalance.setEnabled(enabled);
        btnCredit. setEnabled(enabled);
        btnDebit.setEnabled(enabled);
    }

    public void setPatientData(String rawData) {
        try {
            // Format: Name|Dob|Blood|Allergies|Chronic|HealthId|Address
            String[] parts = rawData. split("\\|");
            
            if (parts.length >= 7) {
                lblName.setText(parts[0]);
                lblDob.setText(parts[1]);
                lblBlood.setText(parts[2]);
                lblAllergy.setText(parts[3]);
                lblChronic.setText(parts[4]);
                lblInsurance.setText(parts[5]);
                lblAddress.setText(parts[6]);
                
                appendLog("Đã tải thông tin: " + parts[0]);
                setTransactionEnabled(true);
                onGetBalance();
            } else {
                appendLog("Dữ liệu không đúng format!  Parts: " + parts.length);
                appendLog("Raw: " + rawData);
            }
        } catch (Exception e) {
            appendLog("Lỗi hiển thị: " + e.getMessage());
        }
    }

    private APDUCommands getApdu() {
        return parent.getApdu();
    }

    private void onGetBalance() {
        APDUCommands apdu = getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối thẻ. "); return;
        }
        int bal = apdu.getBalance();
        if (bal >= 0) {
            lblBalance.setText(String.valueOf(bal));
            appendLog("Số dư: " + bal);
        } else {
            appendLog("Lỗi lấy số dư");
        }
    }

    private void onCredit() {
        APDUCommands apdu = getApdu();
        if (apdu == null) { JOptionPane.showMessageDialog(this, "Chưa kết nối. "); return; }
        try {
            int amt = Integer.parseInt(txtAmount. getText(). trim());
            if (amt <= 0) { JOptionPane.showMessageDialog(this, "Số tiền > 0"); return; }
            if (apdu.credit(amt)) {
                appendLog("Nạp +" + amt);
                onGetBalance();
            } else appendLog("Nạp thất bại");
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Số không hợp lệ"); }
    }

    private void onDebit() {
        APDUCommands apdu = getApdu();
        if (apdu == null) { JOptionPane. showMessageDialog(this, "Chưa kết nối. "); return; }
        try {
            int amt = Integer.parseInt(txtAmount.getText().trim());
            if (amt <= 0) { JOptionPane.showMessageDialog(this, "Số tiền > 0"); return; }
            if (apdu.debit(amt)) {
                appendLog("Thanh toán -" + amt);
                onGetBalance();
            } else appendLog("Thanh toán thất bại (Không đủ?)");
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Số không hợp lệ"); }
    }

    public void loadPatientInfoFromDb(String pid) {
        // Placeholder - implement nếu cần
    }
}