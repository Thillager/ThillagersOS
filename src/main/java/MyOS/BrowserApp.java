package MyOS;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
    
// ================= BROWSER =================
public class BrowserApp extends JInternalFrame {
private JTextField urlField;
private JButton goBtn, dlBtn;
private JProgressBar progress;
private JEditorPane displayPane;

public BrowserApp() {
    super("Web Browser", true, true, true, true);
    setSize(800, 600);
    setLayout(new BorderLayout());

    // --- Top Panel: URL + Buttons ---
    urlField = new JTextField("https://", 40);
    goBtn = new JButton("[GO]");
    dlBtn = new JButton("[DL]");
    progress = new JProgressBar(0, 100);

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    topPanel.add(urlField);
    topPanel.add(goBtn);
    topPanel.add(dlBtn);
    topPanel.setBackground(new Color(60,60,60));
    add(topPanel, BorderLayout.NORTH);
    add(progress, BorderLayout.SOUTH);

    // --- Display Panel ---
    displayPane = new JEditorPane();
    displayPane.setEditable(false);
    displayPane.setContentType("text/html"); // HTML anzeigen
    JScrollPane scrollPane = new JScrollPane(displayPane);
    add(scrollPane, BorderLayout.CENTER);

    // --- Styling ---
    urlField.setBackground(Color.WHITE);
    goBtn.setBackground(new Color(80,80,80));
    goBtn.setForeground(Color.WHITE);
    dlBtn.setBackground(new Color(80,80,80));
    dlBtn.setForeground(Color.WHITE);

    // --- GO Button: Webseite anzeigen ---
    goBtn.addActionListener(e -> loadURL(urlField.getText()));

    // --- Download Button ---
    dlBtn.addActionListener(e -> {
new Thread(() -> {
    try {
        URL url = new URL(urlField.getText());
        String tempName = urlField.getText().substring(urlField.getText().lastIndexOf('/') + 1);
        final String fileName = tempName.isEmpty() ? "dl_" + System.currentTimeMillis() : tempName;

        File targetFile = new File(Main.VM_DIR, fileName);

        // Download mit Stream
        try (InputStream in = url.openStream(); 
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        SwingUtilities.invokeLater(() -> {
            progress.setValue(100);
            JOptionPane.showMessageDialog(BrowserApp.this, "Download fertig: " + fileName);
        });

    } catch(Exception ex) {
        SwingUtilities.invokeLater(() -> {
            progress.setValue(0);
            JOptionPane.showMessageDialog(BrowserApp.this, "Fehler beim Download:\n" + ex.getMessage());
        });
    }
}).start();
});
}


private void loadURL(String urlStr) {
    new Thread(() -> {
        try {
            displayPane.setPage(urlStr); // im internen Fenster laden
        } catch(Exception e) {
            SwingUtilities.invokeLater(() -> displayPane.setText("<html><body><h2>Fehler beim Laden</h2></body></html>"));
        }
    }).start();
}
}

