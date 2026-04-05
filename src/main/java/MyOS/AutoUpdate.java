package MyOS;

import java.io.File;
import java.io.InputStream;
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
            String fileName = f.getName();

            // Finde das 'v' (z.B. in SuperCalcv1.0.illag) um den reinen Programmnamen zu extrahieren
            int vIndex = fileName.toLowerCase().lastIndexOf('v');

            // Wenn kein 'v' im Namen ist, überspringen wir die Datei (kein gültiges Format)
            if (vIndex == -1) continue;

            // Extrahiert z.B. "SuperCalc" aus "SuperCalcv1.0.illag"
            String programNameOnly = fileName.substring(0, vIndex);

            try {
                // Sucht auf GitHub nach Tags, die mit "SuperCalcv" beginnen
                String latestTag = getLatestReleaseTag(programNameOnly);
                if (latestTag == null) continue;

                // Extrahiert die aktuelle Version, z.B. "v1.0"
                String currentVersion = extractVersionFromFilename(fileName);

                // Vergleiche aktuelle Version mit GitHub Version
                if (compareVersions(latestTag, currentVersion) > 0) {
                    downloadRelease(programNameOnly, latestTag, f);
                    JOptionPane.showMessageDialog(null, programNameOnly + " wurde auf Version " + latestTag + " aktualisiert!");
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Prüfen von " + programNameOnly + ": " + e.getMessage());
            }
        }
    }

    private static String getLatestReleaseTag(String programName) throws Exception {
        URL url = new URL(GITHUB_API_BASE);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        // WICHTIG: GitHub verlangt zwingend einen User-Agent!
        conn.setRequestProperty("User-Agent", "Java-MyOS-Updater"); 
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        InputStream is = conn.getInputStream();
        String jsonText = new String(is.readAllBytes());
        is.close();

        // Alle "tag_name": "..." extrahieren
        List<String> tags = new ArrayList<>();
        int idx = 0;
        while ((idx = jsonText.indexOf("\"tag_name\":", idx)) != -1) {
            int start = jsonText.indexOf("\"", idx + 11) + 1;
            int end = jsonText.indexOf("\"", start);
            String tag = jsonText.substring(start, end);

            // Prüft ob der Tag z.B. mit "supercalcv" beginnt
            if (tag.toLowerCase().startsWith(programName.toLowerCase() + "v")) {
                tags.add(tag);
            }
            idx = end;
        }

        if (tags.isEmpty()) return null;

        // Maximale Version bestimmen
        String max = tags.get(0);
        for (String t : tags) {
            if (compareVersions(t, max) > 0) max = t;
        }
        return max;
    }

    private static int[] extractVersionFromTag(String tag) {
        // Entfernt alles vor und inklusive dem 'v' (z.B. "SuperCalcv2.1" -> "2.1")
        String v = tag.replaceAll(".*[vV]", ""); 
        String[] parts = v.split("\\.");
        int[] ver = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { 
                ver[i] = Integer.parseInt(parts[i]); 
            } catch (Exception e) { 
                ver[i] = 0; 
            }
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

        // Wir bauen den Namen so, wie er auf GitHub ist (z.B. SuperCalcv2.1.illag)
        String assetName = tag + extension; 
        String downloadUrl = "https://github.com/Thillager/ThillagersOSResources/releases/download/" + tag + "/" + assetName;

        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Java-MyOS-Updater"); // Auch hier den User-Agent setzen zur Sicherheit

        // Ziel-Pfad mit neuem Namen (inkl. Version)
        File newFile = new File(targetFile.getParent(), assetName);

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Die alte Datei löschen, damit keine Duplikate im Ordner bleiben
        if (!targetFile.getName().equals(newFile.getName())) {
            targetFile.delete();
        }
    }
}