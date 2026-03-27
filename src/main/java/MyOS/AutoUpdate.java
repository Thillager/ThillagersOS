package MyOS;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * AutoUpdate ohne externe JSON-Bibliothek
 */
public class AutoUpdate {

    private static final String GITHUB_API_BASE = "https://api.github.com/repos/Thillager/ThillagersOSResources/releases";

    public static void checkUpdates() {
        File vmDir = new File(Main.VM_DIR);
        if (!vmDir.exists()) return;

        File[] files = vmDir.listFiles(f -> f.getName().endsWith(".jar") || f.getName().endsWith(".illag"));
        if (files == null || files.length == 0) return;

        for (File f : files) {
            String programName = f.getName();
            if (programName.endsWith(".jar") || programName.endsWith(".illag")) {
                programName = programName.substring(0, programName.lastIndexOf('.'));
            }

            try {
                String latestTag = getLatestReleaseTag(programName);
                if (latestTag == null) continue;

                String currentVersion = extractVersionFromFilename(f.getName());

                if (compareVersions(latestTag, currentVersion) > 0) {
                    downloadRelease(programName, latestTag, f);
                    JOptionPane.showMessageDialog(null, programName + " wurde auf Version " + latestTag + " aktualisiert!");
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Prüfen von " + programName + ": " + e.getMessage());
            }
        }
    }

    private static String getLatestReleaseTag(String programName) throws Exception {
        URL url = new URL(GITHUB_API_BASE);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        InputStream is = conn.getInputStream();
        String jsonText = new String(is.readAllBytes());
        is.close();

        // Einfach alle "tag_name": "..." extrahieren
        List<String> tags = new ArrayList<>();
        int idx = 0;
        while ((idx = jsonText.indexOf("\"tag_name\":", idx)) != -1) {
            int start = jsonText.indexOf("\"", idx + 11) + 1;
            int end = jsonText.indexOf("\"", start);
            String tag = jsonText.substring(start, end);
            if (tag.toLowerCase().startsWith(programName.toLowerCase() + "v")) {
                tags.add(tag);
            }
            idx = end;
        }

        if (tags.isEmpty()) return null;

        // maximale Version bestimmen
        String max = tags.get(0);
        for (String t : tags) {
            if (compareVersions(t, max) > 0) max = t;
        }
        return max;
    }

    private static int[] extractVersionFromTag(String tag) {
        String v = tag.replaceAll(".*v", "");
        String[] parts = v.split("\\.");
        int[] ver = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { ver[i] = Integer.parseInt(parts[i]); } catch (Exception e) { ver[i] = 0; }
        }
        return ver;
    }

    private static int compareVersions(String v1, String v2) {
        int[] ver1 = extractVersionFromTag(v1);
        int[] ver2 = extractVersionFromTag(v2);
        int len = Math.max(ver1.length, ver2.length);
        for (int i = 0; i < len; i++) {
            int a = i < ver1.length ? ver1[i] : 0;
            int b = i < ver2.length ? ver2[i] : 0;
            if (a != b) return a - b;
        }
        return 0;
    }

    private static String extractVersionFromFilename(String filename) {
        int idx = filename.toLowerCase().lastIndexOf('v');
        if (idx == -1) return "v0";
        int dot = filename.lastIndexOf('.');
        if (dot == -1) dot = filename.length();
        return filename.substring(idx, dot);
    }

    private static void downloadRelease(String programName, String tag, File targetFile) throws Exception {
        String extension = targetFile.getName().endsWith(".illag") ? ".illag" : ".jar";

        // Wir bauen den Namen so, wie er auf GitHub ist (z.B. TuiCalcv2.0.illag)
        String assetName = tag + extension; 
        String downloadUrl = "https://github.com/Thillager/ThillagersOSResources/releases/download/" + tag + "/" + assetName;

        URL url = new URL(downloadUrl);

        // Ziel-Pfad: Wir speichern die Datei am besten mit dem neuen Namen (inkl. Version)
        // Damit extractVersionFromFilename beim nächsten Mal die v2.0 erkennt!
        File newFile = new File(targetFile.getParent(), assetName);

        try (InputStream in = url.openStream()) {
            Files.copy(in, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Die alte Datei löschen, wenn sie einen anderen Namen hatte
        if (!targetFile.getName().equals(newFile.getName())) {
            targetFile.delete();
        }
    }
}