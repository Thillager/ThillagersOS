package MyOS;

import java.awt.Dimension;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

public class WindowManager {

    private JDesktopPane desktop;
    private JPanel taskIconsPanel;

    private int windowOffset = 0;

    // Verbindung Fenster → Taskbar Button
    private Map<JInternalFrame, JButton> windowToButtonMap = new HashMap<>();

    public WindowManager(JDesktopPane desktop, JPanel taskIconsPanel) {
        this.desktop = desktop;
        this.taskIconsPanel = taskIconsPanel;
    }

    public void openApp(JInternalFrame app) {

        // ===== Position =====
        app.setLocation(100 + windowOffset, 80 + windowOffset);
        windowOffset = (windowOffset + 30) % 300;

        // ===== Zum Desktop hinzufügen =====
        desktop.add(app);
        app.setVisible(true);

        // Theme anwenden
        ThemeManager.applyThemeToComponent(app);

        try {
            app.setSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== TASKBAR BUTTON =====
        JButton tBtn = new JButton();

        ImageIcon icon = getIconFor(app.getTitle());

        if (icon != null) {
            tBtn.setIcon(icon);
        } else {
            tBtn.setText(app.getTitle());
        }

        tBtn.setPreferredSize(new Dimension(48, 48));
        tBtn.setBorderPainted(false);
        tBtn.setContentAreaFilled(false);
        tBtn.setFocusPainted(false);
        tBtn.setToolTipText(app.getTitle());

        // Klick-Verhalten (wie Windows)
        tBtn.addActionListener(e -> {
            try {
                if (app.isIcon()) {
                    // Minimiert → wiederherstellen
                    app.setIcon(false);
                    app.setVisible(true);
                    app.toFront();
                    app.setSelected(true);

                } else if (app.isVisible() && app.isSelected()) {
                    // Aktiv → verstecken
                    app.setVisible(false);

                } else {
                    // Im Hintergrund → nach vorne holen
                    app.setVisible(true);
                    app.toFront();
                    app.setSelected(true);
                }
            } catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }
        });

        // Button speichern
        windowToButtonMap.put(app, tBtn);

        // Zur Taskbar hinzufügen
        taskIconsPanel.add(tBtn);
        taskIconsPanel.revalidate();
        taskIconsPanel.repaint();

        // ===== Fenster schließen → Button entfernen =====
        app.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                JButton btn = windowToButtonMap.remove(app);
                if (btn != null) {
                    taskIconsPanel.remove(btn);
                    taskIconsPanel.revalidate();
                    taskIconsPanel.repaint();
                }
            }
        });
    }

    // ===== ICON LOGIK (aus Main übernommen) =====
    private ImageIcon getIconFor(String title) {
        return Main.getIconFor(title); // nutzt deine bestehende Methode
    }
}