package MyOS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class TerminalApp extends JInternalFrame {
  private JTextArea area;
  private JTextField input;
  private Process process;
  private BufferedWriter writer;
  private String lastCommand = "";
    private File currentDir;

  public TerminalApp(File dir) {
      super("Terminal", true, true, true, true); // WICHTIG: Titel + Resizable, Closable, Maximizable, Iconifiable
      this.currentDir = dir;
      setSize(600, 400);

      area = new JTextArea();
      area.setBackground(Color.BLACK);
      area.setForeground(Color.GREEN);
      area.setEditable(false);
      // In den TerminalApp Konstruktor:
      area.setFont(new Font("Monospaced", Font.PLAIN, 14));
      area.setLineWrap(false);

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

          pb.directory(dir); // Nutze übergebenes Verzeichnis
          process = pb.start();

          writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

          new Thread(() -> readStream(process.getInputStream())).start();
          new Thread(() -> readStream(process.getErrorStream())).start();

      } catch (Exception e) {
          area.append("Fehler: " + e.getMessage());
            e.printStackTrace();
      }

      input.addKeyListener(new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
              if (e.getKeyCode() == KeyEvent.VK_UP) {
                  // Wenn Pfeil-hoch gedrückt wird: Letzten Befehl ins Feld setzen
                  input.setText(lastCommand);
              } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                  // Optional: Pfeil-runter macht das Feld leer
                  input.setText("");
              }
          }
      });

      input.addActionListener(e -> {
          String text = input.getText().trim();
          if (text.isEmpty()) return;

          lastCommand = text; // Befehl für "Pfeil-hoch" merken

          // --- START CUSTOM COMMANDS ---
          if (handleCustomCommands(text)) {
              input.setText("");
              return; // Wenn es ein eigener Befehl war, hier abbrechen
          }
          // --- ENDE CUSTOM COMMANDS ---

          // Normaler System-Befehl: An CMD/Bash weiterleiten
          try {
              writer.write(text);
              writer.newLine();
              writer.flush();
              input.setText("");
          } catch (Exception ex) {
              area.append("Fehler beim Senden: " + ex.getMessage() + "\n");
                ex.printStackTrace();
          }
      });
      setVisible(true); // WICHTIG: sonst wird Fenster nicht angezeigt
  }

  public JTextField getInputField() {
  return input;
}

  void readStream(InputStream is) {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
          String line;
          while ((line = br.readLine()) != null) {
              String l = line;
              SwingUtilities.invokeLater(() -> area.append(l + "\n"));
          }
      } catch (Exception e) {
            SwingUtilities.invokeLater(() -> area.append("Fehler beim Lesen: " + e.getMessage() + "\n"));
                e.printStackTrace();
      }
  }

    private boolean handleCustomCommands(String command) {
        // In Kleinschreibung umwandeln für einfachere Prüfung
        String lowerCmd = command.toLowerCase();

        if (lowerCmd.equals("hello")) {
            area.append("> MyOS: Hallo! Ich bin dein Custom-Terminal.\n");
            return true; // "true" sagt der App: Ich habe den Befehl verarbeitet.
        }

        if (lowerCmd.equals("clear") || lowerCmd.equals("cls")) {
            area.setText(""); // Terminal leeren
            return true;
        }

        if (lowerCmd.equals("version")) {
            area.append("> Thillagers OS v1.0.1 (Build 2026)\n");
            return true;
        }

        if (lowerCmd.startsWith("msg ")) {
            String msg = command.substring(4);
            JOptionPane.showMessageDialog(this, "Nachricht vom Terminal: " + msg);
            return true;
        }

        if (lowerCmd.equals("help")) {
            area.append("""
                > MyOS Custom-Befehle:
                > hello       - Grüßt dich
                > clear/cls   - Terminal leeren
                > version     - OS-Version anzeigen
                > msg [Text]  - Nachricht anzeigen
                > help        - Diese Hilfe
                """);
            return true;
        }

        if (lowerCmd.equals("explorer")) {
            SwingUtilities.invokeLater(() -> 
            Main.windowManager.openApp(new ExplorerApp()));
            return true;
        }

        if (lowerCmd.equals("terminal")) {
            SwingUtilities.invokeLater(() -> 
            Main.windowManager.openApp(new TerminalApp(new File(Main.VM_DIR))));
            return true;
        }

        if (lowerCmd.equals("browser")) {
            SwingUtilities.invokeLater(() ->
            Main.windowManager.openApp(new BrowserApp()));
            return true;
        }

        if (lowerCmd.equals("app store")) {
            SwingUtilities.invokeLater(() ->
            Main.windowManager.openApp(new AppStore()));
            return true;
        }

        if (lowerCmd.equals("text editor")) {
            SwingUtilities.invokeLater(() ->
            Main.windowManager.openApp(new TextEditor()));
            return true;
        }

        if (lowerCmd.equals("exit")) {
            dispose(); // Terminal schließen
            return true;
        }

        if (lowerCmd.equals("shutdown")) {
            Main.saveSettings();
            System.exit(0);
            return true;
        }

        if (lowerCmd.equals("where i?") || lowerCmd.equals("pwd") || lowerCmd.equals("where i")) {
            //getAbsolutePath() gibt den vollen Pfad des Ordners zurück
            area.append("Du bist in " + currentDir.getAbsolutePath() + "\n");
            return true;
        }

        if (lowerCmd.equals("uwe")) {
            SwingUtilities.invokeLater(() ->
            Main.windowManager.openApp(new Uwe()));
        }


        if (lowerCmd.startsWith("cd ")) {
            String path = command.substring(3).trim();

            File newDir;

            if (path.equals("..")) {
                newDir = currentDir.getParentFile();
            } else {
                newDir = new File(currentDir, path);
            }

            if (newDir != null && newDir.exists() && newDir.isDirectory()) {
                currentDir = newDir;
                area.append("Wechsel zu: " + currentDir.getAbsolutePath() + "\n");
            } else {
                area.append("Ordner nicht gefunden!\n");
            }

            return true;
        }

        // Wenn kein Treffer dabei war, false zurückgeben
        return false;
    }

    
}


