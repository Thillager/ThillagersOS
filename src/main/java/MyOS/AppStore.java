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

// ================= APP STORE =================
public class AppStore extends JInternalFrame {

    public AppStore() {
        super("App Store", true, true, true, true);
        setSize(400, 500);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        // Hier fügst du deine Apps hinzu
        addAppEntry(listPanel, "SuperCalcv1.0", "https://github.com/Thillager/ThillagersOSResources/releases/download/SuperCalcv1.0/SuperCalcv1.0.illag");
        addAppEntry(listPanel, "TuiCalcv1.0", "https://github.com/Thillager/ThillagersOS/releases/download/TuiCalc/TuiCalc.illag");
        addAppEntry(listPanel, "Antivirus",            "https://github.com/Thillager/ThillagersOSResources/releases/download/Antivirusv1.0/Antivirusv1.0.illag"
                    );

        add(new JScrollPane(listPanel), BorderLayout.CENTER);

        JButton custom = new JButton("URL installieren");
        custom.addActionListener(e -> {
            String url = JOptionPane.showInputDialog("Link:");
            if (url != null && !url.isEmpty()) {
                // Extrahiert den Namen nach dem letzten Slash
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                installApp(fileName, url);
            }
        });
        add(custom, BorderLayout.SOUTH);
    }

    private void addAppEntry(JPanel p, String name, String url) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(new EmptyBorder(5, 5, 5, 5)); // Ein wenig Abstand
        row.add(new JLabel(name), BorderLayout.CENTER);

        JButton btn = new JButton("Install");
        btn.addActionListener(e -> {
            // Extrahiert die Endung automatisch aus der URL
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            installApp(fileName, url);
        });

        row.add(btn, BorderLayout.EAST);
        p.add(row);
    }

    private void installApp(String fileName, String urlStr) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                // Nutzt das VM_DIR aus deiner Main-Klasse
                File targetFile = new File(Main.VM_DIR, fileName);

                // Verzeichnis erstellen, falls es nicht existiert
                if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }

                // Download und Speichern
                Files.copy(url.openStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, fileName + " wurde erfolgreich installiert!"));
            } catch (Exception ex) { 
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Fehler beim Download von: " + fileName)); 
            }
        }).start();
    }
}