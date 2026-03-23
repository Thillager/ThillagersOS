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

// ================= TEXT EDITOR (ENHANCED) =================
public class TextEditor extends JInternalFrame {
    private JTextArea area;
    private File currentFile;

    public TextEditor() { this(null); }
    public TextEditor(File f) {
        super("Text Editor", true, true, true, true);
        this.currentFile = f;
        setSize(600, 500);
        area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(new Color(250, 250, 250));

        JToolBar toolBar = new JToolBar();
        JButton loadBtn = new JButton("[LOAD] Oeffnen");
        JButton saveBtn = new JButton("[SAVE] Speichern");
        JButton clearBtn = new JButton("[CLEAR]");

        loadBtn.addActionListener(e -> loadAction());
        saveBtn.addActionListener(e -> saveAction());
        clearBtn.addActionListener(e -> area.setText(""));

        toolBar.add(loadBtn); toolBar.add(saveBtn); toolBar.addSeparator(); toolBar.add(clearBtn);

        if(f != null) loadFile(f);

        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);

        // Status Bar
        JLabel status = new JLabel(" Zeichensatz: UTF-8");
        status.setBorder(new BevelBorder(BevelBorder.LOWERED));
        add(status, BorderLayout.SOUTH);
    }

    private void loadAction() {
        String name = JOptionPane.showInputDialog("Dateiname in " + Main.VM_DIR + " (z.B. test.txt):");
        if(name != null) {
            File f = new File(Main.VM_DIR, name);
            if(f.exists()) loadFile(f);
            else JOptionPane.showMessageDialog(this, "Datei nicht gefunden!");
        }
    }

    private void loadFile(File f) {
        try {
            currentFile = f;
            area.setText(new String(Files.readAllBytes(f.toPath())));
        } catch(Exception e){ JOptionPane.showMessageDialog(this, "Fehler beim Laden."); }
    }

    private void saveAction() {
        if(currentFile == null) {
            String name = JOptionPane.showInputDialog("Speichern als:");
            if(name == null) return;
            currentFile = new File(Main.VM_DIR, name);
        }
        try {
            Files.write(currentFile.toPath(), area.getText().getBytes());
            JOptionPane.showMessageDialog(this, "Gespeichert!");
        } catch(Exception ex){ JOptionPane.showMessageDialog(this, "Fehler beim Speichern."); }
    }
}