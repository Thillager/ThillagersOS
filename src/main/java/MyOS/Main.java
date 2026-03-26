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

// ================= MAIN OS CLASS =================
public class Main extends JFrame {

    public static Color currentBg = new Color(30, 30, 30);
    public static Color accentColor = new Color(0, 120, 215);
    public static Color taskbarColor = new Color(20, 20, 20);
    public static Color textColor = Color.WHITE;
    public static String currentTheme = "Win10";
    public static String wallpaperPath = "";

    public static Image wallpaper = null;
    private static JPanel taskIconsPanel;
    public static JDesktopPane desktop;
    private static JLayeredPane lp;
    public static final String VM_DIR = "VM_Disk";
    private static int windowOffset = 0;
    private static Map<JInternalFrame, JButton> windowToButtonMap = new HashMap<>();
    public static JPanel taskbar;
    private StartMenu customStartMenu;

    public static Properties systemProps = new Properties();
    public static long timeOffsetMillis = 0;
    long themeOffsetMillis = 0;
    public static List<File> customShortcuts = new ArrayList<>();

    public static Main instance;

    public Main() {
        instance = this;
        File vmDir = new File(VM_DIR);
        if (!vmDir.exists())
            vmDir.mkdir();

        setTitle("Thillagers OS");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(sz.width, sz.height);
        setLocation(0, 0);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        lp = new JLayeredPane();
        setContentPane(lp);
        lp.setLayout(null);

        desktop = new JDesktopPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (wallpaper != null) {
                    g.drawImage(wallpaper, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(currentBg);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        desktop.setLayout(null);

        taskbar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // Hintergrund komplett füllen, auch bei halbtransparent
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                taskbar.setOpaque(true);
            }
        };

        JButton startBtn = new JButton("START");
        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> {
            if (customStartMenu == null || !customStartMenu.isVisible()) {
                customStartMenu = new StartMenu(this);
                // Dynamische Berechnung des Y-Werts für das Startmenü
                int h = getContentPane().getHeight();
                int menuY = (currentTheme.equals("macOS")) ? 40 : (h - 450);
                customStartMenu.showAt(0, menuY);
            } else {
                customStartMenu.dispose();
            }
        });
        taskbar.add(startBtn, BorderLayout.WEST);

        taskIconsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        taskIconsPanel.setOpaque(false);
        taskbar.add(taskIconsPanel, BorderLayout.CENTER);

        JLabel timeLabel = new JLabel();
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setBorder(new EmptyBorder(0, 0, 0, 20));
        timeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        timeLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String newTime = JOptionPane.showInputDialog("Uhrzeit einstellen (HH:mm):");
                if (newTime != null && newTime.matches("\\d{1,2}:\\d{2}")) {
                    try {
                        String[] parts = newTime.split(":");
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                        cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
                        cal.set(Calendar.SECOND, 0);
                        timeOffsetMillis = cal.getTimeInMillis() - System.currentTimeMillis();
                        saveSettings();
                    } catch (Exception ex) {
                    }
                }
            }
        });
        new javax.swing.Timer(1000, e -> {
            long themeOffsetMillis = currentTheme.equals("macOS") ? 3600_000 : 0;
            timeLabel.setText(
                    new SimpleDateFormat("HH:mm:ss").format(
                            new Date(System.currentTimeMillis() + timeOffsetMillis + themeOffsetMillis)));
        }).start();

        taskbar.add(timeLabel, BorderLayout.EAST);
        loadSettings();
        applyTheme(currentTheme);
        if (!wallpaperPath.isEmpty())
            setWallpaper(wallpaperPath);
        // Position wird jetzt vom ComponentListener gesetzt
        lp.add(desktop, JLayeredPane.DEFAULT_LAYER);
        lp.add(taskbar, JLayeredPane.PALETTE_LAYER); // PALTTE_LAYER liegt über DEFAULT_LAYER

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getContentPane().getWidth();
                int h = getContentPane().getHeight();

                desktop.setBounds(0, 0, w, h);

                int tbHeight;
                int tbY;

                switch (currentTheme) {
                    case "macOS":
                        tbHeight = 35;
                        tbY = 0; // oben
                        break;
                    case "Win95":
                        tbHeight = 40;
                        tbY = h - tbHeight; // unten
                        break;
                    default: // Win10, Linux
                        tbHeight = 55;
                        tbY = h - tbHeight; // unten
                        break;
                }

                taskbar.setBounds(0, tbY, w, tbHeight);

                // Desktop-Icons dynamisch positionieren
                int iconYStart = (currentTheme.equals("macOS")) ? tbHeight + 10 : 40;
                initDesktopIcons(h - tbHeight, iconYStart);

                taskbar.repaint();
            }
        });
    }

    public static void applyTheme(String theme) {
        currentTheme = theme;

        // Farben & Schriftarten
        Color bgColor, taskbarBg, textCol, iconBorder;
        Font defaultFont;

        switch (theme) {
            case "Win95":
                bgColor = new Color(192, 192, 192);
                taskbarBg = new Color(192, 192, 192);
                textCol = Color.BLACK;
                iconBorder = Color.BLACK;
                defaultFont = new Font("MS Sans Serif", Font.PLAIN, 12);
                break;
            case "Win10":
                bgColor = new Color(30, 30, 30);
                taskbarBg = new Color(20, 20, 20);
                textCol = Color.GRAY;
                iconBorder = Color.WHITE;
                defaultFont = new Font("Segoe UI", Font.PLAIN, 13);
                break;
            case "macOS":
                bgColor = new Color(220, 220, 220);
                taskbarBg = new Color(255, 255, 255, 220);
                textCol = Color.BLACK;
                iconBorder = Color.GRAY;
                defaultFont = new Font("San Francisco", Font.PLAIN, 13);
                break;
            case "Linux":
                bgColor = new Color(48, 10, 36);
                taskbarBg = new Color(30, 30, 30);
                textCol = Color.WHITE;
                iconBorder = Color.WHITE;
                defaultFont = new Font("Ubuntu", Font.PLAIN, 13);
                break;
            default:
                bgColor = new Color(30, 30, 30);
                taskbarBg = new Color(20, 20, 20);
                textCol = Color.WHITE;
                iconBorder = Color.WHITE;
                defaultFont = new Font("Segoe UI", Font.PLAIN, 13);
        }

        // Explorer & interne Fenster
        if (instance != null && instance.desktop != null) {
            for (JInternalFrame frame : instance.desktop.getAllFrames()) {
                if (frame instanceof ExplorerApp) {
                    ((ExplorerApp) frame).applyTheme(currentTheme);
                } else {
                    frame.getContentPane().setBackground(bgColor);
                    for (Component c : frame.getContentPane().getComponents()) {
                        c.setBackground(bgColor);
                        c.setForeground(textCol);
                        c.setFont(defaultFont);
                    }
                }
            }
        }

        // Globale Farben
        currentBg = bgColor;
        taskbarColor = taskbarBg;
        textColor = textCol;

        // Taskbar
        if (instance != null && instance.taskbar != null) {
            instance.taskbar.setBackground(taskbarBg);
            for (Component c : instance.taskbar.getComponents()) {
                c.setForeground(textCol);
                c.setFont(defaultFont);
                if (c instanceof JButton) {
                    JButton btn = (JButton) c;
                    btn.setBackground(taskbarBg);
                    btn.setForeground(textCol);
                }
            }
        }

        // Desktop-Icons
        if (instance != null && instance.desktop != null) {
            for (Component c : instance.desktop.getComponents()) {
                if (c instanceof JPanel) {
                    JPanel p = (JPanel) c;
                    for (Component inner : p.getComponents()) {
                        if (inner instanceof JLabel) {
                            JLabel label = (JLabel) inner;
                            label.setForeground(textCol);
                            label.setFont(defaultFont.deriveFont(Font.BOLD, 12));
                            label.setBorder(new LineBorder(iconBorder, 1));
                        }
                        if (inner instanceof JButton) {
                            JButton btn = (JButton) inner;
                            btn.setForeground(textCol);
                            btn.setFont(defaultFont.deriveFont(Font.PLAIN, 10));
                            btn.setBackground(bgColor);
                        }
                    }
                }
            }
        }

        // StartMenu
        if (instance != null && instance.customStartMenu != null) {
            instance.customStartMenu.getContentPane().setBackground(bgColor);
            for (Component c : instance.customStartMenu.getContentPane().getComponents()) {
                c.setForeground(textCol);
                c.setFont(defaultFont);
                c.setBackground(bgColor);
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel) c).getComponents()) {
                        inner.setForeground(textCol);
                        inner.setFont(defaultFont);
                        inner.setBackground(bgColor);
                    }
                }
            }
        }

        // Explorer, Terminal, TextEditor
        if (instance != null && instance.desktop != null) {
            for (JInternalFrame frame : instance.desktop.getAllFrames()) {
                frame.getContentPane().setBackground(bgColor);
                for (Component c : frame.getContentPane().getComponents()) {
                    c.setForeground(textCol);
                    c.setFont(defaultFont);
                    c.setBackground(bgColor);
                    if (c instanceof JScrollPane) {
                        JScrollPane sp = (JScrollPane) c;
                        if (sp.getViewport().getView() instanceof JTextArea) {
                            JTextArea ta = (JTextArea) sp.getViewport().getView();
                            ta.setBackground(bgColor);
                            ta.setForeground(textCol);
                            ta.setFont(defaultFont);
                        }
                        if (sp.getViewport().getView() instanceof JLabel) {
                            JLabel l = (JLabel) sp.getViewport().getView();
                            l.setForeground(textCol);
                            l.setFont(defaultFont);
                        }
                    }
                    if (c instanceof JToolBar) {
                        JToolBar tb = (JToolBar) c;
                        tb.setBackground(bgColor);
                        for (Component btn : tb.getComponents()) {
                            if (btn instanceof JButton) {
                                JButton b = (JButton) btn;
                                b.setForeground(textCol);
                                b.setBackground(bgColor);
                                b.setFont(defaultFont);
                            }
                        }
                    }
                }
            }
        }

        // Taskbar Größe & repaint
        if (instance != null) {
            Dimension sz = instance.getContentPane().getSize();
            int tbHeight, tbY;
            switch (currentTheme) {
                case "macOS":
                    tbHeight = 35;
                    tbY = 0;
                    break;
                case "Win95":
                    tbHeight = 40;
                    tbY = sz.height - tbHeight;
                    break;
                default:
                    tbHeight = 55;
                    tbY = sz.height - tbHeight;
                    break;
            }
            instance.taskbar.setBounds(0, tbY, sz.width, tbHeight);
            instance.taskbar.repaint();
            instance.revalidate();
            instance.repaint();
        }
    }

    private void initDesktopIcons(int screenHeight, int yOffset) {
        desktop.removeAll();

        addDesktopIcon("Terminal", 40, yOffset, e -> openApp(new TerminalApp(new File(VM_DIR))));
        addDesktopIcon("Explorer", 40, yOffset + 120, e -> openApp(new ExplorerApp()));
        addDesktopIcon("App Store", 40, yOffset + 240, e -> openApp(new AppStore()));
        addDesktopIcon("Browser", 40, yOffset + 480, e -> openApp(new BrowserApp()));
        addDesktopIcon("Taskmanager", 40, yOffset + 600, e -> openApp(new SystemMonitorApp()));

        // Desktop-JARs
        File dir = new File(VM_DIR);
        File[] files = dir.listFiles();
        int startX = 160;
        int startY = yOffset; // wichtig!
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                if (f.getName().endsWith(".jar")) {
                    addDesktopIcon(f.getName(), startX, startY, e -> executeFile(f));
                    startY += 120;
                    if (startY > screenHeight - 200) {
                        startY = yOffset;
                        startX += 120;
                    }
                }
            }
        }

        // Benutzer-Shortcuts
        for (File f : customShortcuts) {
            if (f.exists()) {
                addDesktopIcon(f.getName(), startX, startY, e -> executeFile(f));
                startY += 120;
                if (startY > screenHeight - 200) {
                    startY = yOffset;
                    startX += 120;
                }
            }
        }

        desktop.revalidate();
        desktop.repaint();
    }

    public static void runJarAskMain(File f) {
        String mainClass = JOptionPane.showInputDialog(
                null,
                "Main-Klasse eingeben (z.B. com.example.Main):",
                "Jar starten",
                JOptionPane.QUESTION_MESSAGE);

        if (mainClass == null || mainClass.trim().isEmpty())
            return;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp",
                    f.getAbsolutePath(),
                    mainClass);

            pb.directory(f.getParentFile()); // wichtig!
            pb.inheritIO(); // zeigt Fehler im Terminal
            pb.start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim Starten!");
        }
    }

    public static void runJar(File f) {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", f.getName());
            pb.directory(f.getParentFile()); // WICHTIG!
            pb.inheritIO(); // zeigt Fehler im Terminal
            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Startfehler: " + f.getName());
        }
    }

    public static void runJarInTerminal(File jarFile, String mainClass) {
        TerminalApp term = new TerminalApp(jarFile.getParentFile());
        Main.openApp(term);

        try {
            // 1. Prozess-Builder Setup
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", jarFile.getAbsolutePath(), mainClass);
            pb.directory(jarFile.getParentFile());

            // Startet den Prozess
            Process proc = pb.start();

            // 2. OUTPUT-BRIDGE: Streams vom JAR lesen und im Terminal anzeigen
            new Thread(() -> term.readStream(proc.getInputStream())).start();
            new Thread(() -> term.readStream(proc.getErrorStream())).start();

            // 3. INPUT-BRIDGE: Eingaben vom Terminal-Textfeld an das JAR senden
            // Wir brauchen einen Writer, um in den "Standard-In" des JARs zu schreiben
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));

            // Wir definieren, was passiert, wenn du ENTER im Terminal drückst
            ActionListener inputBridge = e -> {
                try {
                    String input = term.getInputField().getText();
                    writer.write(input + "\n"); // Schickt den Text + Zeilenumbruch an das JAR
                    writer.flush(); // Erzwingt das Senden (wichtig für Scanner!)
                    term.getInputField().setText(""); // Feld leeren
                } catch (IOException ex) {
                    // Passiert meistens, wenn das JAR bereits geschlossen wurde
                    System.err.println("Fehler beim Senden an JAR: " + ex.getMessage());
                }
            };

            // Den Listener an das Eingabefeld hängen
            term.getInputField().addActionListener(inputBridge);

            // 4. CLEANUP: Wenn das JAR beendet wird, den Listener wieder entfernen
            new Thread(() -> {
                try {
                    proc.waitFor(); // Warten bis das Programm fertig ist
                    // Wichtig: Listener entfernen, damit das Terminal danach wieder normal nutzbar
                    // ist
                    term.getInputField().removeActionListener(inputBridge);
                    writer.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Fehler beim Starten: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void runJarGui(File jarFile, String mainClass) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", jarFile.getAbsolutePath(), mainClass);
            pb.directory(jarFile.getParentFile());
            pb.inheritIO(); // Fehler im Terminal sichtbar
            pb.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Fehler beim Starten: " + e.getMessage());
        }
    }

    public static void runJava(File javaFile) {
        try {
            // Prüfen, ob GUI/Swing verwendet wird
            boolean isSwingApp = Files.lines(javaFile.toPath())
                    .anyMatch(line -> line.contains("javax.swing") || line.contains("JFrame")
                            || line.contains("JButton"));

            if (isSwingApp) {
                // GUI-Datei → separater Prozess starten
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) {
                    JOptionPane.showMessageDialog(null, "Kein JDK gefunden!");
                    return;
                }

                // Kompilieren
                int result = compiler.run(null, null, null, javaFile.getAbsolutePath());
                if (result != 0) {
                    JOptionPane.showMessageDialog(null, "Kompilierungsfehler!");
                    return;
                }

                String className = javaFile.getName().replace(".java", "");
                ProcessBuilder pb = new ProcessBuilder("java", className);
                pb.directory(javaFile.getParentFile());
                pb.inheritIO(); // Ausgaben erscheinen im Terminal
                pb.start();

            } else {
                // Terminal-Datei → TerminalApp nutzen
                TerminalApp term = new TerminalApp(javaFile.getParentFile());
                openApp(term);
                term.getInputField().setText("java " + javaFile.getName());
                term.getInputField().postActionEvent();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Fehler beim Starten: " + e.getMessage());
        }
    }

    public static void runIllag(File illagFile) {
        try {
            File tempDir = Files.createTempDirectory("illag_run").toFile();

            // ENTpacken
            try (JarFile jar = new JarFile(illagFile)) {
                jar.stream().forEach(entry -> {
                    try {
                        File out = new File(tempDir, entry.getName());
                        if (entry.isDirectory()) {
                            out.mkdirs();
                        } else {
                            out.getParentFile().mkdirs();
                            InputStream is = jar.getInputStream(entry);
                            Files.copy(is, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // CONFIG laden
            Properties props = new Properties();
            props.load(new FileInputStream(new File(tempDir, "config.properties")));

            String mainClass = props.getProperty("main");
            String mode = props.getProperty("mode");

            if (mainClass == null || mainClass.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Keine Main-Klasse gefunden!");
                return;
            }

            // ===== KORREKTES STARTEN BASIEREND AUF MODE =====
            switch (mode.toLowerCase()) {
                case "java":
                    // GUI Java-Datei
                    File srcDir = new File(tempDir, "src");
                    File javaFile = new File(srcDir, mainClass + ".java");
                    if (javaFile.exists()) {
                        runJava(javaFile); // nutzt schon runJava() für GUI oder Terminal
                    } else {
                        // Falls keine .java, evtl .jar erstellen
                        File jarFile = new File(tempDir, "app.jar");
                        if (jarFile.exists())
                            runJarGui(jarFile, mainClass);
                    }
                    break;

                case "jar":
                    // GUI JAR
                    File jarFile = new File(tempDir, "app.jar");
                    if (jarFile.exists())
                        runJarGui(jarFile, mainClass);
                    break;

                case "terminal":
                    // Terminal-Modus
                    jarFile = new File(tempDir, "app.jar");
                    if (jarFile.exists())
                        runJarInTerminal(jarFile, mainClass);
                    else {
                        // Wenn es eine Java-Datei ist
                        srcDir = new File(tempDir, "src");
                        javaFile = new File(srcDir, mainClass + ".java");
                        if (javaFile.exists()) {
                            TerminalApp term = new TerminalApp(srcDir);
                            Main.openApp(term);
                            SwingUtilities.invokeLater(() -> {
                                term.getInputField().setText("java " + mainClass);
                                term.getInputField().postActionEvent();
                            });
                        }
                    }
                    break;

                default:
                    JOptionPane.showMessageDialog(null, "Unbekannter Mode: " + mode);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim Starten der .illag Datei!");
        }
    }

    public static void buildIllagFromJava(File javaFile) {
        try {
            // Dialog: Terminal oder GUI
            String[] options = { "GUI", "Terminal" };
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Soll die App im Terminal oder GUI laufen?",
                    "Mode wählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            String mode = (choice == 1) ? "terminal" : "java"; // "java" = GUI, "terminal" = Terminal

            File out = new File(javaFile.getParent(), javaFile.getName().replace(".java", ".illag"));

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
                // config.properties erstellen
                jos.putNextEntry(new JarEntry("config.properties"));
                String config = "main=" + javaFile.getName().replace(".java", "") + "\nmode=" + mode;
                jos.write(config.getBytes());
                jos.closeEntry();

                // src
                jos.putNextEntry(new JarEntry("src/" + javaFile.getName()));
                jos.write(Files.readAllBytes(javaFile.toPath()));
                jos.closeEntry();
            }

            JOptionPane.showMessageDialog(null, ".illag erfolgreich erstellt!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void buildIllagFromJar(File jarFile, String mainClass) {
        try {
            // Dialog: Terminal oder GUI
            String[] options = { "GUI", "Terminal" };
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Soll die App im Terminal oder GUI laufen?",
                    "Mode wählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            String mode = (choice == 1) ? "terminal" : "jar"; // "jar" = GUI, "terminal" = Terminal

            File out = new File(jarFile.getParent(), jarFile.getName().replace(".jar", ".illag"));

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
                // config.properties erstellen
                jos.putNextEntry(new JarEntry("config.properties"));
                String config = "main=" + mainClass + "\nmode=" + mode;
                jos.write(config.getBytes());
                jos.closeEntry();

                // app.jar hinzufügen
                jos.putNextEntry(new JarEntry("app.jar"));
                jos.write(Files.readAllBytes(jarFile.toPath()));
                jos.closeEntry();
            }

            JOptionPane.showMessageDialog(null, ".illag erfolgreich erstellt!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void executeFile(File f) {
        if (f == null || !f.exists())
            return;

        if (f.isDirectory()) {
            ExplorerApp app = new ExplorerApp();
            app.setPath(f);
            openApp(app);

        } else if (f.getName().toLowerCase().endsWith(".jar")) {
            String mainClass = null;
            boolean forceGui = false;

            // Spezielle JAR prüfen
            if (f.getName().equalsIgnoreCase("Calculator.jar")) {
                mainClass = "SuperCalculator";
                forceGui = true; // GUI erzwingen
            }
            if (f.getName().equalsIgnoreCase("TuiCalc.jar")) {
                mainClass = "TuiCalc";
                forceGui = false;
            }

            // Wenn noch keine Main-Klasse gesetzt, vom Benutzer abfragen
            if (mainClass == null || mainClass.trim().isEmpty()) {
                mainClass = JOptionPane.showInputDialog(
                        null,
                        "Main-Klasse eingeben (z.B. com.example.Main):",
                        "Jar starten",
                        JOptionPane.QUESTION_MESSAGE);
                if (mainClass == null || mainClass.trim().isEmpty())
                    return;
            }

            if (forceGui == true) {
                runJarGui(f, mainClass);
            } else if (forceGui == false) {
                runJarInTerminal(f, mainClass);
            } else {
                // Abfrage, ob im Terminal gestartet werden soll
                int result = JOptionPane.showConfirmDialog(
                        null,
                        "Soll die JAR im Terminal gestartet werden?",
                        "Terminal starten?",
                        JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    runJarInTerminal(f, mainClass);
                } else {
                    runJarGui(f, mainClass);
                }
            }

        } else if (f.getName().toLowerCase().endsWith(".java")) {
            runJava(f); // Java-Dateien starten

        } else if (f.getName().toLowerCase().endsWith(".illag")) {
            runIllag(f);

        } else if (f.getName().toLowerCase().endsWith(".png") ||
                f.getName().toLowerCase().endsWith(".jpg") ||
                f.getName().toLowerCase().endsWith(".jpeg")) {
            openApp(new ImageViewer(f));

        } else {
            openApp(new TextEditor(f));
        }
    }

    public static void setWallpaper(String path) {
        wallpaperPath = path;
        wallpaper = new ImageIcon(path).getImage();
        desktop.repaint();
        saveSettings();
    }

    private static ImageIcon getIconFor(String title) {
        String path;

        if (currentTheme.equals("macOS")) {
            switch (title.toLowerCase()) {
                case "terminal":
                    path = "/resources/terminal.png";
                    break;
                case "explorer":
                    path = "/resources/explorer.png";
                    break;
                case "app store":
                    path = "/resources/store.jpeg";
                    break;
                case "web browser":
                    path = "/resources/browser.jpeg";
                    break;
                default:
                    if (title.endsWith(".jar"))
                        path = "/resources/jar.jpeg";
                    else if (title.endsWith(".java"))
                        path = "/resources/jar.png";
                    else if (title.endsWith(".illag"))
                        path = "/resources/illag.png";
                    else
                        path = "/resources/file.png";
                    System.out.println("Icon-Pfad: " + path);

            }
        } else {

            switch (title.toLowerCase()) {
                case "terminal":
                    path = "/terminal.png";
                    break;
                case "explorer":
                    path = "/explorer.jpeg";
                    break;
                case "app store":
                    path = "/store.jpg";
                    break;
                case "browser":
                    path = "/browser.png";
                    break;
                default:
                    if (title.endsWith(".jar"))
                        path = "/jar.jpeg";
                    else
                        path = "/file.png";
                    System.out.println("Icon-Pfad: " + path);
            }
        }

        URL url = Main.class.getResource(path);
        if (url == null)
            return null;

        ImageIcon icon = new ImageIcon(url);
        Image scaled = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static void addDesktopIcon(String title, int defaultX, int defaultY, ActionListener action) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setName("ICON_" + title);

        int x = defaultX;
        int y = defaultY;
        String pos = systemProps.getProperty("pos_ICON_" + title);
        if (pos != null && pos.contains(",")) {
            try {
                String[] pts = pos.split(",");
                x = Integer.parseInt(pts[0]);
                y = Integer.parseInt(pts[1]);
            } catch (Exception e) {
            }
        }
        p.setBounds(x, y, 100, 100);

        String labelText = title.endsWith(".jar") ? "[JAR]"
                : (title.endsWith(".java") ? "[JAVA]" : (title.contains(".") ? "[FILE]" : "[DIR]"));
        if (title.equals("Terminal") || title.equals("Explorer") || title.equals("App Store")
                || title.equals("Browser"))
            labelText = "[APP]";

        ImageIcon icon = getIconFor(title);

        JLabel img;
        if (icon != null) {
            img = new JLabel(icon);
        } else {
            img = new JLabel(labelText, SwingConstants.CENTER); // fallback
        }
        String iconPath = systemProps.getProperty("icon_" + title);
        if (iconPath != null && !iconPath.isEmpty()) {
            File f = new File(iconPath);
            if (f.exists()) {
                ImageIcon customIcon = new ImageIcon(iconPath);
                Image scaled = customIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                img.setIcon(new ImageIcon(scaled));
            }
        }

        img.setFont(new Font("Monospaced", Font.BOLD, 18));
        img.setForeground(textColor);
        img.setBorder(new LineBorder(textColor, 1));

        JPopupMenu menu = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("Löschen");
        JMenuItem changeIconItem = new JMenuItem("Icon ändern");

        menu.add(deleteItem);
        menu.add(changeIconItem);

        MouseAdapter ma = new MouseAdapter() {
            int pX, pY;

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && action != null)
                    action.actionPerformed(null);
            }

            public void mousePressed(MouseEvent e) {
                pX = e.getX();
                pY = e.getY();

                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseDragged(MouseEvent e) {
                Point pLoc = p.getLocation();
                p.setLocation(pLoc.x + e.getX() - pX, pLoc.y + e.getY() - pY);
                desktop.repaint();
            }

            public void mouseReleased(MouseEvent e) {
                saveSettings();
            }
        };

        deleteItem.addActionListener(e -> {
            desktop.remove(p);
            customShortcuts.removeIf(file -> file.getName().equals(title));
            saveSettings();
            desktop.repaint();
        });

        changeIconItem.addActionListener(e -> {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            int result = chooser.showOpenDialog(null);

            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                File imgFile = chooser.getSelectedFile();

                try {
                    ImageIcon newIcon = new ImageIcon(imgFile.getAbsolutePath());
                    Image scaled = newIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                    img.setIcon(new ImageIcon(scaled));

                    // Optional speichern (siehe unten)
                    systemProps.setProperty("icon_" + title, imgFile.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        img.addMouseListener(ma);
        img.addMouseMotionListener(ma);

        JButton btn = new JButton(title);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btn.addActionListener(action);

        p.add(img, BorderLayout.CENTER);
        p.add(btn, BorderLayout.SOUTH);
        desktop.add(p);
    }

    public static void openApp(JInternalFrame app) {
        // Position des Fensters
        app.setLocation(100 + windowOffset, 80 + windowOffset);
        windowOffset = (windowOffset + 30) % 300;

        // Fenster zum Desktop hinzufügen
        desktop.add(app);
        app.setVisible(true);

        // ← HIER kommt der Theme-Code
        SwingUtilities.invokeLater(() -> {
            if (instance != null) {
                instance.applyTheme(currentTheme); // wendet Theme auf das gerade geöffnete Fenster an
            }
        });

        try {
            app.setSelected(true);
        } catch (Exception e) {
        }

        // TASKBAR-ICON erstellen
        ImageIcon icon = getIconFor(app.getTitle());
        JButton tBtn = new JButton();
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

        tBtn.addActionListener(e -> {
            try {
                if (app.isIcon()) {
                    // 1. Fall: Wurde über das [-] Symbol minimiert
                    app.setIcon(false); // Holt es aus dem minimierten Zustand zurück
                    app.setVisible(true);
                    app.toFront();
                    app.setSelected(true);
                } else if (app.isVisible() && app.isSelected()) {
                    // 2. Fall: Ist offen und im Fokus -> Wir verstecken es
                    app.setVisible(false);
                    // Hinweis: Wenn du stattdessen willst, dass es sich wie beim [-] Klick verhält,
                    // nutze hier: app.setIcon(true);
                } else {
                    // 3. Fall: Ist im Hintergrund oder unsichtbar -> Ab in den Vordergrund
                    app.setVisible(true);
                    app.toFront();
                    app.setSelected(true);
                }
            } catch (java.beans.PropertyVetoException ex) {
                // SetIcon kann eine VetoException werfen, die müssen wir fangen
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        taskIconsPanel.add(tBtn);
        taskIconsPanel.revalidate();
        taskIconsPanel.repaint();

        app.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                taskIconsPanel.remove(tBtn);
                taskIconsPanel.revalidate();
                taskIconsPanel.repaint();
                taskbar.repaint();
            }
        });
    }

    public static void saveSettings() {
        try (OutputStream out = new FileOutputStream("system.cfg")) {
            systemProps.setProperty("theme", currentTheme);

            // Wallpaper relativ speichern
            if (wallpaperPath != null && !wallpaperPath.isEmpty()) {
                File f = new File(wallpaperPath);
                String relPath = f.getAbsolutePath().startsWith(new File(VM_DIR).getAbsolutePath())
                        ? f.getAbsolutePath().substring(new File(VM_DIR).getAbsolutePath().length() + 1)
                        : f.getName(); // falls außerhalb von VM_Disk, nur Name
                systemProps.setProperty("wallpaper", relPath);
            } else {
                systemProps.setProperty("wallpaper", "");
            }

            systemProps.setProperty("timeOffset", String.valueOf(timeOffsetMillis));

            if (desktop != null) {
                for (Component c : desktop.getComponents()) {
                    if (c.getName() != null && c.getName().startsWith("ICON_")) {
                        systemProps.setProperty("pos_" + c.getName(), c.getX() + "," + c.getY());
                    }
                }
            }

            // Shortcuts relativ speichern
            StringBuilder sb = new StringBuilder();
            for (File f : customShortcuts) {
                String relPath = f.getAbsolutePath().startsWith(new File(VM_DIR).getAbsolutePath())
                        ? f.getAbsolutePath().substring(new File(VM_DIR).getAbsolutePath().length() + 1)
                        : f.getName();
                sb.append(relPath).append(";");
            }
            systemProps.setProperty("shortcuts", sb.toString());

            systemProps.store(out, "System Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        try (InputStream in = new FileInputStream("system.cfg")) {
            systemProps.load(in);
            currentTheme = systemProps.getProperty("theme", "Win10");
            String wp = systemProps.getProperty("wallpaper", "");
            wallpaperPath = wp.isEmpty() ? "" : new File(VM_DIR, wp).getAbsolutePath();
            timeOffsetMillis = Long.parseLong(systemProps.getProperty("timeOffset", "0"));

            String sc = systemProps.getProperty("shortcuts", "");
            customShortcuts.clear();
            if (!sc.isEmpty()) {
                for (String p : sc.split(";")) {
                    if (!p.isEmpty())
                        customShortcuts.add(new File(VM_DIR, p));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}