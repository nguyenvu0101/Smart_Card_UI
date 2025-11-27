package hospitalcardgui.admin;

import javax.swing.*;
import java.awt.*;

public class AdminFrame extends JFrame {

    private CardIssuePanel cardIssuePanel;
    private CardManagePanel cardManagePanel;
    private CardWritePanel cardWritePanel;

    public AdminFrame() {
        setTitle("Admin - Quản lý thẻ bệnh nhân");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        cardIssuePanel = new CardIssuePanel();
        cardManagePanel = new CardManagePanel();
        cardWritePanel  = new CardWritePanel();

        tabs.addTab("Cấp thẻ mới", cardIssuePanel);
        tabs.addTab("Quản lý thẻ", cardManagePanel);
        tabs.addTab("Ghi thẻ (patient_id)", cardWritePanel);

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
