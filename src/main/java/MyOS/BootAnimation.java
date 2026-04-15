package MyOS;

import javax.swing.*;
import java.awt.*;

public class BootAnimation extends JWindow {

    private float alpha = 0f;
    private int angle = 0;
    private int progress = 0;

    public BootAnimation() {
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocationRelativeTo(null);

        setContentPane(new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Hintergrund
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, alpha));

                // TEXT
                g2.setColor(Color.RED);
                g2.setFont(new Font("SansSerif", Font.BOLD, 42));
                String text = "Thillagers OS v3.0.0";

                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = getHeight() / 2 - 50;
                g2.drawString(text, tx, ty);

                // ROTIERENDER KREIS
                int cx = getWidth() / 2;
                int cy = getHeight() / 2 + 20;

                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians((360 / 12) * i + angle);
                    int x = (int) (cx + Math.cos(a) * 30);
                    int y = (int) (cy + Math.sin(a) * 30);

                    float brightness = (float) i / 12;
                    g2.setColor(new Color(255, 0, 0,
                            (int)(brightness * 255)));

                    g2.fillOval(x - 4, y - 4, 8, 8);
                }

                // PROGRESS BAR
                int barWidth = 300;
                int barHeight = 10;
                int bx = (getWidth() - barWidth) / 2;
                int by = cy + 60;

                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(bx, by, barWidth, barHeight);

                g2.setColor(Color.RED);
                g2.fillRect(bx, by,
                        (int)(barWidth * (progress / 100.0)), barHeight);
            }
        });
    }

    public void setProgress(int p) {
        this.progress = p;
        repaint();
    }

    public void startAnimation() {
        setVisible(true);

        new Timer(16, e -> {
            angle += 5;
            repaint();
        }).start();
    }

    public void finish(Runnable onFinish) {
        new Thread(() -> {
            try {
                for (alpha = 0; alpha <= 1; alpha += 0.05f) {
                    repaint();
                    Thread.sleep(20);
                }
                Thread.sleep(500);
                for (alpha = 1; alpha >= 0; alpha -= 0.05f) {
                    repaint();
                    Thread.sleep(20);
                }
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                dispose();
                onFinish.run();
            });
        }).start();
    }
}