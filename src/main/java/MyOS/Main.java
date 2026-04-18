package MyOS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLClassLoader;
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

import MyOS.SettingsApp;
import MyOS.api.MyOSApp;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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

    public static WindowManager windowManager;

    public static boolean customTaskbarColor = false;
    public static boolean customTextColor = false;

    public Main() {
        instance = this;
        File vmDir = new File(VM_DIR);
        if (!vmDir.exists())
            vmDir.mkdir();

        setTitle("Thillagers OS");
        setUndecorated(false);
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
                    } catch (Exception ex) { JOptionPane.showMessageDialog(null, "Ungültiges Format!"); 
                        ex.printStackTrace();
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
        ThemeManager.applyTheme(currentTheme);
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

            windowManager = new WindowManager(desktop, taskIconsPanel);
    }

    private void initDesktopIcons(int screenHeight, int yOffset) {
        desktop.removeAll();

        addDesktopIcon("Terminal", 40, yOffset, e -> windowManager.openApp(new TerminalApp(new File(VM_DIR))));
        addDesktopIcon("Explorer", 40, yOffset + 120, e -> windowManager.openApp(new ExplorerApp()));
        addDesktopIcon("App Store", 40, yOffset + 240, e -> windowManager.openApp(new AppStore()));
        addDesktopIcon("Browser", 40, yOffset + 480, e -> windowManager.openApp(new BrowserApp()));
        addDesktopIcon("Taskmanager", 40, yOffset + 600, e -> windowManager.openApp(new SystemMonitorApp()));

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

    public StartMenu getCustomStartMenu() {
        return customStartMenu;
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

    public static void runJarAsInternalApp(File jarFile, String className) {
        try {
            URL[] urls = { jarFile.toURI().toURL() };
            URLClassLoader cl = new URLClassLoader(urls);

            Class<?> cls = cl.loadClass(className);

            Object obj = cls.getDeclaredConstructor().newInstance();

            if (!(obj instanceof MyOS.api.MyOSApp)) {
                JOptionPane.showMessageDialog(null, "Keine gültige ThillagersOS App!");
                return;
            }

            MyOS.api.MyOSApp app = (MyOS.api.MyOSApp) obj;

            JInternalFrame frame = new JInternalFrame(
                    app.getAppName(),
                    true, true, true, true
            );

            frame.setSize(600, 400);
            int offset = windowOffset * 30;

            frame.setLocation(50 + offset, 50 + offset);

            // Offset erhöhen (für nächsten Window-Start)
            windowOffset++;

            // Reset wenn zu viele Fenster offen (sonst wandert es raus)
            if (windowOffset > 10) {
                windowOffset = 0;
            }
            frame.add(app.createUI());
            frame.setVisible(true);

            desktop.add(frame);
            frame.setSelected(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim Laden der App!");
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
        windowManager.openApp(term);

        try {
            // 1. Prozess-Builder Setup
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", jarFile.getAbsolutePath(), mainClass);
            pb.directory(jarFile.getParentFile());

            // Startet den Prozess
            Process proc = pb.start();

            // 2. OUTPUT-BRIDGE: Streams vom JAR lesen und im Terminal anzeigen
            new Thread(() -> {
                term.readStream(proc.getInputStream());
            }).start();
            new Thread(() -> {
                term.readStream(proc.getErrorStream());
            }).start();

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
            e.printStackTrace();
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
                windowManager.openApp(term);
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

            // Entpacken
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
            String mode = props.getProperty("mode", "ask"); // Standard: nachfragen

            if (mainClass == null || mainClass.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Keine Main-Klasse gefunden!");
                return;
            }

            // **NEUER MODE "internal" für MyOSApp**
            switch (mode.toLowerCase()) {
                case "internal":
                    // Direkt als Internal App starten
                    File jarFile = new File(tempDir, "app.jar");
                    if (jarFile.exists()) {
                        runJarAsInternalApp(jarFile, mainClass);
                    }
                    break;

                case "java":
                    File srcDir = new File(tempDir, "src");
                    File javaFile = new File(srcDir, mainClass + ".java");
                    if (javaFile.exists()) {
                        runJava(javaFile);
                    } else {
                        File jarFile2 = new File(tempDir, "app.jar");
                        if (jarFile2.exists())
                            runJarGui(jarFile2, mainClass);
                    }
                    break;

                case "jar":
                    File jarFile3 = new File(tempDir, "app.jar");
                    if (jarFile3.exists())
                        runJarGui(jarFile3, mainClass);
                    break;

                case "terminal":
                    File jarFile4 = new File(tempDir, "app.jar");
                    if (jarFile4.exists())
                        runJarInTerminal(jarFile4, mainClass);
                    else {
                        srcDir = new File(tempDir, "src");
                        javaFile = new File(srcDir, mainClass + ".java");
                        if (javaFile.exists()) {
                            TerminalApp term = new TerminalApp(srcDir);
                            windowManager.openApp(term);
                            SwingUtilities.invokeLater(() -> {
                                term.getInputField().setText("java " + mainClass);
                                term.getInputField().postActionEvent();
                            });
                        }
                    }
                    break;

                default: // "ask" oder unbekannt
                    // **FÜR .ILLAG: Gleiche Abfrage wie bei JAR**
                    int choice = JOptionPane.showOptionDialog(
                        null,
                        "Wie soll die .illag App gestartet werden?\n\n" +
                        "• Internal App: Läuft im OS-Fenster (MyOSApp API)\n" +
                        "• Extern: Eigener JFrame-Prozess",
                        "Illag-Start-Modus",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Internal App", "Extern (eigenes Fenster)"},
                        "Internal App"
                    );

                    if (choice == JOptionPane.CLOSED_OPTION) return;

                    if (choice == 0) { // Internal App
                        File jarFile5 = new File(tempDir, "app.jar");
                        if (jarFile5.exists()) {
                            runJarAsInternalApp(jarFile5, mainClass);
                        } else {
                            JOptionPane.showMessageDialog(null, "Keine app.jar gefunden für Internal App!");
                        }
                    } else { // Extern
                        int termChoice = JOptionPane.showConfirmDialog(
                            null, "Terminal oder normales Fenster?", "Extern starten",
                            JOptionPane.YES_NO_OPTION);
                        if (termChoice == JOptionPane.YES_OPTION) {
                            File jarFile6 = new File(tempDir, "app.jar");
                            if (jarFile6.exists())
                                runJarInTerminal(jarFile6, mainClass);
                        } else {
                            File jarFile7 = new File(tempDir, "app.jar");
                            if (jarFile7.exists())
                                runJarGui(jarFile7, mainClass);
                        }
                    }
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim Starten der .illag Datei!");
        }
    }

    // In compileJavaToJar() und buildIllagFromJar() - zusätzliche Option hinzufügen:

    // **BEI JAVA → ILLAG:**
    public static void buildIllagFromJava(File javaFile) {
        try {
            String[] options = { "Internal App (MyOSApp)", "GUI", "Terminal" };
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Wie soll die App laufen?\n\n" +
                    "• Internal App: Im OS-Fenster (MyOSApp API)\n" +
                    "• GUI: Externes JFrame-Fenster\n" +
                    "• Terminal: Konsolen-App",
                    "Illag Mode wählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == JOptionPane.CLOSED_OPTION) return;

            String mode = choice == 0 ? "internal" : (choice == 1 ? "java" : "terminal");

            File out = new File(javaFile.getParent(), javaFile.getName().replace(".java", ".illag"));

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
                jos.putNextEntry(new JarEntry("config.properties"));
                String config = "main=" + javaFile.getName().replace(".java", "") + "\nmode=" + mode;
                jos.write(config.getBytes());
                jos.closeEntry();

                jos.putNextEntry(new JarEntry("src/" + javaFile.getName()));
                jos.write(Files.readAllBytes(javaFile.toPath()));
                jos.closeEntry();
            }

            JOptionPane.showMessageDialog(null, ".illag erstellt! Mode: " + mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // **BEI JAR → ILLAG:**
    public static void buildIllagFromJar(File jarFile, String mainClass) {
        try {
            String[] options = { "Internal App (MyOSApp)", "GUI", "Terminal" };
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Wie soll die App laufen?\n\n" +
                    "• Internal App: Im OS-Fenster (MyOSApp API)\n" +
                    "• GUI: Externes JFrame-Fenster\n" +
                    "• Terminal: Konsolen-App",
                    "Illag Mode wählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == JOptionPane.CLOSED_OPTION) return;

            String mode = choice == 0 ? "internal" : (choice == 1 ? "jar" : "terminal");

            File out = new File(jarFile.getParent(), jarFile.getName().replace(".jar", ".illag"));

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
                jos.putNextEntry(new JarEntry("config.properties"));
                String config = "main=" + mainClass + "\nmode=" + mode;
                jos.write(config.getBytes());
                jos.closeEntry();

                jos.putNextEntry(new JarEntry("app.jar"));
                jos.write(Files.readAllBytes(jarFile.toPath()));
                jos.closeEntry();
            }

            JOptionPane.showMessageDialog(null, ".illag erstellt! Mode: " + mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void buildFullProjectJar(File srcDir, File jarFile, String mainClass) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            List<String> files = new ArrayList<>();

            Files.walk(srcDir.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> files.add(p.toString()));

            int result = compiler.run(null, null, null, files.toArray(new String[0]));
            if (result != 0) {
                System.out.println("Compile error!");
                return;
            }

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {

                Files.walk(srcDir.toPath())
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try {
                            String entry = srcDir.toPath().relativize(p).toString().replace("\\", "/");
                            jos.putNextEntry(new JarEntry(entry));
                            jos.write(Files.readAllBytes(p));
                            jos.closeEntry();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
            }

            System.out.println("JAR erstellt: " + jarFile.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static boolean checkSecurity(File f) {
        try {
            Class<?> cls = Class.forName("SecurityManager");
            java.lang.reflect.Method m = cls.getMethod("isAllowed", File.class);
            Object result = m.invoke(null, f);
            return (Boolean) result;
        } catch (ClassNotFoundException e) {
            // Antivirus nicht installiert → alles erlauben
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true; // im Zweifel NICHT blockieren
        }
    }

    public static void executeFile(File f) {

        if (!checkSecurity(f)) {
            JOptionPane.showMessageDialog(null, "Blockiert durch Antivirus!");
            return;
        }

        if (f == null || !f.exists())
            return;

        if (f.isDirectory()) {
            ExplorerApp app = new ExplorerApp();
            app.setPath(f);
            windowManager.openApp(app);

        } else if (f.getName().toLowerCase().endsWith(".jar")) {
            // **NEUE LOGIK: Erst fragen, ob InternalFrame oder extern**
            int choice = JOptionPane.showOptionDialog(
                null,
                "Wie soll die JAR gestartet werden?\n\n" +
                "• Internal App: Läuft im OS-Fenster (MyOSApp API)\n" +
                "• Extern: Eigener JFrame-Prozess",
                "JAR-Start-Modus",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Internal App", "Extern (eigenes Fenster)"},
                "Internal App"
            );

            String mainClass = null;
            if (choice == JOptionPane.CLOSED_OPTION) return;

            // Spezielle JARs
            if (f.getName().equalsIgnoreCase("Calculator.jar")) {
                mainClass = "SuperCalculator";
            }
            if (f.getName().equalsIgnoreCase("TuiCalc.jar")) {
                mainClass = "TuiCalc";
            }

            if (mainClass == null || mainClass.trim().isEmpty()) {
                mainClass = JOptionPane.showInputDialog(
                    null, "Main-Klasse eingeben (z.B. com.example.Main):", "Jar starten", JOptionPane.QUESTION_MESSAGE);
                if (mainClass == null || mainClass.trim().isEmpty()) return;
            }

            if (choice == 0) { // Internal App
                runJarAsInternalApp(f, mainClass);
            } else { // Extern
                int termChoice = JOptionPane.showConfirmDialog(
                    null, "Soll die JAR im Terminal gestartet werden?", "Terminal starten?",
                    JOptionPane.YES_NO_OPTION);
                if (termChoice == JOptionPane.YES_OPTION) {
                    runJarInTerminal(f, mainClass);
                } else {
                    runJarGui(f, mainClass);
                }
            }

        } else if (f.getName().toLowerCase().endsWith(".java")) {
            runJava(f);

        } else if (f.getName().toLowerCase().endsWith(".illag")) {
            runIllag(f);

        } else if (f.getName().toLowerCase().endsWith(".png") ||
                f.getName().toLowerCase().endsWith(".jpg") ||
                f.getName().toLowerCase().endsWith(".jpeg")) {
            windowManager.openApp(new ImageViewer(f));

        } else {
            windowManager.openApp(new TextEditor(f));
        }
    }

    public static void setWallpaper(String path) {
        wallpaperPath = path;
        wallpaper = new ImageIcon(path).getImage();
        desktop.repaint();
        saveSettings();
    }

    public static ImageIcon getIconFor(String title) {
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
                System.err.println("Ungültige Position für " + title + ": " + pos);
                e.printStackTrace();
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

            systemProps.setProperty("customTaskbarColor", String.valueOf(customTaskbarColor));
            systemProps.setProperty("customTextColor", String.valueOf(customTextColor));

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

            // Farben laden
            String bg = systemProps.getProperty("bgColor");
            if (bg != null) currentBg = SettingsApp.hexToColor(bg);

            String tb = systemProps.getProperty("taskbarColor");
            if (tb != null) taskbarColor = SettingsApp.hexToColor(tb);

            String accent = systemProps.getProperty("accentColor");
            if (accent != null) accentColor = SettingsApp.hexToColor(accent);

            String textCol = systemProps.getProperty("textColor");
            if (textCol != null) textColor = SettingsApp.hexToColor(textCol);

            // Shortcuts laden
            String sc = systemProps.getProperty("shortcuts", "");
            customShortcuts.clear();
            if (!sc.isEmpty()) {
                for (String p : sc.split(";")) {
                    if (!p.isEmpty())
                        customShortcuts.add(new File(VM_DIR, p));
                }
            }

            customTaskbarColor = Boolean.parseBoolean(systemProps.getProperty("customTaskbarColor", "false"));
            customTextColor = Boolean.parseBoolean(systemProps.getProperty("customTextColor", "false"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void preloadSettings() {
        try (InputStream in = new FileInputStream("system.cfg")) {
            systemProps.load(in);
        } catch (Exception ignored) {}
}
    public static void main(String[] args) {
        BootManager.boot();
    }
}