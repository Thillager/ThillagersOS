package MyOS;

import java.io.File;
import java.nio.file.*;

public class Updater {

    public static void main(String[] args) {
        try {
            String oldJar = args[0];
            String newJar = args[1];

            Thread.sleep(2000); // warten bis alte Version zu ist

            Files.move(
                new File(newJar).toPath(),
                new File(oldJar).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );

            // neue Version starten
            new ProcessBuilder("java", "-jar", oldJar).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}