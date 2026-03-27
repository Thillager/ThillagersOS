package MyOS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Stack;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class GithubApp extends JInternalFrame {

    private JTextField searchField;
    private DefaultListModel<String> listModel;
    private JList<String> listView;
    private JLabel statusLabel;
    private JButton backButton;

    private Stack<String> navigationStack = new Stack<>();
    private String currentRepo = ""; 
    private final HttpClient client = HttpClient.newHttpClient();

    public GithubApp() {
        // Fenster-Einstellungen (Titel, Resizable, Closable, Maximizable, Iconifiable)
        super("GitHub Explorer", true, true, true, true);
        setSize(600, 400);
        setLayout(new BorderLayout(10, 10));

        // --- UI Komponenten ---
        JPanel header = new JPanel(new BorderLayout(5, 5));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        backButton = new JButton("<- Zurück");
        backButton.setEnabled(false);

        searchField = new JTextField();
        JButton searchBtn = new JButton("Suche");

        JPanel searchBar = new JPanel(new BorderLayout(5, 5));
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchBtn, BorderLayout.EAST);

        header.add(backButton, BorderLayout.WEST);
        header.add(searchBar, BorderLayout.CENTER);

        listModel = new DefaultListModel<>();
        listView = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(listView);

        statusLabel = new JLabel("Bereit.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // --- Events ---
        searchBtn.addActionListener(e -> searchRepos(searchField.getText()));
        searchField.addActionListener(e -> searchRepos(searchField.getText()));

        listView.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    handleSelection();
                }
            }
        });

        backButton.addActionListener(e -> goBack());
    }

    private void searchRepos(String query) {
        if (query.isEmpty()) return;
        statusLabel.setText("Suche läuft...");
        listModel.clear();
        navigationStack.clear();
        backButton.setEnabled(false);

        String url = "https://api.github.com/search/repositories?q=" + query;
        fetchData(url, response -> {
            JSONObject json = new JSONObject(response);
            JSONArray items = json.getJSONArray("items");

            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject repo = items.getJSONObject(i);
                    listModel.addElement("[REPO] " + repo.getString("full_name"));
                }
                statusLabel.setText("Ergebnisse für: " + query);
            });
        });
    }

    private void browseRepo(String repoFullname, String path) {
        statusLabel.setText("Lade: " + (path.isEmpty() ? "/" : path));
        String url = "https://api.github.com/repos/" + repoFullname + "/contents/" + path;

        fetchData(url, response -> {
            JSONArray files = new JSONArray(response);
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);
                    String type = file.getString("type");
                    String prefix = type.equals("dir") ? "[ORDNER] " : "[DATEI] ";
                    listModel.addElement(prefix + file.getString("name"));
                }
                currentRepo = repoFullname;
                backButton.setEnabled(true);
            });
        });
    }

    private void handleSelection() {
        String selected = listView.getSelectedValue();
        if (selected == null) return;

        if (selected.startsWith("[REPO] ")) {
            String repoName = selected.replace("[REPO] ", "");
            navigationStack.push("SEARCH"); 
            browseRepo(repoName, "");
        } else if (selected.startsWith("[ORDNER] ")) {
            String folderName = selected.replace("[ORDNER] ", "");
            String currentPath = (navigationStack.isEmpty() || navigationStack.peek().equals("SEARCH")) ? "" : navigationStack.peek();
            String newPath = currentPath.isEmpty() ? folderName : currentPath + "/" + folderName;
            navigationStack.push(newPath);
            browseRepo(currentRepo, newPath);
        } else if (selected.startsWith("[DATEI] ")) {
            downloadFile(selected.replace("[DATEI] ", ""));
        }
    }

    private void downloadFile(String fileName) {
        String currentPath = (navigationStack.isEmpty() || navigationStack.peek().equals("SEARCH")) ? "" : navigationStack.peek();
        String downloadUrl = "https://raw.githubusercontent.com/" + currentRepo + "/main/" + (currentPath.isEmpty() ? "" : currentPath + "/") + fileName;

        statusLabel.setText("Downloade: " + fileName + "...");

        new Thread(() -> {
            try (InputStream in = new URL(downloadUrl).openStream();
                 FileOutputStream fos = new FileOutputStream("VM_Disk/apps/" + fileName)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                SwingUtilities.invokeLater(() -> statusLabel.setText("Gespeichert in VM_Disk/apps/" + fileName));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Download fehlgeschlagen (Main Branch Only)"));
            }
        }).start();
    }

    private void goBack() {
        if (navigationStack.isEmpty()) return;
        navigationStack.pop();
        if (navigationStack.isEmpty() || navigationStack.peek().equals("SEARCH")) {
            searchRepos(searchField.getText());
        } else {
            browseRepo(currentRepo, navigationStack.peek());
        }
    }

    private void fetchData(String url, java.util.function.Consumer<String> callback) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    callback.accept(response.body());
                } else {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("API Fehler (Limit?) Code: " + response.statusCode()));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Verbindungsfehler!"));
            }
        }).start();
    }
}