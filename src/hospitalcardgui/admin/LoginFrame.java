package hospitalcardgui.admin;

import hospitalcardgui.admin.AdminKeyManager;
import hospitalcardgui.DatabaseConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;

    public LoginFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("Đăng nhập Hệ thống Quản trị");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null); // Căn giữa màn hình
        setResizable(false);

        // Layout chính
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(panel);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Tiêu đề
        JLabel lblTitle = new JLabel("ADMIN LOGIN");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(lblTitle, c);

        // Username
        c.gridwidth = 1; c.gridy = 1;
        panel.add(new JLabel("Tài khoản:"), c);
        
        c.gridx = 1;
        txtUsername = new JTextField("admin", 15); // Mặc định điền sẵn admin
        panel.add(txtUsername, c);

        // Password
        c.gridx = 0; c.gridy = 2;
        panel.add(new JLabel("Mật khẩu:"), c);
        
        c.gridx = 1;
        txtPassword = new JPasswordField(15);
        panel.add(txtPassword, c);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnLogin = new JButton("Đăng nhập");
        btnExit = new JButton("Thoát");

        btnPanel.add(btnLogin);
        btnPanel.add(btnExit);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        panel.add(btnPanel, c);

        // --- XỬ LÝ SỰ KIỆN ---
        
        // Nút Đăng nhập
        btnLogin.addActionListener((ActionEvent e) -> {
            handleLogin();
        });
        
        // Nút Thoát
        btnExit.addActionListener(e -> System.exit(0));

        // Cho phép ấn Enter để login
        getRootPane().setDefaultButton(btnLogin);
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Gọi hàm xác thực trong DatabaseConnection (trả về true nếu user/pass đúng)
        boolean isValid = DatabaseConnection.verifyAdmin(username, password);

        if (isValid) {
            // 2. ĐĂNG NHẬP THÀNH CÔNG -> SINH KEY QUẢN TRỊ
            // Đây là bước quan trọng để kích hoạt tính năng mã hóa DB
            AdminKeyManager.generateKeyFromPassword(password);
            System.out.println("Login successful. Admin Key generated in RAM.");

            // 3. Đóng LoginFrame
            this.dispose();
            
            // 4. Mở AdminFrame
            SwingUtilities.invokeLater(() -> {
                new AdminFrame().setVisible(true); 
            });
            
        } else {
            JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!", "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
