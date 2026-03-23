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

public class TerminalApp extends JInternalFrame {
  private JTextArea area;
  private JTextField input;
  private Process process;
  private BufferedWriter writer;
  private String lastCommand = "";

  public TerminalApp(File dir) {
      super("Terminal", true, true, true, true); // WICHTIG: Titel + Resizable, Closable, Maximizable, Iconifiable
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

      // Im Konstruktor von TerminalApp:
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

      // WICHTIG: Im ActionListener (wenn Enter gedrückt wird) den Befehl speichern
      input.addActionListener(e -> {
          String text = input.getText();
          if (!text.isEmpty()) {
              lastCommand = text; // Hier wird der Befehl für "Pfeil-hoch" gemerkt
          }
          try {
              writer.write(text);
              writer.newLine();
              writer.flush();
              input.setText("");
          } catch (Exception ex) {
              area.append("Fehler beim Senden\n");
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
      } catch (Exception e) {}
  }
}


