package MyOS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Uwe extends JInternalFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private String apiKey; // Nicht mehr final, damit wir ihn nachträglich setzen können
    private final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private List<String> messages = new ArrayList<>();

    public Uwe() {
        // JInternalFrame Setup: Titel, resizable, closable, maximizable, iconifiable
        super("Uwe - Unwissenschaftlicher Chatbot", true, true, true, true);

        // --- NEU: API KEY ABFRAGE ---
        apiKey = System.getenv("Uwe");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = JOptionPane.showInputDialog(
                    null,
                    "Ich konnte keinen API-Key in den Systemvariablen finden.\nBitte gib deinen OpenRouter API-Key ein:",
                    "API-Key benötigt",
                    JOptionPane.QUESTION_MESSAGE
            );

            // Falls der Nutzer auf Abbrechen drückt
            if (apiKey == null) {
                apiKey = ""; 
            }
        }
        // ----------------------------

        File srcFolder = findSrcFolder(new File("."));
        String quellCode = (srcFolder != null) ? scanSourceCode(srcFolder) : "Kein Quellcode gefunden.";

        // --- HIER DEINEN EIGENEN SYSTEM PROMPT ANPASSEN ---
        String meinSystemPrompt = "Du bist Uwe, der ultimative Assistent für Thillagers OS. " +     "Du bist ernst, aber extrem kompetent. " +     "Du duzt den Entwickler und du bist höflich und nett. " +     "Wenn du Code schreibst, erkläre immer kurz, warum du es so gemacht hast. " +     "Du bist im Kontext eines eigenen Betriebssystems (Thillagers OS) angegliedert, das aus einer Reihe von eigenständigen Java‑Applikationen besteht (Browser, TextEditor, Explorer, SystemMonitor, AppStore, Terminal usw.). " + "Antworte immer so verständlich, aber auch kurz wie möglich" + "Deine Antowort sollte nie Markdown enthalten und nutze nur Zeiche, die auf einer standard deutschen Tastatur zu finden sind." 
        + "Analysiere den Code genau, damit du keine falschen informationen über den Code gibst!"  + "Hier ist der aktuelle Code von Thillagers OS, damit du immer den vollen Überblick hast:\n\n";
        // --------------------------------------------------

        // Wir kombinieren deine Anweisungen mit dem Wissen über den Code
        String finalerPrompt = meinSystemPrompt + "\n\nPROJEKT-CODE ZUR INFO:\n" + quellCode;

        messages.add("{\"role\":\"system\",\"content\":\"" + escape(finalerPrompt) + "\"}");

        // GUI Setup für das InternalFrame
        this.setSize(750, 600);
        this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE); // Wichtig für InternalFrames!

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(245, 245, 245));
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        JButton sendButton = new JButton("Senden");
        sendButton.addActionListener(this::sendMessage);
        inputField.addActionListener(this::sendMessage);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        this.add(scrollPane, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);
        this.setVisible(true);

        if (apiKey.isEmpty()) {
            chatArea.append("Uwe: WARNUNG - Kein API-Key eingegeben. Ich werde nicht antworten können!\n\n");
        } else {
            chatArea.append("Uwe: System-Prompt geladen. Was gibt's zu tun?\n\n");
        }
    }

    private File findSrcFolder(File startDir) {
        File current = startDir.getAbsoluteFile();
        while (current != null) {
            File potentialSrc = new File(current, "src");
            if (potentialSrc.exists() && potentialSrc.isDirectory()) return potentialSrc;
            current = current.getParentFile();
        }
        return null;
    }

    private String scanSourceCode(File folder) {
        StringBuilder sb = new StringBuilder();
        File[] list = folder.listFiles();
        if (list == null) return "";
        for (File f : list) {
            if (f.isDirectory()) sb.append(scanSourceCode(f));
            else if (f.getName().endsWith(".java")) {
                try {
                    sb.append("\n--- ").append(f.getName()).append(" ---\n");
                    sb.append(Files.readString(f.toPath())).append("\n");
                } catch (Exception e) {
                    sb.append("Fehler beim Lesen von ").append(f.getName()).append(": ").append(e.getMessage()).append("\n");
                    e.printStackTrace();
                 }
            }
        }
        return sb.toString();
    }

    private void sendMessage(ActionEvent e) {
        String userText = inputField.getText().trim();
        if (userText.isEmpty()) return;

        chatArea.append("Du: " + userText + "\n");
        inputField.setText("");

        // DEBUG-AUSGABE direkt im Chat
        chatArea.append("System: Sende Anfrage an OpenRouter...\n");

        messages.add("{\"role\":\"user\",\"content\":\"" + escape(userText) + "\"}");

        new Thread(() -> {
            try {
                String aiRawResponse = getAIResponse();
                // DEBUG: Zeig uns die rohe Antwort, falls das Parsing schiefgeht
                System.out.println("RAW Response: " + aiRawResponse); 

                String cleanText = parseResponse(aiRawResponse);
                messages.add("{\"role\":\"assistant\",\"content\":\"" + escape(cleanText) + "\"}");

                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Uwe: " + cleanText + "\n\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> chatArea.append("System-Error: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    private String getAIResponse() {
        try {
            StringBuilder msgBuilder = new StringBuilder("[");
            for (int i = 0; i < messages.size(); i++) {
                msgBuilder.append(messages.get(i)).append(i < messages.size() - 1 ? "," : "");
            }
            msgBuilder.append("]");
            String json = "{\"model\":\"nvidia/nemotron-3-nano-30b-a3b:free\",\"messages\":" + msgBuilder.toString() + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey) // Hier wird jetzt die Variable genutzt
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) { 
                                    e.printStackTrace(); // Das zeigt dir im Terminal, was wirklich schiefgeht!
                                    return "{\"choices\":[{\"message\":{\"content\":\"Fehler: " + e.getMessage() + "\"}}]}"; 
    }
    }

    private String parseResponse(String json) {
        try {
            String key = "\"content\":\"";
            int start = json.indexOf(key) + key.length();
            int end = json.indexOf("\"", start);
            while (end != -1 && json.charAt(end - 1) == '\\') end = json.indexOf("\"", end + 1);
            return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Exception e) { return "Parse-Fehler."; }
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Main-Methode zum isolierten Testen des JInternalFrames
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame testFrame = new JFrame("Thillagers OS - Test Umgebung");
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            testFrame.setSize(800, 700);

            JDesktopPane desktopPane = new JDesktopPane();
            testFrame.add(desktopPane);

            Uwe uweApp = new Uwe();
            desktopPane.add(uweApp);

            testFrame.setVisible(true);
        });
    }
}