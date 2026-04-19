package MyOS;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import javax.swing.*;

public class BootManager {

    public static void boot() {
        preloadSettings();
        System.out.println("updatehatversucht1");
        AutoUpdate.checkUpdates();

        BootAnimation anim = new BootAnimation();
        anim.startAnimation();

        new Thread(() -> {
            
            anim.finish(() -> startOS());
        }).start();
    }

    private static void startOS() {
        SwingUtilities.invokeLater(() ->
                new Main().setVisible(true));
    }

    private static void preloadSettings() {
        try (InputStream in = new FileInputStream("system.cfg")) {
            Main.systemProps.load(in);
        } catch (Exception ignored) {}
    }

    /**
     * Diese Methode korrigiert die losen Befehle am Ende deines Codes.
     * Sie erstellt den ProcessBuilder 'pb', startet die neue Version 
     * und beendet den aktuellen BootManager.
     */
    public static void restartWithNewVersion(String jarPath) {
        try {
            // Erstellt den Prozess, um die neue JAR zu starten
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath);

            // Startet den neuen Prozess
            pb.start();

            // Beendet die aktuelle Instanz (das alte OS)
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fehler beim Neustart: " + e.getMessage());
        }
    }
}