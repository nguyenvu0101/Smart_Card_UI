package hospitalcardgui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Simple design system for the Admin UI.
 * Inspired by a modern dashboard with light background and teal accent.
 */
public final class AdminTheme {

    // --- COLORS (Medical theme: teal + green, nền sáng) ---
    public static final Color BG_MAIN = new Color(242, 249, 252);        // overall background hơi xanh dương
    public static final Color BG_CARD = new Color(255, 255, 255);        // panels / cards
    public static final Color BG_ACCENT = new Color(0, 184, 163);        // primary buttons / highlights (teal y tế)
    public static final Color BG_ACCENT_SOFT = new Color(220, 248, 241); // subtle accents xanh mint

    public static final Color TEXT_PRIMARY = new Color(18, 38, 52);
    public static final Color TEXT_MUTED = new Color(109, 120, 133);

    public static final Color BORDER_SOFT = new Color(220, 226, 235);
    public static final Color TABLE_HEADER_BG = new Color(241, 245, 249);

    // --- FONTS ---
    public static final Font FONT_TITLE = new Font("Segoe UI Semibold", Font.PLAIN, 18);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI Semibold", Font.PLAIN, 13);

    private AdminTheme() {}

    // --- FACTORY HELPERS ---

    public static JLabel headingLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static void stylePrimaryButton(AbstractButton b) {
        b.setBackground(BG_ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleDangerButton(AbstractButton b) {
        b.setBackground(new Color(234, 84, 85));
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(AbstractButton b) {
        b.setBackground(BG_ACCENT_SOFT);
        b.setForeground(TEXT_PRIMARY);
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void applyCardStyle(JComponent comp) {
        comp.setBackground(BG_CARD);
        comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SOFT),
                new EmptyBorder(16, 16, 16, 16)
        ));
    }

    public static void applyMainBackground(JComponent comp) {
        comp.setBackground(BG_MAIN);
    }

    /**
     * Recursively apply basic fonts / background to children.
     * This keeps existing layout logic but gives a consistent look.
     */
    public static void beautifyTree(Component root) {
        if (root == null) return;

        if (root instanceof JPanel) {
            root.setBackground(BG_MAIN);
        } else if (root instanceof JScrollPane sp) {
            sp.getViewport().setBackground(BG_CARD);
            sp.setBorder(BorderFactory.createLineBorder(BORDER_SOFT));
        } else if (root instanceof JTable table) {
            table.setFont(FONT_INPUT);
            table.setRowHeight(24);
            table.setGridColor(BORDER_SOFT);
            table.getTableHeader().setFont(FONT_BUTTON);
            table.getTableHeader().setBackground(TABLE_HEADER_BG);
            table.getTableHeader().setForeground(TEXT_PRIMARY);
        } else if (root instanceof JLabel l) {
            l.setFont(FONT_LABEL);
            l.setForeground(TEXT_PRIMARY);
        } else if (root instanceof JTextField tf) {
            tf.setFont(FONT_INPUT);
        } else if (root instanceof JTextArea ta) {
            ta.setFont(FONT_INPUT);
        } else if (root instanceof AbstractButton b) {
            // Default secondary button style. Callers can override.
            styleSecondaryButton(b);
        }

        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                beautifyTree(child);
            }
        }
    }
}


