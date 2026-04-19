package MyOS;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class BootAnimation extends JFrame {

    private float alpha = 1.0f;
    private int angle = 0;
    private int progress = 0;
    private javax.swing.Timer rotationTimer;

    public BootAnimation() {
        setUndecorated(false); // WICHTIG: Kein Rahmen
        // Exakt gleiche Größe wie Main
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.BLACK);

        JPanel drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Transparenz für den Fade-Out am Ende
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                // Titel
                g2.setColor(Color.RED);
                g2.setFont(new Font("SansSerif", Font.BOLD, 50));
                String text = "Thillagers OS v3.1.0";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, getHeight() / 2 - 60);

                // Ladekreis
                int cx = getWidth() / 2;
                int cy = getHeight() / 2 + 30;
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians((360.0 / 12) * i + angle);
                    int x = (int) (cx + Math.cos(a) * 45);
                    int y = (int) (cy + Math.sin(a) * 45);
                    float brightness = (float) i / 12f;
                    g2.setColor(new Color(255, 0, 0, (int) (brightness * 255)));
                    g2.fillOval(x - 6, y - 6, 12, 12);
                }

                // Progress Bar
                int bw = 400, bh = 4;
                int bx = (getWidth() - bw) / 2;
                int by = cy + 120;
                g2.setColor(new Color(40, 40, 40));
                g2.fillRoundRect(bx, by, bw, bh, 2, 2);
                g2.setColor(Color.RED);
                g2.fillRoundRect(bx, by, (int)(bw * (progress / 100.0)), bh, 2, 2);
            }
        };

        setContentPane(drawPanel);
    }

    public void setProgress(int p) {
        this.progress = p;
        repaint();
    }

    public void startAnimation() {
        setVisible(true);
        rotationTimer = new javax.swing.Timer(16, e -> {
            angle += 8;
            repaint();
        });
        rotationTimer.start();
    }

    public void finish(Runnable onFinish) {
    new Thread(() -> {
        // Schneller Fade-Out (ca. 200ms)
        for (float i = 1.0f; i >= 0; i -= 0.1f) {
            alpha = Math.max(0, i);
            SwingUtilities.invokeLater(this::repaint);
            try { Thread.sleep(20); } catch (Exception ignored) {}
        }

        // Sobald unsichtbar -> Sofort Main zeigen
        SwingUtilities.invokeLater(() -> {
            dispose();
            if (onFinish != null) onFinish.run();
        });
    }).start();
}
}