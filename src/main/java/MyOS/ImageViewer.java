package MyOS;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class ImageViewer extends JInternalFrame {
    private BufferedImage currentImage;
    private JLabel imageLabel;
    private File currentFile;

    private Color currentColor = Color.RED;
    private int brushSize = 5;
    private Point lastPoint = null;

    // Modi für die Werkzeuge
    private enum Mode { PEN, FILL, PICKER }
    private Mode currentMode = Mode.PEN;

    private JButton colorBtn; // Global, damit wir die Hintergrundfarbe updaten können

    public ImageViewer(File f) {
        super("Image Editor - " + f.getName(), true, true, true, true);
        this.currentFile = f;

        try {
            BufferedImage loadedImage = ImageIO.read(f);
            currentImage = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = currentImage.getGraphics();
            g.drawImage(loadedImage, 0, 0, null);
            g.dispose();

            setupUI();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Fehler: " + e.getMessage()); 
            e.printStackTrace();
        }

        setSize(800, 600);
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();

        // --- Buttons & UI ---
        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(e -> saveImage());

        colorBtn = new JButton("Farbe");
        colorBtn.setBackground(currentColor);
        colorBtn.addActionListener(e -> {
            Color selected = JColorChooser.showDialog(this, "Wähle Farbe", currentColor);
            if (selected != null) updateCurrentColor(selected);
        });

        JToggleButton penBtn = new JToggleButton("Stift", true);
        JToggleButton fillBtn = new JToggleButton("Füllen");
        JToggleButton pickBtn = new JToggleButton("Pipette"); // Der neue Button

        // Exklusive Auswahl der Buttons (ButtonGroup wäre auch möglich)
        ActionListener modeListener = e -> {
            if (e.getSource() == penBtn) currentMode = Mode.PEN;
            if (e.getSource() == fillBtn) currentMode = Mode.FILL;
            if (e.getSource() == pickBtn) currentMode = Mode.PICKER;

            penBtn.setSelected(currentMode == Mode.PEN);
            fillBtn.setSelected(currentMode == Mode.FILL);
            pickBtn.setSelected(currentMode == Mode.PICKER);
        };

        penBtn.addActionListener(modeListener);
        fillBtn.addActionListener(modeListener);
        pickBtn.addActionListener(modeListener);

        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        sizeSpinner.addChangeListener(e -> brushSize = (int) sizeSpinner.getValue());

        toolBar.add(saveBtn);
        toolBar.addSeparator();
        toolBar.add(penBtn);
        toolBar.add(fillBtn);
        toolBar.add(pickBtn);
        toolBar.addSeparator();
        toolBar.add(colorBtn);
        toolBar.add(new JLabel(" Dicke: "));
        toolBar.add(sizeSpinner);

        imageLabel = new JLabel(new ImageIcon(currentImage));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentMode == Mode.PICKER) {
                    pickColor(e.getX(), e.getY());
                } else if (currentMode == Mode.FILL) {
                    applyFloodFill(e.getX(), e.getY());
                } else {
                    lastPoint = e.getPoint();
                    paintAt(e.getPoint());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentMode == Mode.PEN) paintAt(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        };

        imageLabel.addMouseListener(ma);
        imageLabel.addMouseMotionListener(ma);

        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);
    }

    // --- Hilfsmethode: Farbe setzen & UI updaten ---
    private void updateCurrentColor(Color c) {
        this.currentColor = c;
        this.colorBtn.setBackground(c);
        // Kontrast-Check für Button-Text (optional)
        double brightness = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255;
        colorBtn.setForeground(brightness > 0.5 ? Color.BLACK : Color.WHITE);
    }

    // --- Der Color Picker (Getter) ---
    private void pickColor(int x, int y) {
        if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) return;

        int rgb = currentImage.getRGB(x, y);
        updateCurrentColor(new Color(rgb));
    }

    private void applyFloodFill(int x, int y) {
        if (x < 0 || x >= currentImage.getWidth() || y < 0 || y >= currentImage.getHeight()) return;
        int targetRGB = currentImage.getRGB(x, y);
        int replacementRGB = currentColor.getRGB();
        if (targetRGB == replacementRGB) return;

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x, y));

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (p.x < 0 || p.x >= currentImage.getWidth() || p.y < 0 || p.y >= currentImage.getHeight()) continue;
            if (currentImage.getRGB(p.x, p.y) == targetRGB) {
                currentImage.setRGB(p.x, p.y, replacementRGB);
                queue.add(new Point(p.x + 1, p.y));
                queue.add(new Point(p.x - 1, p.y));
                queue.add(new Point(p.x, p.y + 1));
                queue.add(new Point(p.x, p.y - 1));
            }
        }
        imageLabel.repaint();
    }

    private void paintAt(Point p) {
        Graphics2D g2 = (Graphics2D) currentImage.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(currentColor);
        g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (lastPoint != null) {
            g2.drawLine(lastPoint.x, lastPoint.y, p.x, p.y);
        } else {
            g2.fillOval(p.x - (brushSize/2), p.y - (brushSize/2), brushSize, brushSize);
        }

        lastPoint = p;
        g2.dispose();
        imageLabel.repaint();
    }

    private void saveImage() {
        try {
            ImageIO.write(currentImage, "png", currentFile);
            JOptionPane.showMessageDialog(this, "Gespeichert!");
        } catch (IOException e) { e.printStackTrace(); }
    }
}