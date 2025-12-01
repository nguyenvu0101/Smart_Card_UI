package hospitalcardgui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminFrame extends JFrame {

    private CardIssuePanel cardIssuePanel;
    private CardManagePanel cardManagePanel;
    private CardWritePanel cardWritePanel;
    private ResetPinPanel resetPinPanel;

    public AdminFrame() {
        setTitle("Quản Lý Bệnh Nhân");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // Main background similar to dashboard style
        getContentPane().setBackground(AdminTheme.BG_MAIN);

        // Root container với header màu sắc chủ đề y tế
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(AdminTheme.BG_MAIN);

        // Header bar xanh y tế
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AdminTheme.BG_ACCENT);
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("HỆ THỐNG QUẢN LÝ THẺ Y TẾ");
        title.setFont(AdminTheme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Bệnh nhân • Thẻ thông minh • Bảo mật");
        subtitle.setFont(AdminTheme.FONT_LABEL);
        subtitle.setForeground(new Color(224, 247, 250));

        JPanel textWrap = new JPanel(new BorderLayout(0, 2));
        textWrap.setOpaque(false);
        textWrap.add(title, BorderLayout.NORTH);
        textWrap.add(subtitle, BorderLayout.SOUTH);

        header.add(textWrap, BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(AdminTheme.FONT_BUTTON);
        tabs.setBackground(AdminTheme.BG_CARD);
        tabs.setOpaque(true);

        cardIssuePanel = new CardIssuePanel();
        cardManagePanel = new CardManagePanel();
        cardWritePanel  = new CardWritePanel();
        resetPinPanel = new ResetPinPanel();

        tabs.addTab("Lưu thông tin bệnh nhân", cardIssuePanel);
        tabs.addTab("Quản lý thông tin bệnh nhân", cardManagePanel);
        tabs.addTab("Phát Hành Thẻ", cardWritePanel);
        tabs.addTab("Reset PIN", resetPinPanel);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                cardManagePanel.resetAndLoad();
            }
        });

        // Apply theme to all children
        AdminTheme.beautifyTree(cardIssuePanel);
        AdminTheme.beautifyTree(cardManagePanel);
        AdminTheme.beautifyTree(cardWritePanel);
        AdminTheme.beautifyTree(resetPinPanel);

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }
}
