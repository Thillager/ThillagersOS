package MyOS;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

// ================= TEXT EDITOR (ENHANCED) =================
public class TextEditor extends JInternalFrame {
    private JTextArea area;
    private File currentFile;

    public TextEditor() { this(null); }
    public TextEditor(File f) {
        super("Text Editor", true, true, true, true);
        this.currentFile = f;
        setSize(600, 500);
        area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(new Color(250, 250, 250));

        JToolBar toolBar = new JToolBar();
        JButton loadBtn = new JButton("[LOAD] Oeffnen");
        JButton saveBtn = new JButton("[SAVE] Speichern");
        JButton clearBtn = new JButton("[CLEAR]");

        loadBtn.addActionListener(e -> loadAction());
        saveBtn.addActionListener(e -> saveAction());
        clearBtn.addActionListener(e -> area.setText(""));

        toolBar.add(loadBtn); toolBar.add(saveBtn); toolBar.addSeparator(); toolBar.add(clearBtn);

        if(f != null) loadFile(f);

        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);

        // Status Bar
        JLabel status = new JLabel(" Zeichensatz: UTF-8");
        status.setBorder(new BevelBorder(BevelBorder.LOWERED));
        add(status, BorderLayout.SOUTH);
    }

    private void loadAction() {
        String name = JOptionPane.showInputDialog("Dateiname in " + Main.VM_DIR + " (z.B. test.txt):");
        if(name != null) {
            File f = new File(Main.VM_DIR, name);
            if(f.exists()) loadFile(f);
            else JOptionPane.showMessageDialog(this, "Datei nicht gefunden!");
        }
    }

    private void loadFile(File f) {
        try {
            currentFile = f;
            area.setText(new String(Files.readAllBytes(f.toPath())));
        } catch(Exception e){ JOptionPane.showMessageDialog(this, "Fehler beim Laden."); 
            e.printStackTrace();
        }
    }

    private void saveAction() {
        if(currentFile == null) {
            String name = JOptionPane.showInputDialog("Speichern als:");
            if(name == null) return;
            currentFile = new File(Main.VM_DIR, name);
        }
        try {
            Files.write(currentFile.toPath(), area.getText().getBytes());
            JOptionPane.showMessageDialog(this, "Gespeichert!");
        } catch(Exception ex){ JOptionPane.showMessageDialog(this, "Fehler beim Speichern."); 
            ex.printStackTrace();
        }
    }
}