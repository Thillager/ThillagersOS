package MyOS;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class StartMenu extends JDialog {
  private JTextField searchField;
  private JPanel resultsPanel;
  private List<AppEntry> allApps = new ArrayList<>();
  private Main parent;

  class AppEntry {
      String name;
      Runnable action;
      AppEntry(String n, Runnable a) { name = n; action = a; }
  }

  public StartMenu(Main parent) {
      super(parent);
      this.parent = parent;

      setUndecorated(true);
      setLayout(new BorderLayout());

      // --- Hintergrundfarbe nach Theme ---
      updateBackground();

      getRootPane().setBorder(new LineBorder(Color.GRAY, 2));

      // --- Suchfeld ---
      searchField = new JTextField();
      searchField.setBorder(new TitledBorder(new LineBorder(Color.GRAY), "Suche...", TitledBorder.LEFT, TitledBorder.TOP, null, parent.textColor));
      searchField.setBackground(themeColorBackground());
      searchField.setForeground(parent.textColor);

      // --- Ergebnis-Panel ---
      resultsPanel = new JPanel();
      resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
      resultsPanel.setBackground(themeColorBackground());

      // --- Theme Buttons ---
      JPanel themeBox = new JPanel(new GridLayout(2,2,5,5));
      themeBox.setOpaque(false);
      String[] themes = {"Win95", "Win10", "macOS", "Linux"};
      for(String t : themes) {
          JButton b = new JButton(t);
          b.setBackground(themeColorBackground());
          b.setForeground(parent.textColor);
          b.setFocusPainted(false);
          b.addActionListener(e -> { 
              ThemeManager.applyTheme(t); 
              updateBackground(); 
              dispose(); 
          });
          themeBox.add(b);
      }

      // --- Exit Button ---
      JButton exitBtn = new JButton("System Beenden");
      exitBtn.setBackground(themeColorBackground());
      exitBtn.setForeground(parent.textColor);
      exitBtn.setFocusPainted(false);
      exitBtn.addActionListener(e -> { Main.saveSettings(); System.exit(0); });

      // --- Restart Button ---
      JButton restartBtn = new JButton("Neustarten");
      restartBtn.setBackground(themeColorBackground());
      restartBtn.setForeground(parent.textColor);
      restartBtn.setFocusPainted(false);
      restartBtn.addActionListener(e -> { 
          Main.saveSettings(); 
          try {
              // 1. Neuen Prozess starten
              Runtime.getRuntime().exec("java -jar MyOS.jar");
          } catch (IOException ex) {
              ex.printStackTrace();
          }
          // 2. Jetzt erst den alten Prozess beenden
          System.exit(0); 
      });

      // --- Aufbau ---
      add(searchField, BorderLayout.NORTH);
      add(new JScrollPane(resultsPanel), BorderLayout.CENTER);

      JPanel south = new JPanel(new BorderLayout());
      south.setOpaque(false);
      south.add(themeBox, BorderLayout.CENTER);
      south.add(exitBtn, BorderLayout.SOUTH);
      south.add(restartBtn, BorderLayout.NORTH);
      add(south, BorderLayout.SOUTH);

      // --- Apps laden ---
      loadApps();

      // --- Suche reagieren lassen ---
      searchField.getDocument().addDocumentListener(new DocumentListener() {
          public void insertUpdate(DocumentEvent e) { updateResults(searchField.getText()); }
          public void removeUpdate(DocumentEvent e) { updateResults(searchField.getText()); }
          public void changedUpdate(DocumentEvent e) { updateResults(searchField.getText()); }
      });

      // --- Schließen beim Fokusverlust ---
      addWindowFocusListener(new WindowAdapter() { 
          public void windowLostFocus(WindowEvent e) { dispose(); } 
      });
  }

  // --- Hintergrundfarbe nach Theme ---
  private void updateBackground() {
      Color bg = themeColorBackground();
      getContentPane().setBackground(bg);
      if(resultsPanel != null) resultsPanel.setBackground(bg);
  }

  private Color themeColorBackground() {
      switch(parent.currentTheme) {
          case "Win95": return new Color(192,192,192);
          case "Win10": return new Color(40,40,40);
          case "macOS": return new Color(230,230,230,220);
          case "Linux": return new Color(50,50,50);
          default: return new Color(40,40,40);
      }
  }

  // --- Apps laden ---
  private void loadApps() {
      allApps.clear();
      allApps.add(new AppEntry("Terminal", () -> Main.windowManager.openApp(new TerminalApp(new File(Main.VM_DIR)))));
      allApps.add(new AppEntry("Explorer", () -> Main.windowManager.openApp(new ExplorerApp())));
      allApps.add(new AppEntry("App Store", () -> Main.windowManager.openApp(new AppStore())));
      allApps.add(new AppEntry("Browser", () -> Main.windowManager.openApp(new BrowserApp())));
      allApps.add(new AppEntry("Text Editor", () -> Main.windowManager.openApp(new TextEditor())));
      allApps.add(new AppEntry("Uwe", () -> Main.windowManager.openApp(new Uwe())));
      allApps.add(new AppEntry("System Monitor", () -> Main.windowManager.openApp(new SystemMonitorApp())));
      allApps.add(new AppEntry("Messenger", () -> Main.windowManager.openApp(new Messenger())));
      allApps.add(new AppEntry("Settings", () -> Main.windowManager.openApp(new SettingsApp())));
      allApps.add(new AppEntry("Github", () -> Main.windowManager.openApp(new GithubApp())));
      allApps.add(new AppEntry("TestApp", () -> {
          File f = new File(Main.VM_DIR, "TestApp.jar");
          Main.runJarAsInternalApp(f, "TestApp");
      }));
    

      updateResults("");
  }

  // --- Ergebnisliste aktualisieren ---
  private void updateResults(String q) {
      resultsPanel.removeAll();
      for(AppEntry a : allApps) {
          if(q.isEmpty() || a.name.toLowerCase().contains(q.toLowerCase())) {
              JButton b = new JButton(a.name);
              b.setMaximumSize(new Dimension(300, 40));
              b.setBackground(themeColorBackground());
              b.setForeground(parent.textColor);
              b.setFocusPainted(false);
              b.addActionListener(e -> { a.action.run(); dispose(); });
              resultsPanel.add(b);
          }
      }
      resultsPanel.revalidate(); 
      resultsPanel.repaint();
  }

  public void showAt(int x, int y) {
  pack(); 
  int tbHeight = parent.taskbar.getHeight();

  if(parent.currentTheme.equals("macOS")) {
      y = tbHeight + 5; // Taskbar oben
  } else {
      y = parent.getHeight() - tbHeight - getHeight() - 5; // Taskbar unten
  }

  setLocation(x, y);
  setVisible(true);
  searchField.requestFocus();
}
}