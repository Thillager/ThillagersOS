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

// ================= EXPLORER =================
    public class ExplorerApp extends JInternalFrame {
        private DefaultListModel<File> model = new DefaultListModel<>();
        private JList<File> list = new JList<>(model);
        private File currentPath = new File(Main.VM_DIR);
        private static File fileClipboard = null;
        private static boolean isCutOperation = false;

        public ExplorerApp() {
            super("Explorer", true, true, true, true);
            setSize(700, 500);
            setLayout(new BorderLayout());
            applyTheme(Main.currentTheme); // sofort Theme anwenden

            list.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    boolean isCmd = e.isControlDown() || e.isMetaDown();
                    if (isCmd && e.getKeyCode() == KeyEvent.VK_C) {
                        fileClipboard = list.getSelectedValue();
                        isCutOperation = false;
                    } else if (isCmd && e.getKeyCode() == KeyEvent.VK_X) {
                        fileClipboard = list.getSelectedValue();
                        isCutOperation = true;
                    } else if (isCmd && e.getKeyCode() == KeyEvent.VK_V) {
                        pasteFile();
                    } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        File f = list.getSelectedValue();
                        if(f != null && f.delete()) refresh();
                    }
                }
            });

            list.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { handleContextMenu(e); }
                public void mouseReleased(MouseEvent e) { handleContextMenu(e); }
                public void mouseClicked(MouseEvent e) { if(e.getClickCount() == 2) openFile(); }
            });

            JButton backBtn = new JButton("[BACK] Zurueck");
            backBtn.addActionListener(e -> {
                if(currentPath.getParentFile() != null && currentPath.getPath().contains(Main.VM_DIR)) {
                    currentPath = currentPath.getParentFile(); refresh();
                }
            });

            add(backBtn, BorderLayout.NORTH);
            add(new JScrollPane(list), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton openBtn = new JButton("[OPEN] Datei Oeffnen");
            openBtn.addActionListener(e -> openFile());
            bottom.add(openBtn);
            add(bottom, BorderLayout.SOUTH);

            refresh();
        }

        public void setPath(File path) {
            this.currentPath = path;
            refresh();
        }

        private void handleContextMenu(MouseEvent e) {
            if(e.isPopupTrigger()) {
                int idx = list.locationToIndex(e.getPoint());
                JPopupMenu menu = new JPopupMenu();

                if(idx != -1 && list.getCellBounds(idx, idx).contains(e.getPoint())) {
                    list.setSelectedIndex(idx);
                    File f = list.getSelectedValue();

                    menu.add(new JMenuItem(new AbstractAction("Kopieren") {
                        public void actionPerformed(ActionEvent e) { fileClipboard = f; isCutOperation = false; }
                    }));
                    menu.add(new JMenuItem(new AbstractAction("Ausschneiden") {
                        public void actionPerformed(ActionEvent e) { fileClipboard = f; isCutOperation = true; }
                    }));

                    if(f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".jpg")) {
                        menu.add(new JMenuItem(new AbstractAction("Als Hintergrund setzen") {
                            public void actionPerformed(ActionEvent e) { Main.setWallpaper(f.getAbsolutePath()); }
                        }));
                    }

                    if(f.getName().toLowerCase().endsWith(".java")) {
                        menu.add(new JMenuItem(new AbstractAction("Zu .jar kompilieren") {
                            public void actionPerformed(ActionEvent e) { compileJavaToJar(f); }
                        }));
                    }

                    menu.add(new JMenuItem(new AbstractAction("Verknuepfung auf Desktop") {
                        public void actionPerformed(ActionEvent e) { 
                            if (!Main.customShortcuts.contains(f)) {
                                Main.customShortcuts.add(f);
                                Main.saveSettings();
                                Main.addDesktopIcon(f.getName(), 160, 40, evt -> Main.executeFile(f)); 
                                Main.desktop.revalidate();
                                Main.desktop.repaint();
                            }
                        }
                    }));

                    menu.add(new JMenuItem(new AbstractAction("Mit Main-Klasse starten") {
    public void actionPerformed(ActionEvent e) {
        Main.runJarAskMain(f);
    }
}));

                    menu.add(new JMenuItem(new AbstractAction("Loeschen") {
                        public void actionPerformed(ActionEvent e) { if(f.delete()) refresh(); }
                    }));
                    menu.add(new JMenuItem(new AbstractAction("Umbenennen") {
    public void actionPerformed(ActionEvent e) {
        String newName = JOptionPane.showInputDialog(ExplorerApp.this, "Neuer Name:", f.getName());
        if(newName != null && !newName.isEmpty()) {
            File newFile = new File(f.getParent(), newName);
            if(f.renameTo(newFile)) refresh();
            else JOptionPane.showMessageDialog(ExplorerApp.this, "Fehler beim Umbenennen.");
        }
    }
}));


                } else {
                    list.clearSelection();
                    JMenu neuMenu = new JMenu("Neu");
                    neuMenu.add(new JMenuItem(new AbstractAction("Datei") {
                        public void actionPerformed(ActionEvent e) { createNew(false); }
                    }));
                    neuMenu.add(new JMenuItem(new AbstractAction("Ordner") {
                        public void actionPerformed(ActionEvent e) { createNew(true); }
                    }));
                    menu.add(neuMenu);

                    JMenuItem pasteItem = new JMenuItem(new AbstractAction("Einfuegen") {
                        public void actionPerformed(ActionEvent e) { pasteFile(); }
                    });
                    pasteItem.setEnabled(fileClipboard != null);
                    menu.add(pasteItem);
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        private void createNew(boolean isDir) {
            String name = JOptionPane.showInputDialog(this, "Name:");
            if(name != null && !name.isEmpty()) {
                File f = new File(currentPath, name);
                try {
                    if(isDir) f.mkdir(); else f.createNewFile();
                    refresh();
                } catch(IOException ex) { JOptionPane.showMessageDialog(this, "Fehler beim Erstellen."); }
            }
        }

        private void compileJavaToJar(File javaFile) {
            new Thread(() -> {
                try {
                    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                    if(compiler == null) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Kein JDK gefunden (Compiler null)."));
                        return;
                    }
                    int result = compiler.run(null, null, null, javaFile.getAbsolutePath());
                    if(result == 0) {
                        String classFileName = javaFile.getName().replace(".java", ".class");
                        File classFile = new File(javaFile.getParent(), classFileName);
                        File jarFile = new File(javaFile.getParent(), javaFile.getName().replace(".java", ".jar"));

                        Manifest manifest = new Manifest();
                        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

                        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
                            jos.putNextEntry(new JarEntry(classFileName));
                            jos.write(Files.readAllBytes(classFile.toPath()));
                            jos.closeEntry();
                        }
                        classFile.delete();
                        SwingUtilities.invokeLater(() -> { refresh(); JOptionPane.showMessageDialog(this, "Jar erfolgreich erstellt!"); });
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Kompilierungsfehler."));
                    }
                } catch(Exception ex) { 
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage()));
                }
            }).start();
        }

        private void refresh() {
            model.clear();
            File[] fs = currentPath.listFiles();
            if(fs != null) {
                Arrays.sort(fs, (a,b) -> a.isDirectory() == b.isDirectory() ? a.getName().compareTo(b.getName()) : a.isDirectory() ? -1 : 1);
                for(File f : fs) model.addElement(f);
            }
        }

        private void openFile() {
            File f = list.getSelectedValue();
            Main.executeFile(f);
            if (f != null && f.isDirectory()) refresh(); // Update self if it was a directory opening
        }

        private void pasteFile() {
            if(fileClipboard == null) return;
            try {
                File dest = new File(currentPath, fileClipboard.getName());
                if(dest.exists()) dest = new File(currentPath, "Kopie_" + fileClipboard.getName());

                if(isCutOperation) {
                    Files.move(fileClipboard.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    fileClipboard = null; 
                } else {
                    Files.copy(fileClipboard.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                refresh();
            } catch (IOException e) { JOptionPane.showMessageDialog(this, "Fehler beim Operation."); }
        }

        public void applyTheme(String theme) {
    Color bg, fg;
    switch(theme) {
        case "Win95":
            bg = new Color(192,192,192);
            fg = Color.BLACK;
            break;
        case "Win10":
            bg = new Color(30,30,30);
            fg = Color.WHITE;
            break;
        case "macOS":
            bg = new Color(230,230,230);
            fg = Color.BLACK;
            break;
        case "Linux":
            bg = new Color(48,10,36);
            fg = Color.WHITE;
            break;
        default:
            bg = new Color(30,30,30);
            fg = Color.WHITE;
    }
    list.setBackground(bg);
    list.setForeground(fg);
    list.setFont(new Font("SansSerif", Font.PLAIN, 12));
    getContentPane().setBackground(bg);
}

    }
