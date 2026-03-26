package MyOS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

// ================= EXPLORER =================
public class ExplorerApp extends JInternalFrame {
    private DefaultListModel<File> model = new DefaultListModel<>();
    private JList<File> list = new JList<>(model);
    private File currentPath = new File(Main.VM_DIR);
    private static File fileClipboard = null;
    private static boolean isCutOperation = false;
    private static JTextField path;

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
                    if (f != null && f.delete()) refresh();
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { handleContextMenu(e); }
            public void mouseReleased(MouseEvent e) { handleContextMenu(e); }
            public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) openFile(); }
        });

        JPanel up = new JPanel(new BorderLayout());

        JButton backBtn = new JButton("[BACK] Zurueck");
        backBtn.addActionListener(e -> {
            if (currentPath.getParentFile() != null && currentPath.getPath().contains(Main.VM_DIR)) {
                currentPath = currentPath.getParentFile(); 
                refresh();
            }
        });


        path = new JTextField(currentPath.getAbsolutePath());
            path.addActionListener(e -> {
            String input = path.getText();
            File newPath;

            // Falls der User nur ab "VM_Disk" eingegeben hat, vervollständigen wir den Pfad
            if (input.startsWith("VM_Disk")) {
                // Wir nehmen das Eltern-Verzeichnis von VM_DIR und hängen die Eingabe dran
                File baseDir = new File(Main.VM_DIR).getParentFile();
                newPath = new File(baseDir, input);
            } else {
                // Falls er doch den vollen Pfad eingegeben hat
                newPath = new File(input);
            }

            // Validierung
            if (newPath.exists() && newPath.isDirectory() && newPath.getAbsolutePath().contains("VM_Disk")) {
                setPath(newPath);
            } else {
                JOptionPane.showMessageDialog(this, "Pfad nicht gefunden oder kein Zugriff.");
                refresh(); // Setzt die Anzeige auf den letzten gültigen Stand zurück
            }
        });

        
        up.add(backBtn, BorderLayout.WEST);
        up.add(path, BorderLayout.CENTER);      
        add(up, BorderLayout.NORTH);
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
        if (e.isPopupTrigger()) {
            int idx = list.locationToIndex(e.getPoint());
            JPopupMenu menu = new JPopupMenu();

            if (idx != -1 && list.getCellBounds(idx, idx).contains(e.getPoint())) {
                list.setSelectedIndex(idx);
                File f = list.getSelectedValue();

                menu.add(new JMenuItem(new AbstractAction("Kopieren") {
                    public void actionPerformed(ActionEvent e) { fileClipboard = f; isCutOperation = false; }
                }));
                menu.add(new JMenuItem(new AbstractAction("Ausschneiden") {
                    public void actionPerformed(ActionEvent e) { fileClipboard = f; isCutOperation = true; }
                }));

                if (f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".jpg")) {
                    menu.add(new JMenuItem(new AbstractAction("Als Hintergrund setzen") {
                        public void actionPerformed(ActionEvent e) { Main.setWallpaper(f.getAbsolutePath()); }
                    }));
                }

                if (f.getName().toLowerCase().endsWith(".java")) {
                    menu.add(new JMenuItem(new AbstractAction("Zu .jar kompilieren") {
                        public void actionPerformed(ActionEvent e) { compileJavaToJar(f); }
                    }));

                    menu.add(new JMenuItem(new AbstractAction("Als .illag packen") {
                        public void actionPerformed(ActionEvent e) {
                            Main.buildIllagFromJava(f);
                            refresh();
                        }
                    }));
                }

                if (f.getName().toLowerCase().endsWith(".jar")) {
                    menu.add(new JMenuItem(new AbstractAction("Als .illag packen") {
                        public void actionPerformed(ActionEvent e) {
                            String mainClass = JOptionPane.showInputDialog("Main-Klasse:");
                            if (mainClass != null && !mainClass.isEmpty()) {
                                Main.buildIllagFromJar(f, mainClass);
                            }
                            refresh();
                        }
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

                menu.add(new JMenuItem(new AbstractAction("Terminal hier öffnen") {
                    public void actionPerformed(ActionEvent e) {
                        // Wenn es ein Ordner ist, nimm ihn direkt, sonst den Parent
                        File targetDir = f.isDirectory() ? f : f.getParentFile();
                        Main.windowManager.openApp(new TerminalApp(targetDir));
                    }
                }));

                menu.add(new JMenuItem(new AbstractAction("Umbenennen") {
                    public void actionPerformed(ActionEvent e) {
                        String newName = JOptionPane.showInputDialog(ExplorerApp.this, "Neuer Name:", f.getName());
                        if (newName != null && !newName.isEmpty()) {
                            File newFile = new File(f.getParent(), newName);
                            if (f.renameTo(newFile)) refresh();
                            else JOptionPane.showMessageDialog(ExplorerApp.this, "Fehler beim Umbenennen.");
                        }
                    }
                }));

                menu.add(new JMenuItem(new AbstractAction("Eigenschaften") {
                    public void actionPerformed(ActionEvent e) {
                        String info = String.format(
                            "Name: %s\nGröße: %d Bytes\nTyp: %s\nPfad: %s",
                            f.getName(), f.length(), f.isDirectory() ? "Ordner" : "Datei", f.getAbsolutePath()
                        );
                        JOptionPane.showMessageDialog(ExplorerApp.this, info, "Eigenschaften", JOptionPane.INFORMATION_MESSAGE);
                    }
                }));

                menu.add(new JMenuItem(new AbstractAction("Loeschen") {
                    public void actionPerformed(ActionEvent e) { if (f.delete()) refresh(); }
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

                menu.add(new JMenuItem(new AbstractAction("Terminal hier öffnen") {
                    public void actionPerformed(ActionEvent e) {
                        Main.windowManager.openApp(new TerminalApp(currentPath));
                    }
                }));
                
                pasteItem.setEnabled(fileClipboard != null);
                menu.add(pasteItem);
            }
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void createNew(boolean isDir) {
        String name = JOptionPane.showInputDialog(this, "Name:");
        if (name != null && !name.isEmpty()) {
            File f = new File(currentPath, name);
            try {
                if (isDir) f.mkdir(); else f.createNewFile();
                refresh();
            } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Fehler beim Erstellen."); 
                ex.printStackTrace();
            }
        }
    }

    private void compileJavaToJar(File javaFile) {
        new Thread(() -> {
            try {
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Kein JDK gefunden (Compiler null).")
                    );
                    return;
                }

                int result = compiler.run(null, null, null, javaFile.getAbsolutePath());
                if (result != 0) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Kompilierungsfehler.")
                    );
                    return;
                }

                // --- Schritt 1: Alle relevanten class-Dateien finden ---
                File dir = javaFile.getParentFile();
                String baseName = javaFile.getName().replace(".java", "");

                File[] classFiles = dir.listFiles((d, name) ->
                    name.startsWith(baseName) && name.endsWith(".class")
                );

                if (classFiles == null || classFiles.length == 0) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Keine .class Dateien gefunden!")
                    );
                    return;
                }

                // --- Schritt 2: Jar-Datei vorbereiten ---
                File jarFile = new File(dir, baseName + ".jar");
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, baseName);

                // --- Schritt 3: Alle Klassen ins Jar schreiben ---
                try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
                    for (File cf : classFiles) {
                        jos.putNextEntry(new JarEntry(cf.getName()));
                        jos.write(Files.readAllBytes(cf.toPath()));
                        jos.closeEntry();
                    }
                }

                // Optional: .class Dateien löschen
                for (File cf : classFiles) cf.delete();

                SwingUtilities.invokeLater(() -> {
                    refresh();
                    JOptionPane.showMessageDialog(this, "Jar erfolgreich erstellt!");
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage())
                );
                ex.printStackTrace();
            }
        }).start();
    }

    private void refresh() {
        String fullPath = currentPath.getAbsolutePath();

        model.clear();
        // Pfad für die Anzeige kürzen
        if (fullPath.contains("VM_Disk")) {
            // Findet den Index von "VM_Disk" und nimmt den Rest des Strings
            String displayPath = fullPath.substring(fullPath.indexOf("VM_Disk"));
            path.setText(displayPath);
        } else {
            path.setText(fullPath);
        }

        File[] fs = currentPath.listFiles();
        if (fs != null) {
            Arrays.sort(fs, (a, b) -> a.isDirectory() == b.isDirectory() 
                ? a.getName().compareTo(b.getName()) 
                : a.isDirectory() ? -1 : 1);
            for (File f : fs) model.addElement(f);
        }
    }

    private void openFile() {
        File f = list.getSelectedValue();
        if (f == null) return;
        if (f.isDirectory()) {
            currentPath = f;
            refresh();
        } else {
            Main.executeFile(f);
        }
    }

    private void pasteFile() {
        if (fileClipboard == null) return;
        try {
            File dest = new File(currentPath, fileClipboard.getName());
            if (dest.exists()) dest = new File(currentPath, "Kopie_" + fileClipboard.getName());

            if (isCutOperation) {
                Files.move(fileClipboard.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileClipboard = null; 
            } else {
                Files.copy(fileClipboard.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            refresh();
        } catch (IOException e) { JOptionPane.showMessageDialog(this, "Fehler beim Operation."); e.printStackTrace(); }
    }

    public void applyTheme(String theme) {
        Color bg, fg;
        switch(theme) {
            case "Win95": bg = new Color(192, 192, 192); fg = Color.BLACK; break;
            case "Win10": bg = new Color(30, 30, 30); fg = Color.WHITE; break;
            case "macOS": bg = new Color(230, 230, 230); fg = Color.BLACK; break;
            case "Linux": bg = new Color(48, 10, 36); fg = Color.WHITE; break;
            default: bg = new Color(30, 30, 30); fg = Color.WHITE;
        }
        list.setBackground(bg);
        list.setForeground(fg);
        list.setFont(new Font("SansSerif", Font.PLAIN, 12));
        getContentPane().setBackground(bg);
    }
}