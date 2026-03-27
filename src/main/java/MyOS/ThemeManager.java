package MyOS;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JInternalFrame;

public class ThemeManager {

    public static void applyTheme(String theme) {

        Main.currentTheme = theme;

        Color bgColor, taskbarBg, textCol, iconBorder;
        Font defaultFont;

        // ===== THEMES =====
        switch (theme) {
            case "Win95":
                bgColor = new Color(192, 192, 192);
                taskbarBg = new Color(192, 192, 192);
                textCol = Color.BLACK;
                iconBorder = Color.BLACK;
                defaultFont = new Font("MS Sans Serif", Font.PLAIN, 12);
                break;

            case "macOS":
                bgColor = new Color(220, 220, 220);
                taskbarBg = new Color(255, 255, 255, 220);
                textCol = Color.BLACK;
                iconBorder = Color.GRAY;
                defaultFont = new Font("San Francisco", Font.PLAIN, 13);
                break;

            case "Linux":
                bgColor = new Color(48, 10, 36);
                taskbarBg = new Color(30, 30, 30);
                textCol = Color.WHITE;
                iconBorder = Color.WHITE;
                defaultFont = new Font("Ubuntu", Font.PLAIN, 13);
                break;

            case "Win10":
            default:
                bgColor = new Color(30, 30, 30);
                taskbarBg = new Color(20, 20, 20);
                textCol = Color.GRAY;
                iconBorder = Color.WHITE;
                defaultFont = new Font("Segoe UI", Font.PLAIN, 13);
                break;
        }

        // ===== GLOBAL SETZEN =====
        Main.currentBg = bgColor;
        if (!Main.customTaskbarColor) {
            Main.taskbarColor = taskbarBg;
        }
        if (!Main.customTextColor) {
            Main.textColor = textCol;
        }

        if (Main.instance == null) return;

        // ===== DESKTOP FENSTER =====
        if (Main.desktop != null) {
            for (JInternalFrame frame : Main.desktop.getAllFrames()) {
                applyThemeToComponent(frame);
            }
        }

        // ===== TASKBAR =====
        if (Main.taskbar != null) {

            if (!Main.customTaskbarColor) {
                Main.taskbarColor = taskbarBg;
            }

            // 🔥 Taskbar selbst setzen
            Main.taskbar.setBackground(Main.taskbarColor);

            for (Component c : Main.taskbar.getComponents()) {
                c.setForeground(textCol);
                c.setFont(defaultFont);

                if (c instanceof JButton) {
                    JButton btn = (JButton) c;

                    // 🔥 WICHTIG: NICHT taskbarBg!
                    btn.setBackground(Main.taskbarColor);
                    btn.setForeground(textCol);
                }
            }
        }

        // ===== START MENU =====
        if (Main.instance.getCustomStartMenu() != null) {
            applyThemeToComponent(Main.instance.getCustomStartMenu());
        }

        // ===== DESKTOP ICONS =====
        if (Main.desktop != null) {
            for (Component c : Main.desktop.getComponents()) {
                applyThemeToComponent(c);
            }
        }

        // ===== TASKBAR POSITION =====
        Dimension sz = Main.instance.getContentPane().getSize();
        int tbHeight, tbY;

        switch (theme) {
            case "macOS":
                tbHeight = 35;
                tbY = 0;
                break;

            case "Win95":
                tbHeight = 40;
                tbY = sz.height - tbHeight;
                break;

            default:
                tbHeight = 55;
                tbY = sz.height - tbHeight;
                break;
        }

        Main.taskbar.setBounds(0, tbY, sz.width, tbHeight);

        // ===== REFRESH =====
        Main.instance.taskbar.repaint();
        Main.instance.revalidate();
        Main.instance.repaint();
    }

    // ===== REKURSIVE THEME METHODE =====
    public static void applyThemeToComponent(Component c) {
        if (c == null) return;

        c.setBackground(Main.currentBg);
        c.setForeground(Main.textColor);
        c.setFont(getDefaultFont());

        if (c instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) c).getComponents()) {
                applyThemeToComponent(child);
            }
        }
    }

    public static Font getDefaultFont() {
        switch (Main.currentTheme) {
            case "Win95":
                return new Font("MS Sans Serif", Font.PLAIN, 12);
            case "macOS":
                return new Font("San Francisco", Font.PLAIN, 13);
            case "Linux":
                return new Font("Ubuntu", Font.PLAIN, 13);
            case "Win10":
            default:
                return new Font("Segoe UI", Font.PLAIN, 13);
        }
    }
}
