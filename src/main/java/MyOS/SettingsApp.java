package MyOS;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SettingsApp extends JInternalFrame {

    private JPanel contentPane;
    private JButton wallpaperButton;
    private JComboBox<String> themeCombo;
    private JButton bgColorButton, accentColorButton, taskbarColorButton, textColorButton;
    private JTextField timeOffsetField;
    private JLabel wallpaperPreview;
    private JButton resetBtn;

    public SettingsApp() {
        super("Einstellungen", true, true, true, true);
        setSize(500, 450);
        setLayout(new BorderLayout());

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new GridLayout(0, 2, 10, 10));
        add(contentPane, BorderLayout.CENTER);

        // ================= THEME =================
        contentPane.add(new JLabel("Theme:"));
        themeCombo = new JComboBox<>(new String[] { "Win10", "Win95", "macOS", "Linux" });
        themeCombo.setSelectedItem(Main.currentTheme);
        contentPane.add(themeCombo);

        // ================= HINTERGRUNDFARBE =================
        contentPane.add(new JLabel("Hintergrundfarbe:"));
        bgColorButton = new JButton("Ändern");
        bgColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Hintergrundfarbe wählen", Main.currentBg);
            if (newColor != null) {
                Main.currentBg = newColor;
                Main.desktop.repaint();
            }
        });
        contentPane.add(bgColorButton);

        // ================= TASKBARFARBE =================
        contentPane.add(new JLabel("Taskbar-Farbe:"));
        taskbarColorButton = new JButton("Ändern");
        taskbarColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Taskbar-Farbe wählen", Main.taskbarColor);
            if (newColor != null) {
                Main.taskbarColor = newColor;

                // 🔥 HIER HIN
                Main.customTaskbarColor = true;

                if (Main.taskbar != null) {
                    Main.taskbar.setBackground(newColor);
                    Main.taskbar.repaint();
                }
            }
        });
        contentPane.add(taskbarColorButton);

        // ================= AKZENTFARBE =================
        contentPane.add(new JLabel("Akzentfarbe:"));
        accentColorButton = new JButton("Ändern");
        accentColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Akzentfarbe wählen", Main.accentColor);
            if (newColor != null) {
                Main.accentColor = newColor;
                Main.desktop.repaint();
                if (Main.taskbar != null) Main.taskbar.repaint();
            }
        });
        

        // ================= TEXTFARBE =================
        contentPane.add(new JLabel("Textfarbe:"));
        textColorButton = new JButton("Ändern");
        textColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Textfarbe wählen", Main.textColor);
            if (newColor != null) {
                Main.textColor = newColor;

                
                Main.customTextColor = true;

                if (Main.desktop != null) ThemeManager.applyThemeToComponent(Main.desktop);
                if (Main.taskbar != null) ThemeManager.applyThemeToComponent(Main.taskbar);
            }
        });
        contentPane.add(textColorButton);

        // ================= ZEIT-OFFSET =================
        contentPane.add(new JLabel("Zeit-Offset in ms:"));
        timeOffsetField = new JTextField(String.valueOf(Main.timeOffsetMillis));
        contentPane.add(timeOffsetField);

        // ================= WALLPAPER =================
        contentPane.add(new JLabel("Wallpaper:"));
        JPanel wallpaperPanel = new JPanel(new BorderLayout());
        wallpaperButton = new JButton("Ändern");
        wallpaperPreview = new JLabel();
        wallpaperPreview.setPreferredSize(new Dimension(100, 100));
        wallpaperPanel.add(wallpaperButton, BorderLayout.WEST);
        wallpaperPanel.add(wallpaperPreview, BorderLayout.EAST);
        contentPane.add(wallpaperPanel);

        wallpaperButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (f.exists()) {
                    Main.setWallpaper(f.getAbsolutePath());
                    ImageIcon preview = new ImageIcon(new ImageIcon(f.getAbsolutePath()).getImage()
                            .getScaledInstance(100, 100, Image.SCALE_SMOOTH));
                    wallpaperPreview.setIcon(preview);
                }
            }
        });

        // ================= BOTTOM BUTTONS =================
        JPanel bottomPanel = new JPanel();
        JButton saveBtn = new JButton("Speichern");
        JButton cancelBtn = new JButton("Abbrechen");
        JButton resetBtn = new JButton("Zurücksetzen");

        saveBtn.addActionListener(e -> saveSettings());
        cancelBtn.addActionListener(e -> dispose());
        resetBtn.addActionListener(e -> reset());

        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);
        bottomPanel.add(resetBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void saveSettings() {
        // ===== THEME =====
        Main.currentTheme = (String) themeCombo.getSelectedItem();

        // ===== ZEIT-OFFSET =====
        try {
            Main.timeOffsetMillis = Long.parseLong(timeOffsetField.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ungültiger Zeit-Offset!");
        }

        // ===== FARBEN SPEICHERN =====
        if (Main.systemProps != null) {
            Main.systemProps.setProperty("bgColor", colorToHex(Main.currentBg));
            Main.systemProps.setProperty("taskbarColor", colorToHex(Main.taskbarColor));
            Main.systemProps.setProperty("accentColor", colorToHex(Main.accentColor));
            Main.systemProps.setProperty("textColor", colorToHex(Main.textColor));
        }

        // ===== THEME ANWENDEN =====
        // Hier setzen wir Taskbar-Farbe vorher, damit ThemeManager sie nicht überschreibt
        if (Main.taskbar != null) Main.taskbar.setBackground(Main.taskbarColor);

        ThemeManager.applyTheme(Main.currentTheme);

        // ===== SPEICHERN =====
        Main.saveSettings();

        JOptionPane.showMessageDialog(this, "Einstellungen gespeichert!");
        ThemeManager.applyTheme(Main.currentTheme);
        Main.desktop.repaint();
        Main.taskbar.repaint();
        dispose();
    }

    // ===== Farb-Konvertierungen =====
    public static String colorToHex(Color c) {
        if (c == null) return "#ffffff";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color hexToColor(String s) {
        try {
            return Color.decode(s);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private void reset() {
        Main.customTaskbarColor = false;
        Main.customTextColor = false;
    }
    
}