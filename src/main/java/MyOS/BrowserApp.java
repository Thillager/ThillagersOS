package MyOS;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class BrowserApp extends JInternalFrame {

    private JFXPanel jfxPanel;
    private WebEngine engine;
    private JTextField urlField;
    private JButton backBtn, forwardBtn, goBtn, refreshBtn;

    // Liste der Dateiendungen, die als Download erkannt werden sollen
    private static final List<String> DOWNLOADABLE_EXTENSIONS = Arrays.asList(
        ".zip", ".jar", ".illag", ".exe", ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".mp3", ".mp4", ".txt"
    );

    public BrowserApp() {
        super("Web Browser", true, true, true, true);
        setSize(1000, 700);
        setLayout(new BorderLayout());

        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        // GUI-Elemente (Swing)
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        backBtn = new JButton("<-");
        forwardBtn = new JButton("->");
        refreshBtn = new JButton("↻");
        goBtn = new JButton("Go");

        urlField = new JTextField("https://www.google.com");

        buttonPanel.add(backBtn);
        buttonPanel.add(forwardBtn);
        buttonPanel.add(refreshBtn);

        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(goBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Initialisierung von JavaFX (muss auf dem Platform-Thread geschehen)
        Platform.runLater(this::initFX);

        // Button-Aktionen
        goBtn.addActionListener(e -> loadURL(urlField.getText()));
        urlField.addActionListener(e -> loadURL(urlField.getText())); // Ermöglicht das Drücken von "Enter"

        backBtn.addActionListener(e -> Platform.runLater(() -> {
            if (engine.getHistory().getCurrentIndex() > 0) {
                engine.getHistory().go(-1);
            }
        }));

        forwardBtn.addActionListener(e -> Platform.runLater(() -> {
            if (engine.getHistory().getCurrentIndex() < engine.getHistory().getEntries().size() - 1) {
                engine.getHistory().go(1);
            }
        }));

        refreshBtn.addActionListener(e -> Platform.runLater(() -> engine.reload()));
    }

    private void initFX() {
        WebView webView = new WebView();
        engine = webView.getEngine();


        // Listener für URL-Änderungen (Aktualisiert das Textfeld und fängt Downloads ab)
        engine.locationProperty().addListener((observable, oldValue, newValue) -> {
            SwingUtilities.invokeLater(() -> urlField.setText(newValue));

            // SICHERHEIT 2: Lokalen Dateizugriff (file://) komplett blockieren
            if (newValue.toLowerCase().startsWith("file://")) {
                System.out.println("Sicherheit: Lokaler Dateizugriff blockiert -> " + newValue);
                Platform.runLater(() -> engine.load(oldValue != null ? oldValue : "about:blank"));
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Zugriff auf lokale Dateien ist aus Sicherheitsgründen gesperrt.", "Sicherheitswarnung", JOptionPane.WARNING_MESSAGE)
                );
                return;
            }

            checkAndDownload(newValue);
        });

        // Listener, um DOM-Klicks abzufangen (für direkte Links auf Dateien)
        engine.getLoadWorker().stateProperty().addListener(
            (ObservableValue<? extends Worker.State> ov, Worker.State oldState, Worker.State newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    EventListener listener = ev -> {
                        String href = ((Element) ev.getTarget()).getAttribute("href");
                        if (href != null && !href.isEmpty()) {
                            if (!href.startsWith("http")) {
                                try {
                                    URL base = new URL(engine.getLocation());
                                    href = new URL(base, href).toString();
                                } catch (Exception ignored) {}
                            }

                            if (isDownloadable(href)) {
                                ev.preventDefault(); // Navigation im WebView stoppen
                                String finalHref = href;
                                SwingUtilities.invokeLater(() -> startDownload(finalHref));
                            }
                        }
                    };

                    Document doc = engine.getDocument();
                    if (doc != null) {
                        NodeList links = doc.getElementsByTagName("a");
                        for (int i = 0; i < links.getLength(); i++) {
                            ((EventTarget) links.item(i)).addEventListener("click", listener, false);
                        }
                    }
                }
            });

        Scene scene = new Scene(webView);
        jfxPanel.setScene(scene);

        // Startseite laden
        engine.load(urlField.getText());
    }

    private void loadURL(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        final String finalUrl = url;
        Platform.runLater(() -> engine.load(finalUrl));
    }

    private boolean isDownloadable(String url) {
        String lowerUrl = url.toLowerCase();
        for (String ext : DOWNLOADABLE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext) || lowerUrl.contains(ext + "?")) {
                return true;
            }
        }
        return false;
    }

    private void checkAndDownload(String url) {
        if (isDownloadable(url)) {
            Platform.runLater(() -> {
                if (engine.getHistory().getCurrentIndex() > 0) {
                    engine.getHistory().go(-1); // Seite zurückspringen, damit man nicht auf einer weißen Seite festhängt
                } else {
                    engine.load("about:blank");
                }
            });
            startDownload(url);
        }
    }

    private void startDownload(String fileUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                int responseCode = httpConn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String fileName = "";
                    String disposition = httpConn.getHeaderField("Content-Disposition");

                    if (disposition != null) {
                        int index = disposition.indexOf("filename=");
                        if (index > 0) {
                            fileName = disposition.substring(index + 10, disposition.length() - 1);
                        }
                    } else {
                        fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                        if (fileName.contains("?")) {
                            fileName = fileName.substring(0, fileName.indexOf("?"));
                        }
                    }

                    // SICHERHEIT 3: Dateinamen bereinigen, um Path-Traversal ("../../") zu verhindern
                    fileName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_"); 
                    if (fileName.isEmpty()) {
                        fileName = "download_" + System.currentTimeMillis();
                    }

                    // SICHERHEIT 4: Zielort zwingend auf VM_Disk festlegen
                    File vmDir = new File(Main.VM_DIR);
                    if (!vmDir.exists()) vmDir.mkdir();

                    File saveFilePath = new File(vmDir, fileName);

                    // SICHERHEIT 5: Dateikonflikte verhindern (Keine Dateien überschreiben)
                    int counter = 1;
                    while (saveFilePath.exists()) {
                        int dotIndex = fileName.lastIndexOf(".");
                        if (dotIndex > 0) {
                            saveFilePath = new File(vmDir, fileName.substring(0, dotIndex) + "_" + counter + fileName.substring(dotIndex));
                        } else {
                            saveFilePath = new File(vmDir, fileName + "_" + counter);
                        }
                        counter++;
                    }

                    File finalPath = saveFilePath;
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Download gestartet:\n" + finalPath.getName(), "Download", JOptionPane.INFORMATION_MESSAGE));

                    InputStream inputStream = httpConn.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(finalPath);

                    int bytesRead;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Download erfolgreich!\nGespeichert in: " + finalPath.getAbsolutePath(), "Download", JOptionPane.INFORMATION_MESSAGE);
                        Main.instance.desktop.repaint(); // Evtl. Explorer updaten, falls offen
                    });

                } else {
                    System.out.println("Fehler beim Herunterladen. Server meldet HTTP Code: " + responseCode);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Download fehlgeschlagen! HTTP Code: " + responseCode, "Fehler", JOptionPane.ERROR_MESSAGE));
                }
                httpConn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Download-Fehler: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
}