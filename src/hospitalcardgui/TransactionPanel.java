package hospitalcardgui;

import javax. swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

public class TransactionPanel extends JPanel {

    private final MainFrame parent;
    
    // Component cho Ảnh đại diện
    private JLabel lblAvatar;
    private JButton btnUploadImg;
    private static final int AVATAR_WIDTH = 100; // Kích thước hiển thị
    private static final int AVATAR_HEIGHT = 120;

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
        setLayout(new BorderLayout(10, 10));

        // =================================================================
        // 1. CONTAINER TRÊN CÙNG (Gồm Avatar bên trái + Info bên phải)
        // =================================================================
        JPanel topContainer = new JPanel(new BorderLayout(10, 0));
        topContainer.setBorder(BorderFactory.createTitledBorder("Thông tin bệnh nhân"));

        // --- A. PANEL ẢNH (BÊN TRÁI) ---
        JPanel avatarPanel = new JPanel(new BorderLayout(5, 5));
        
        // Label hiển thị ảnh
        lblAvatar = new JLabel("No Image", SwingConstants.CENTER);
        lblAvatar.setPreferredSize(new Dimension(AVATAR_WIDTH, AVATAR_HEIGHT));
        lblAvatar.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // Nút chọn ảnh
        btnUploadImg = new JButton("Chọn & Lưu Ảnh");
        btnUploadImg.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btnUploadImg.addActionListener(e -> onUploadImage());

        avatarPanel.add(lblAvatar, BorderLayout.CENTER);
        avatarPanel.add(btnUploadImg, BorderLayout.SOUTH);

        // Thêm vào bên trái container
        topContainer.add(avatarPanel, BorderLayout.WEST);


        // --- B. PANEL THÔNG TIN CHỮ (BÊN PHẢI - Code cũ của bạn) ---
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addLabelInfo(infoPanel, c, "Họ tên:", lblName = new JLabel("-"), row++);
        addLabelInfo(infoPanel, c, "Ngày sinh:", lblDob = new JLabel("-"), row++);
        addLabelInfo(infoPanel, c, "Quê quán:", lblAddress = new JLabel("-"), row++);
        addLabelInfo(infoPanel, c, "Nhóm máu:", lblBlood = new JLabel("-"), row++);
        addLabelInfo(infoPanel, c, "Dị ứng:", lblAllergy = new JLabel("-"), row++);
        addLabelInfo(infoPanel, c, "Bệnh mãn tính:", lblChronic = new JLabel("-"), row++);
        addLabelInfo(infoPanel, c, "Mã BHYT:", lblInsurance = new JLabel("-"), row++);

        // Thêm vào giữa container
        topContainer.add(infoPanel, BorderLayout.CENTER);

        // Thêm Container lớn vào Frame
        add(topContainer, BorderLayout.NORTH);


        // =================================================================
        // 2. GIAO DỊCH (GIỮ NGUYÊN CODE CŨ)
        // =================================================================
        JPanel mid = new JPanel(new GridBagLayout());
        mid.setBorder(BorderFactory.createTitledBorder("Ví y tế & giao dịch"));
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        mid.add(new JLabel("Số dư:"), c);

        c.gridx = 1;
        lblBalance = new JLabel("0");
        lblBalance.setForeground(new Color(0, 120, 0));
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

        // =================================================================
        // 3. LOG (GIỮ NGUYÊN CODE CŨ)
        // =================================================================
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("Lịch sử thao tác (log)"));
        txtLog = new JTextArea(8, 40);
        txtLog.setEditable(false);
        bottom.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setTransactionEnabled(false);
    }

    // Hàm phụ trợ để add label cho gọn code initUI
    private void addLabelInfo(JPanel p, GridBagConstraints c, String title, JLabel lblVal, int row) {
        c.gridx = 0; c.gridy = row;
        p.add(new JLabel(title), c);
        c.gridx = 1;
        p.add(lblVal, c);
    }
    
    // =================================================================
    // XỬ LÝ ẢNH (LOGIC MỚI)
    // =================================================================
    private void onUploadImage() {
        APDUCommands apdu = parent.getApdu();
        if (apdu == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối thẻ!");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh thẻ (JPG/PNG)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // 1. Đọc và Resize ảnh (Bắt buộc vì bộ nhớ thẻ rất nhỏ)
                BufferedImage originalImage = ImageIO.read(selectedFile);
                Image scaledImage = originalImage.getScaledInstance(AVATAR_WIDTH, AVATAR_HEIGHT, Image.SCALE_SMOOTH);
                
                // Hiển thị lên giao diện ngay
                lblAvatar.setIcon(new ImageIcon(scaledImage));
                lblAvatar.setText("");

                // 2. Chuyển đổi sang byte array để gửi xuống thẻ
                BufferedImage bufferedScaled = new BufferedImage(AVATAR_WIDTH, AVATAR_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = bufferedScaled.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedScaled, "jpg", baos); // Nén JPG để nhẹ nhất
                byte[] imageBytes = baos.toByteArray();

                // Kiểm tra kích thước (Ví dụ giới hạn 10KB)
                if (imageBytes.length > 10240) {
                    JOptionPane.showMessageDialog(this, "Ảnh sau khi nén vẫn quá lớn (>10KB). Chọn ảnh đơn giản hơn.");
                    return;
                }

                appendLog("Đang tải ảnh lên thẻ... Size: " + imageBytes.length + " bytes");

                // 3. Gửi xuống thẻ (Cần implement hàm này trong APDUCommands hoặc gọi trực tiếp ở đây)
                // Lưu ý: Phải chia nhỏ gói tin (Chunking) vì APDU chỉ chịu được ~250 byte/lần
                boolean success = apdu.uploadImageToCard(imageBytes); 
                
                if (success) {
                    appendLog("Lưu ảnh vào thẻ thành công!");
                } else {
                    appendLog("Lưu ảnh thất bại.");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                appendLog("Lỗi xử lý ảnh: " + ex.getMessage());
            }
        }
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