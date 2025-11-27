package hospitalcardgui;

import javax.smartcardio.CardChannel;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private final CardManager cardManager = new CardManager();
    private APDUCommands apdu;

    // panel kết nối
    private JButton btnConnect;
    private JLabel lblStatus;

    // CardLayout cho 2 trang
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private PinPanel pinPanel;
    private TransactionPanel transactionPanel;

    // Lưu patient_id sau khi đọc từ thẻ
    private String currentPatientId;

    public MainFrame() {
        initUI();
        testSupabase();
    }

    /* ====== public API cho panel con dùng ====== */

    public APDUCommands getApdu() {
        return apdu;
    }

    public void log(String msg) {
        if (transactionPanel != null) {
            transactionPanel.appendLog(msg);
        }
    }

    public void showPinPage() {
        cardLayout.show(cardPanel, "PIN");
    }

    public void showTransactionPage() {
        // Enable chức năng giao dịch khi vào trang này
        transactionPanel.setTransactionEnabled(true);
        cardLayout.show(cardPanel, "TX");
    }

    public void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }

    // Lưu và lấy patient_id
    public String getCurrentPatientId() {
        return currentPatientId;
    }

    public void setCurrentPatientId(String id) {
        this.currentPatientId = id;
    }

    // Gọi từ PinPanel sau khi PIN đúng và đã đọc patient_id
    public void loadPatientInfoOnTransactionPanel() {
        if (transactionPanel != null && currentPatientId != null) {
            transactionPanel.loadPatientInfoFromDb(currentPatientId);
        }
    }

    /* ======================================= */

    private void initUI() {
        setTitle("Hệ thống thẻ bệnh nhân (Kiosk)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(root);

        // ===== KẾT NỐI THẺ (trên cùng) =====
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder("Kết nối thẻ"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        btnConnect = new JButton("Kết nối thẻ");
        btnConnect.addActionListener(e -> onConnect());
        top.add(btnConnect, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 2;
        lblStatus = new JLabel("Trạng thái: chưa kết nối");
        lblStatus.setForeground(Color.RED);
        top.add(lblStatus, c);

        root.add(top, BorderLayout.NORTH);

        // ===== CardLayout cho 2 trang con =====
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        pinPanel = new PinPanel(this);
        transactionPanel = new TransactionPanel(this);

        cardPanel.add(pinPanel, "PIN");
        cardPanel.add(transactionPanel, "TX");

        root.add(cardPanel, BorderLayout.CENTER);

        // Mặc định vào trang nhập PIN
        showPinPage();
        pinPanel.enablePinInput(false); // chưa kết nối thẻ
    }

    private void testSupabase() {
        try (Connection c = DatabaseConnection.getConnection()) {
            log("Kết nối Supabase OK.");
        } catch (ClassNotFoundException e) {
            log("Không tìm thấy JDBC Driver PostgreSQL.");
        } catch (SQLException e) {
            log("Lỗi SQL Supabase: " + e.getMessage());
        } catch (Exception e) {
            log("Lỗi khác: " + e.getMessage());
        }
    }

    private void onConnect() {
        if (!cardManager.isConnected()) {
            // Kết nối thẻ
            if (cardManager.connect()) {
                CardChannel ch = cardManager.getChannel();
                apdu = new APDUCommands(ch);

                btnConnect.setText("Ngắt kết nối");
                setStatus("Đã kết nối, hãy nhập PIN", Color.ORANGE);
                pinPanel.enablePinInput(true);
                log("Kết nối thẻ thành công.");
            } else {
                log("Kết nối thẻ thất bại.");
                JOptionPane.showMessageDialog(this,
                        "Không kết nối được với thẻ.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Ngắt kết nối thẻ
            cardManager.disconnect();
            apdu = null;
            currentPatientId = null;

            btnConnect.setText("Kết nối thẻ");
            setStatus("Trạng thái: chưa kết nối", Color.RED);
            pinPanel.enablePinInput(false);
            transactionPanel.setTransactionEnabled(false);
            showPinPage();
            log("Đã ngắt kết nối thẻ.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
