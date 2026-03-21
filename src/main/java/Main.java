import javax.swing.*;
    import javax.swing.border.*;
    import javax.swing.text.*;
    import javax.swing.event.DocumentEvent;
    import javax.swing.event.DocumentListener;
    import javax.swing.tree.*;
    import java.awt.*;
    import java.awt.event.*;
    import java.io.*;
    import java.net.*;
    import java.nio.file.*;
    import java.text.SimpleDateFormat;
    import java.util.*;
    import java.util.List;
    import java.util.jar.*;
    import javax.tools.*;
    import java.awt.dnd.DropTarget;
    import java.awt.dnd.DropTargetDropEvent;
    import java.awt.dnd.DnDConstants;
    import java.awt.datatransfer.DataFlavor;

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
        public static JPanel desktop; 
        private static JLayeredPane lp;
        public static final String VM_DIR = "VM_Disk";
        private static int windowOffset = 0; 
        private static Map<JFrame, JButton> windowToButtonMap = new HashMap<>();
        private static JPanel taskbar;
        private StartMenu customStartMenu;

        public static Properties systemProps = new Properties();
        public static long timeOffsetMillis = 0;
        public static List<File> customShortcuts = new ArrayList<>();

        public Main() {
            File vmDir = new File(VM_DIR);
            if(!vmDir.exists()) vmDir.mkdir();

            setTitle("Thillagers OS - Ultimate Complete Edition");
            setUndecorated(true);
            Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
            setSize(sz.width, sz.height);
            setLocation(0, 0);
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            lp = new JLayeredPane();
            setContentPane(lp);
            lp.setLayout(null);

            desktop = new JPanel(null) {
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
            desktop.setBounds(0, 0, sz.width, sz.height);
            lp.add(desktop, JLayeredPane.DEFAULT_LAYER);

            taskbar = new JPanel(new BorderLayout());
            taskbar.setBounds(0, sz.height - 55, sz.width, 55);
            taskbar.setBackground(taskbarColor);
            lp.add(taskbar, JLayeredPane.PALETTE_LAYER);

            JButton startBtn = new JButton("START");
            startBtn.setFocusPainted(false);
            startBtn.addActionListener(e -> {
                if (customStartMenu == null || !customStartMenu.isVisible()) {
                    customStartMenu = new StartMenu(this);
                    int menuY = (currentTheme.equals("macOS")) ? 50 : (getHeight() - 505);
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
                        } catch (Exception ex) {}
                    }
                }
            });
            new javax.swing.Timer(1000, e -> timeLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis() + timeOffsetMillis)))).start();
            taskbar.add(timeLabel, BorderLayout.EAST);

            loadSettings();
            applyTheme(currentTheme);
            if(!wallpaperPath.isEmpty()) setWallpaper(wallpaperPath);
        }

        public static ImageIcon loadIcon(String name) {
            try {
                return new ImageIcon(Main.class.getResource("/resources/" + name));
            } catch (Exception e) {
                return null;
            }
        }

        public void applyTheme(String theme) {
            currentTheme = theme;
            switch(theme) {
                case "Win95":
                    currentBg = new Color(0, 128, 128);
                    taskbarColor = new Color(192, 192, 192);
                    textColor = Color.BLACK;
                    taskbar.setBounds(0, getHeight() - 40, getWidth(), 40);
                    break;
                case "macOS":
                    currentBg = new Color(220, 220, 220);
                    taskbarColor = new Color(255, 255, 255, 220);
                    textColor = Color.BLACK;
                    taskbar.setBounds(0, 0, getWidth(), 35);
                    break;
                case "Linux":
                    currentBg = new Color(48, 10, 36);
                    taskbarColor = new Color(30, 30, 30);
                    textColor = Color.WHITE;
                    taskbar.setBounds(0, getHeight() - 50, getWidth(), 50);
                    break;
                default: // Win10
                    currentBg = new Color(30, 30, 30);
                    taskbarColor = new Color(20, 20, 20);
                    textColor = Color.WHITE;
                    taskbar.setBounds(0, getHeight() - 55, getWidth(), 55);
                    break;
            }
            taskbar.setBackground(taskbarColor);
            initDesktopIcons(getHeight());
            saveSettings();
        }

        private void initDesktopIcons(int screenHeight) {
            desktop.removeAll();
            int yOff = (currentTheme.equals("macOS")) ? 50 : 40;
            addDesktopIcon("Terminal", 40, yOff, e -> openApp(new TerminalApp(new File(VM_DIR))));
            addDesktopIcon("Explorer", 40, yOff + 120, e -> openApp(new ExplorerApp()));
            addDesktopIcon("App Store", 40, yOff + 240, e -> openApp(new AppStore()));
            addDesktopIcon("Browser", 40, yOff + 480, e -> openApp(new BrowserApp()));

            File dir = new File(VM_DIR);
            File[] files = dir.listFiles();
            int startX = 160;
            int startY = yOff;
            if(files != null) {
                Arrays.sort(files);
                for(File f : files) {
                    if(f.getName().endsWith(".jar")) {
                        addDesktopIcon(f.getName(), startX, startY, e -> runJar(f));
                        startY += 120;
                        if(startY > screenHeight - 200) { startY = yOff; startX += 120; }
                    }
                }
            }

            for(File f : customShortcuts) {
                if(f.exists()) {
                    addDesktopIcon(f.getName(), startX, startY, e -> executeFile(f));
                    startY += 120;
                    if(startY > screenHeight - 200) { startY = yOff; startX += 120; }
                }
            }

            desktop.revalidate();
            desktop.repaint();
        }

        public static void runJar(File f) {
            try {
                Runtime.getRuntime().exec(new String[]{"java", "-jar", f.getAbsolutePath()});
            } catch(Exception e) {
                JOptionPane.showMessageDialog(null, "Startfehler: " + f.getName());
            }
        }

        public static void executeFile(File f) {
            if(f == null || !f.exists()) return;
            if(f.isDirectory()) { 
                ExplorerApp app = new ExplorerApp();
                app.setPath(f);
                openApp(app); 
            }
            else if(f.getName().endsWith(".jar")) runJar(f);
            else if(f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".jpg")) openApp(new ImageViewer(f));
            else openApp(new TextEditor(f));
        }

        public static void setWallpaper(String path) {
            wallpaperPath = path;
            wallpaper = new ImageIcon(path).getImage();
            desktop.repaint();
            saveSettings();
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
                } catch(Exception e){}
            }
            p.setBounds(x, y, 100, 100);

            String labelText = title.endsWith(".jar") ? "[JAR]" : (title.endsWith(".java") ? "[JAVA]" : (title.contains(".") ? "[FILE]" : "[DIR]"));
            if(title.equals("Terminal") || title.equals("Explorer") || title.equals("App Store") || title.equals("Browser")) labelText = "[APP]";

            JLabel img = new JLabel(labelText, SwingConstants.CENTER);
            img.setFont(new Font("Monospaced", Font.BOLD, 18));
            img.setForeground(textColor);
            img.setBorder(new LineBorder(textColor, 1));

            MouseAdapter ma = new MouseAdapter() {
                int pX, pY;
                public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && action != null) action.actionPerformed(null); }
                public void mousePressed(MouseEvent e) { pX = e.getX(); pY = e.getY(); }
                public void mouseDragged(MouseEvent e) { 
                    Point pLoc = p.getLocation();
                    p.setLocation(pLoc.x + e.getX() - pX, pLoc.y + e.getY() - pY); 
                    desktop.repaint(); 
                }
                public void mouseReleased(MouseEvent e) { saveSettings(); }
            };

            img.addMouseListener(ma);
            img.addMouseMotionListener(ma);

            JButton btn = new JButton(title);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
            btn.addActionListener(action);

            p.add(img, BorderLayout.CENTER); 
            p.add(btn, BorderLayout.SOUTH);
            desktop.add(p);
        }

        public static void openApp(JFrame app) {
            app.setLocation(100 + windowOffset, 80 + windowOffset);
            windowOffset = (windowOffset + 30) % 300;
            app.setVisible(true);
            app.toFront();

            JButton tBtn = new JButton(app.getTitle());
            tBtn.addActionListener(e -> { app.setVisible(true); app.toFront(); });
            taskIconsPanel.add(tBtn);
            windowToButtonMap.put(app, tBtn);
            taskIconsPanel.revalidate();

            app.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    taskIconsPanel.remove(windowToButtonMap.get(app));
                    taskIconsPanel.revalidate();
                    taskIconsPanel.repaint();
                }
            });
        }

        public static void saveSettings() {
            try (OutputStream out = new FileOutputStream("system.cfg")) {
                systemProps.setProperty("theme", currentTheme);
                systemProps.setProperty("wallpaper", wallpaperPath);
                systemProps.setProperty("timeOffset", String.valueOf(timeOffsetMillis));

                if (desktop != null) {
                    for (Component c : desktop.getComponents()) {
                        if (c.getName() != null && c.getName().startsWith("ICON_")) {
                            systemProps.setProperty("pos_" + c.getName(), c.getX() + "," + c.getY());
                        }
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (File f : customShortcuts) {
                    sb.append(f.getAbsolutePath()).append(";");
                }
                systemProps.setProperty("shortcuts", sb.toString());

                systemProps.store(out, "System Settings");
            } catch (IOException e) {}
        }

        private void loadSettings() {
            try (InputStream in = new FileInputStream("system.cfg")) {
                systemProps.load(in);
                currentTheme = systemProps.getProperty("theme", "Win10");
                wallpaperPath = systemProps.getProperty("wallpaper", "");
                timeOffsetMillis = Long.parseLong(systemProps.getProperty("timeOffset", "0"));

                String sc = systemProps.getProperty("shortcuts", "");
                customShortcuts.clear();
                if (!sc.isEmpty()) {
                    for (String p : sc.split(";")) {
                        if (!p.isEmpty()) customShortcuts.add(new File(p));
                    }
                }
            } catch (Exception e) {}
        }

        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> new Main().setVisible(true));
        }
    }

    // ================= STARTMENÜ =================
    class StartMenu extends JDialog {
        private JTextField searchField;
        private JPanel resultsPanel;
        private List<AppEntry> allApps = new ArrayList<>();

        class AppEntry {
            String name; Runnable action;
            AppEntry(String n, Runnable a) { name = n; action = a; }
        }

        public StartMenu(Main parent) {
            super(parent);
            setUndecorated(true);
            setSize(300, 450);
            setLayout(new BorderLayout());
            getContentPane().setBackground(new Color(40,40,40));
            getRootPane().setBorder(new LineBorder(Color.GRAY, 2));

            searchField = new JTextField();
            searchField.setBackground(new Color(60,60,60));
            searchField.setForeground(Color.WHITE);
            searchField.setBorder(new TitledBorder(new LineBorder(Color.GRAY), "Suche...", TitledBorder.LEFT, TitledBorder.TOP, null, Color.WHITE));

            resultsPanel = new JPanel();
            resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
            resultsPanel.setBackground(new Color(40,40,40));

            JPanel themeBox = new JPanel(new GridLayout(2,2));
            String[] themes = {"Win95", "Win10", "macOS", "Linux"};
            for(String t : themes) {
                JButton b = new JButton(t);
                b.addActionListener(e -> { parent.applyTheme(t); dispose(); });
                themeBox.add(b);
            }

            JButton exitBtn = new JButton("System Beenden");
            exitBtn.addActionListener(e -> { Main.saveSettings(); System.exit(0); });

            add(searchField, BorderLayout.NORTH);
            add(new JScrollPane(resultsPanel), BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout());
            south.add(themeBox, BorderLayout.CENTER);
            south.add(exitBtn, BorderLayout.SOUTH);
            add(south, BorderLayout.SOUTH);

            allApps.add(new AppEntry("Terminal", () -> Main.openApp(new TerminalApp(new File(Main.VM_DIR)))));
            allApps.add(new AppEntry("Explorer", () -> Main.openApp(new ExplorerApp())));
            allApps.add(new AppEntry("App Store", () -> Main.openApp(new AppStore())));
            allApps.add(new AppEntry("Browser", () -> Main.openApp(new BrowserApp())));
            allApps.add(new AppEntry("Text Editor", () -> Main.openApp(new TextEditor())));

            File dir = new File(Main.VM_DIR);
            File[] files = dir.listFiles();
            if(files != null) for(File f : files) if(f.getName().endsWith(".jar")) allApps.add(new AppEntry(f.getName(), () -> Main.runJar(f)));

            updateResults("");
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateResults(searchField.getText()); }
                public void removeUpdate(DocumentEvent e) { updateResults(searchField.getText()); }
                public void changedUpdate(DocumentEvent e) { updateResults(searchField.getText()); }
            });
            addWindowFocusListener(new WindowAdapter() { public void windowLostFocus(WindowEvent e) { dispose(); } });
        }

        private void updateResults(String q) {
            resultsPanel.removeAll();
            for(AppEntry a : allApps) {
                if(q.isEmpty() || a.name.toLowerCase().contains(q.toLowerCase())) {
                    JButton b = new JButton(a.name);
                    b.setMaximumSize(new Dimension(300, 40));
                    b.addActionListener(e -> { a.action.run(); dispose(); });
                    resultsPanel.add(b);
                }
            }
            resultsPanel.revalidate(); resultsPanel.repaint();
        }
        public void showAt(int x, int y) { setLocation(x,y); setVisible(true); searchField.requestFocus(); }
    }

    // ================= EXPLORER =================
    class ExplorerApp extends JFrame {
        private DefaultListModel<File> model = new DefaultListModel<>();
        private JList<File> list = new JList<>(model);
        private File currentPath = new File(Main.VM_DIR);
        private static File fileClipboard = null;
        private static boolean isCutOperation = false;

        public ExplorerApp() {
            setTitle("File Explorer");
            setSize(700, 500);
            setLayout(new BorderLayout());

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

                    menu.add(new JMenuItem(new AbstractAction("Loeschen") {
                        public void actionPerformed(ActionEvent e) { if(f.delete()) refresh(); }
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
    }
    // ================= APP STORE =================
    class AppStore extends JFrame {
        public AppStore() {
            setTitle("App Store"); setSize(450, 550); setLayout(new BorderLayout());
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            addAppEntry(listPanel, "Snake Game", "https://example.com/snake.jar");
            add(new JScrollPane(listPanel), BorderLayout.CENTER);
            JButton custom = new JButton("URL installieren");
            custom.addActionListener(e -> {
                String url = JOptionPane.showInputDialog("Link:");
                if(url != null) installApp(url.substring(url.lastIndexOf("/") + 1), url);
            });
            add(custom, BorderLayout.SOUTH);
        }
        private void addAppEntry(JPanel p, String name, String url) {
            JPanel row = new JPanel(new BorderLayout());
            row.add(new JLabel(name), BorderLayout.CENTER);
            JButton btn = new JButton("Install");
            btn.addActionListener(e -> installApp(name.replace(" ", "") + ".jar", url));
            row.add(btn, BorderLayout.EAST);
            p.add(row);
        }
        private void installApp(String fileName, String urlStr) {
            new Thread(() -> {
                try {
                    URL url = new URL(urlStr);
                    Files.copy(url.openStream(), new File(Main.VM_DIR, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Installiert!"));
                } catch(Exception ex) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Fehler")); }
            }).start();
        }
    }

    // ================= BROWSER =================
    class BrowserApp extends JFrame {
        public BrowserApp() {
            setTitle("Web Browser"); setSize(600, 200); setLayout(new FlowLayout());
            JTextField urlField = new JTextField("https://", 35);
            JButton goBtn = new JButton("[GO]");
            JButton dlBtn = new JButton("[DL]");
            JProgressBar progress = new JProgressBar(0, 100);
            goBtn.addActionListener(e -> { try { Desktop.getDesktop().browse(new URI(urlField.getText())); } catch(Exception ex){} });
            dlBtn.addActionListener(e -> {
                progress.setValue(10);
                new Thread(() -> {
                    try {
                        URL url = new URL(urlField.getText());
                        String n = urlField.getText().substring(urlField.getText().lastIndexOf('/') + 1);
                        if(n.isEmpty()) n = "dl_" + System.currentTimeMillis();
                        Files.copy(url.openStream(), new File(Main.VM_DIR, n).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        SwingUtilities.invokeLater(() -> { progress.setValue(100); JOptionPane.showMessageDialog(this, "Fertig!"); });
                    } catch(Exception ex) { SwingUtilities.invokeLater(() -> progress.setValue(0)); }
                }).start();
            });
            add(urlField); add(goBtn); add(dlBtn); add(progress);
        }
    }

    // ================= TOOLS =================
class TerminalApp extends JFrame {
    private JTextArea area;
    private JTextField input;
    private Process process;
    private BufferedWriter writer;

    public TerminalApp(File dir) {
        setTitle("Terminal"); 
        setSize(600, 400);

        area = new JTextArea();
        area.setBackground(Color.BLACK);
        area.setForeground(Color.GREEN);
        area.setEditable(false);

        input = new JTextField();
        input.setBackground(Color.BLACK);
        input.setForeground(Color.WHITE);

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd");
            } else {
                pb = new ProcessBuilder("/bin/bash");
            }

            pb.directory(new File(Main.VM_DIR));
            process = pb.start();

            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            new Thread(() -> readStream(process.getInputStream())).start();
            new Thread(() -> readStream(process.getErrorStream())).start();

        } catch (Exception e) {
            area.append("Fehler: " + e.getMessage());
        }

        input.addActionListener(e -> {
            try {
                writer.write(input.getText());
                writer.newLine();
                writer.flush();
                input.setText("");
            } catch (Exception ex) {
                area.append("Fehler beim Senden\n");
            }
        });
    }

    private void readStream(InputStream is) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                String l = line;
                SwingUtilities.invokeLater(() -> area.append(l + "\n"));
            }
        } catch (Exception e) {}
    }
}    
    class ImageViewer extends JFrame {
        public ImageViewer(File f) {
            setTitle("Image: " + f.getName()); setSize(600, 600);
            add(new JScrollPane(new JLabel(new ImageIcon(f.getAbsolutePath()))));
        }
    }

    // ================= TEXT EDITOR (ENHANCED) =================
    class TextEditor extends JFrame {
        private JTextArea area;
        private File currentFile;

        public TextEditor() { this(null); }
        public TextEditor(File f) {
            this.currentFile = f;
            setTitle("Advanced Editor");
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
                setTitle("Editor: " + f.getName());
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
                setTitle("Editor: " + currentFile.getName());
                JOptionPane.showMessageDialog(this, "Gespeichert!");
            } catch(Exception ex){ JOptionPane.showMessageDialog(this, "Fehler beim Speichern."); }
        }
    }