package MyOS; // WICHTIG: Muss zum Ordner passen!

import MyOS.api.MyOSApp;
import javax.swing.*;
import javax.tools.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files; // Behebt den "cannot find symbol: Files" Fehler

public class MyIde extends JInternalFrame implements MyOSApp {

    public MyIde() {
        super("Java IDE", true, true, true, true);
        setSize(500, 400);
        add(createUI());
    }

    @Override
    public String getAppName() {
        return "Java Editor v1.0";
    }

    @Override
    public JComponent createUI() {
        JPanel panel = new JPanel(new BorderLayout());

        // Der Editor-Bereich
        JTextArea codeArea = new JTextArea(
            "public class TempApp {\n" +
            "    public static javax.swing.JPanel create() {\n" +
            "        javax.swing.JPanel p = new javax.swing.JPanel();\n" +
            "        p.add(new javax.swing.JLabel(\"Hallo Welt!\"));\n" +
            "        return p;\n" +
            "    }\n" +
            "}"
        );
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(codeArea), BorderLayout.CENTER);

        // Button zum Kompilieren
        JButton runButton = new JButton("Compile & Run");
        runButton.addActionListener(e -> compileAndRun(codeArea.getText()));
        panel.add(runButton, BorderLayout.SOUTH);

        return panel;
    }

    private void compileAndRun(String code) {
        try {
            File javaFile = new File("TempApp.java");
            Files.writeString(javaFile.toPath(), code);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                JOptionPane.showMessageDialog(this, "Kein JDK gefunden!");
                return;
            }

            int result = compiler.run(null, null, null, javaFile.getAbsolutePath());
            if (result == 0) {
                JOptionPane.showMessageDialog(this, "Erfolgreich kompiliert!");
            } else {
                JOptionPane.showMessageDialog(this, "Fehler beim Kompilieren!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}