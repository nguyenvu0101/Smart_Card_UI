package hospitalcardgui.admin;

import javax.swing.*;
import java.awt.*;

public class AdminFrame extends JFrame {

    private CardIssuePanel cardIssuePanel;
    private CardManagePanel cardManagePanel;
    private CardWritePanel cardWritePanel;

    public AdminFrame() {
        setTitle("Quản Lý Bệnh Nhân");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        cardIssuePanel = new CardIssuePanel();
        cardManagePanel = new CardManagePanel();
        cardWritePanel  = new CardWritePanel();

        tabs.addTab("Lưu thông tin bệnh nhân", cardIssuePanel);
        tabs.addTab("Quản lý thông tin bệnh nhân", cardManagePanel);
        tabs.addTab("Phát Hành Thẻ", cardWritePanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                cardManagePanel.resetAndLoad();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminFrame().setVisible(true));
    }
}
