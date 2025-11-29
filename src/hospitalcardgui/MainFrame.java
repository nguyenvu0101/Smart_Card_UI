package hospitalcardgui;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CommandAPDU;
import javax. smartcardio.ResponseAPDU;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private final CardManager cardManager = CardManager. getInstance();
    private APDUCommands apdu;

    private JButton btnConnect;
    private JLabel lblStatus;

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private PinPanel pinPanel;
    private TransactionPanel transactionPanel;

    private String currentPatientId;

    // ✅ AID CỦA APPLET (ĐÃ SỬA ĐÚNG: 0011223344556711)
    private static final byte[] APPLET_AID = {
        (byte)0x00, (byte)0x11, (byte)0x22, (byte)0x33, 
        (byte)0x44, (byte)0x55, (byte)0x67, (byte)0x11
    };

    public MainFrame() {
        initUI();
//        testSupabase();
    }

    public APDUCommands getApdu() { return apdu; }

    public void log(String msg) {
        if (transactionPanel != null) {
            transactionPanel.appendLog(msg);
        }
        System.out.println("[LOG] " + msg);
    }

    public void showPinPage() {
        cardLayout.show(cardPanel, "PIN");
    }

    public void showTransactionPage() {
        transactionPanel.setTransactionEnabled(true);
        cardLayout.show(cardPanel, "TX");
    }

    public void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }

    public String getCurrentPatientId() { return currentPatientId; }
    public void setCurrentPatientId(String id) { this.currentPatientId = id; }

    public void loadPatientInfoOnTransactionPanel() {
        if (transactionPanel != null && currentPatientId != null) {
            transactionPanel.loadPatientInfoFromDb(currentPatientId);
        }
    }

    public TransactionPanel getTransactionPanel() { return transactionPanel; }

    private void initUI() {
        setTitle("Hệ thống thẻ bệnh nhân (Kiosk)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(root);

        // ===== KẾT NỐI THẺ =====
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder("Kết nối thẻ"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints. HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        btnConnect = new JButton("Kết nối thẻ");
        btnConnect.addActionListener(e -> onConnect());
        top.add(btnConnect, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 2;
        lblStatus = new JLabel("Trạng thái: chưa kết nối");
        lblStatus.setForeground(Color.RED);
        top.add(lblStatus, c);

        root.add(top, BorderLayout. NORTH);

        // ===== CardLayout =====
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        pinPanel = new PinPanel(this);
        transactionPanel = new TransactionPanel(this);

        cardPanel.add(pinPanel, "PIN");
        cardPanel. add(transactionPanel, "TX");

        root.add(cardPanel, BorderLayout.CENTER);

        showPinPage();
        pinPanel.enablePinInput(false);
    }

//    private void testSupabase() {
//        try (Connection c = DatabaseConnection.getConnection()) {
//            log("Kết nối Supabase OK.");
//        } catch (ClassNotFoundException e) {
//            log("Không tìm thấy JDBC Driver PostgreSQL.");
//        } catch (SQLException e) {
//            log("Lỗi SQL Supabase: " + e. getMessage());
//        } catch (Exception e) {
//            log("Lỗi khác: " + e.getMessage());
//        }
//    }

    private void onConnect() {
        if (!cardManager.isConnected()) {
            // ✅ KẾT NỐI THẺ
            log("========================================");
            log("Đang kết nối thẻ...");
            if (! cardManager.connect()) {
                log("✗ Kết nối thẻ thất bại.");
                JOptionPane.showMessageDialog(this, 
                    "Không kết nối được với thẻ.\nVui lòng kiểm tra đầu đọc và thẻ.", 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            log("✓ Kết nối vật lý thành công.");

            CardChannel ch = cardManager.getChannel();
            if (ch == null) {
                log("✗ Không lấy được CardChannel.");
                cardManager.disconnect();
                return;
            }

            // ✅ SELECT APPLET NGAY SAU KHI KẾT NỐI
            try {
                log("Đang SELECT applet...");
                log("  AID: " + bytesToHex(APPLET_AID));
                
                CommandAPDU selectCmd = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APPLET_AID);
                
                // In ra APDU bytes để debug
                byte[] cmdBytes = selectCmd.getBytes();
                log("  APDU: " + bytesToHex(cmdBytes));
                
                ResponseAPDU selectResp = ch.transmit(selectCmd);
                
                int sw = selectResp. getSW();
                log("  Response SW: " + String.format("0x%04X", sw));

                if (sw != 0x9000) {
                    log("✗ SELECT APPLET THẤT BẠI!");
                    log("  Có thể applet chưa được cài đặt trên thẻ.");
                    log("  Hoặc AID không đúng.");
                    JOptionPane.showMessageDialog(this, 
                        "Không tìm thấy applet trên thẻ.\n" +
                        "AID: " + bytesToHex(APPLET_AID) + "\n" +
                        "SW: " + String.format("0x%04X", sw) + "\n\n" +
                        "Vui lòng kiểm tra:\n" +
                        "1. Applet đã được cài đặt chưa?\n" +
                        "2. AID có đúng không? ", 
                        "Lỗi", JOptionPane. ERROR_MESSAGE);
                    cardManager.disconnect();
                    return;
                }

                log("✓ SELECT APPLET THÀNH CÔNG!");

                // ✅ TẠO APDUCommands SAU KHI SELECT THÀNH CÔNG
                apdu = new APDUCommands(ch);

                // ✅ TEST PING THẺ - Thử lấy Salt để confirm applet hoạt động
                try {
                    log("Đang test ping thẻ (getSalt)...");
                    byte[] testSalt = apdu.getSalt();
                    if (testSalt != null && testSalt. length > 0) {
                        log("✓ Thẻ phản hồi OK.  Salt length=" + testSalt.length);
                        log("  Salt: " + bytesToHex(testSalt));
                        
                        // Kiểm tra xem salt có phải toàn 0 không (chưa được ghi)
                        boolean allZero = true;
                        for (byte b : testSalt) {
                            if (b != 0) {
                                allZero = false;
                                break;
                            }
                        }
                        
                        if (allZero) {
                            log("⚠ CẢNH BÁO: Salt toàn 0 - thẻ có thể chưa được ghi dữ liệu.");
                            log("  Vui lòng chạy CardWritePanel để ghi dữ liệu lên thẻ trước.");
                            JOptionPane.showMessageDialog(this,
                                "Thẻ chưa được khởi tạo dữ liệu.\n" +
                                "Vui lòng chạy chức năng 'Ghi thẻ' (CardWritePanel) trước.",
                                "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                        } else {
                            log("✓ Thẻ đã có dữ liệu.");
                        }
                    } else {
                        log("⚠ Cảnh báo: getSalt() trả về null");
                        log("  Thẻ có thể chưa được khởi tạo dữ liệu.");
                    }
                } catch (Exception testEx) {
                    log("⚠ Test ping thất bại: " + testEx. getMessage());
                    testEx.printStackTrace();
                }

                // ✅ CẬP NHẬT UI
                btnConnect.setText("Ngắt kết nối");
                setStatus("Đã kết nối - Mời nhập PIN", Color. ORANGE);
                pinPanel. enablePinInput(true);
                log("========== SẴN SÀNG NHẬP PIN ==========");

            } catch (Exception ex) {
                ex.printStackTrace();
                log("✗ Lỗi khi SELECT applet: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Lỗi giao tiếp với thẻ:\n" + ex.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                cardManager.disconnect();
                apdu = null;
            }

        } else {
            // ✅ NGẮT KẾT NỐI
            log("========================================");
            log("Đang ngắt kết nối thẻ...");
            cardManager.disconnect();
            apdu = null;
            currentPatientId = null;

            btnConnect.setText("Kết nối thẻ");
            setStatus("Trạng thái: chưa kết nối", Color.RED);
            pinPanel. enablePinInput(false);
            transactionPanel.setTransactionEnabled(false);
            showPinPage();
            log("✓ Đã ngắt kết nối thẻ.");
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb. append(String.format("%02X", b));
        return sb. toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}