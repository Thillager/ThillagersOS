package MyOS;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class BrowserApp extends JInternalFrame {

    private JFXPanel jfxPanel;
    private WebEngine engine;
    private JTextField urlField;

    private final Map<String, SitePolicy> policies = new HashMap<>();

    private static final int MAX_PAGE_SIZE = 5_000_000; // 5MB
    private static final int MAX_DOWNLOAD_SIZE = 50_000_000;

    enum SitePolicy {
        BLOCKED,
        LIMITED,
        TRUSTED
    }

    public BrowserApp() {
        super("Hardened Browser", true, true, true, true);
        setSize(1000, 700);
        setLayout(new BorderLayout());

        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        JTextField field = new JTextField("https://duckduckgo.com");
        urlField = field;

        JButton go = new JButton("Go");
        top.add(field, BorderLayout.CENTER);
        top.add(go, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        go.addActionListener(e -> navigate(urlField.getText()));
        field.addActionListener(e -> navigate(urlField.getText()));

        Platform.runLater(this::initFX);
    }

    private void initFX() {
        WebView view = new WebView();
        engine = view.getEngine();

        // 🔒 Navigation Guard
        engine.locationProperty().addListener((obs, o, n) -> {
            if (n == null) return;

            if (n.startsWith("file://")) {
                block("Lokale Dateien blockiert");
                Platform.runLater(() -> engine.load("about:blank"));
            }

            if (!n.startsWith("https://")) {
                block("Nur HTTPS erlaubt");
                Platform.runLater(() -> engine.load("about:blank"));
            }
        });

        jfxPanel.setScene(new Scene(view));
    }

    // =============================
    // 🚀 NAVIGATION ENTRY
    // =============================

    private void navigate(String url) {
        try {
            if (!url.startsWith("http")) url = "https://" + url;

            URL u = new URL(url);
            String host = u.getHost();

            SitePolicy policy = policies.get(host);

            if (policy == null) {
                policy = askPolicy(host);
                policies.put(host, policy);
            }

            if (policy == SitePolicy.BLOCKED) {
                block("Seite blockiert");
                return;
            }

            loadPage(url, policy);

        } catch (Exception e) {
            block("Ungültige URL");
        }
    }

    // =============================
    // 🔐 USER POLICY
    // =============================

    private SitePolicy askPolicy(String host) {
        Object[] options = {"Blockieren", "Eingeschränkt", "Vertrauen"};
        int res = JOptionPane.showOptionDialog(
                this,
                "Website: " + host,
                "Sicherheitsstufe wählen",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]
        );

        if (res == 0) return SitePolicy.BLOCKED;
        if (res == 2) return SitePolicy.TRUSTED;
        return SitePolicy.LIMITED;
    }

    // =============================
    // 🌐 LOAD PAGE
    // =============================

    private void loadPage(String url, SitePolicy policy) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getContentLength() > MAX_PAGE_SIZE)
                    throw new Exception("Seite zu groß");

                String html = new String(conn.getInputStream().readAllBytes());

                html = sanitize(html, policy);

                if (policy != SitePolicy.TRUSTED)
                    html = injectSandbox(html);

                String finalHtml = html;

                Platform.runLater(() -> {
                    engine.setJavaScriptEnabled(policy != SitePolicy.LIMITED);
                    engine.loadContent(finalHtml);
                });

            } catch (Exception e) {
                e.printStackTrace();
                block("Fehler beim Laden");
            }
        }).start();
    }

    // =============================
    // 🧹 SANITIZE
    // =============================

    private String sanitize(String html, SitePolicy policy) {

        // entferne iframes
        html = html.replaceAll("(?i)<iframe.*?>.*?</iframe>", "");

        // entferne externe scripts bei LIMITED
        if (policy == SitePolicy.LIMITED) {
            html = html.replaceAll("(?i)<script.*?>.*?</script>", "");
        }

        // entferne event handler
        html = html.replaceAll("on\\w+\\s*=\\s*\"[^\"]*\"", "");

        return html;
    }

    // =============================
    // 🧠 JS SANDBOX
    // =============================

    private String injectSandbox(String html) {
        String js = """
        <script>
        window.open = function(){return null;}
        window.close = function(){};
        document.cookie = "";

        fetch = function(){throw "blocked";};
        XMLHttpRequest = function(){throw "blocked";};
        WebSocket = function(){throw "blocked";};

        navigator.geolocation = undefined;
        Notification = undefined;

        console.log("Sandbox aktiv");
        </script>
        """;

        return html.replace("<head>", "<head>" + js);
    }

    // =============================
    // 📥 DOWNLOAD
    // =============================

    private void download(String url) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

                if (conn.getContentLength() > MAX_DOWNLOAD_SIZE)
                    throw new Exception("Datei zu groß");

                String file = url.substring(url.lastIndexOf("/") + 1);
                file = file.replaceAll("[^a-zA-Z0-9._-]", "_");

                File out = new File(Main.VM_DIR, file);

                InputStream in = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(out);

                byte[] buf = new byte[4096];
                int r;

                while ((r = in.read(buf)) != -1)
                    fos.write(buf, 0, r);

                fos.close();
                in.close();

                msg("Download fertig");

            } catch (Exception e) {
                msg("Download Fehler");
            }
        }).start();
    }

    // =============================
    // 🧰 UI
    // =============================

    private void block(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Blockiert", JOptionPane.WARNING_MESSAGE));
    }

    private void msg(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg));
    }
}