package MyOS;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import javax.swing.*;

public class BootManager {

    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/Thillager/ThillagersOS/main/version.txt";

    private static final String JAR_URL =
            "https://github.com/Thillager/ThillagersOS/releases/latest/download/ThillagersOS.jar";

    public static void boot() {
        preloadSettings();

        BootAnimation anim = new BootAnimation();
        anim.startAnimation();

        new Thread(() -> {

            try {
                anim.setProgress(10);

                if (isUpdateAvailable()) {
                    anim.setProgress(20);
                    File newJar = downloadUpdate(anim);

                    anim.setProgress(90);

                    launchUpdater(newJar);
                    return;
                }

                anim.setProgress(100);

            } catch (Exception e) {
                e.printStackTrace();
            }

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

    private static boolean isUpdateAvailable() throws Exception {
        String latest = new String(
                new URL(VERSION_URL).openStream().readAllBytes()).trim();

        String current = Main.systemProps.getProperty("version", "1.0");

        return !latest.equals(current);
    }

    private static File downloadUpdate(BootAnimation anim) throws Exception {
        URL url = new URL(JAR_URL);
        InputStream in = url.openStream();

        File outFile = new File("update.jar");
        FileOutputStream out = new FileOutputStream(outFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        int total = 0;

        int fileSize = url.openConnection().getContentLength();

        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            total += bytesRead;

            int percent = (int)((total / (float)fileSize) * 60) + 20;
            anim.setProgress(percent);
        }

        in.close();
        out.close();

        return outFile;
    }

    private static void launchUpdater(File newJar) throws Exception {

        String currentJar = new File(
                BootManager.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()
        ).getName();

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", currentJar,
                "MyOS.Updater",
                currentJar,
                newJar.getName()
        );

        pb.start();
        System.exit(0);
    }
}