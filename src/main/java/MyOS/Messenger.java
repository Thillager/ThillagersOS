package MyOS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Messenger extends JInternalFrame {
    private JTextArea chatArea;
    private JTextField inputField;

    public Messenger() {
        // Titel, Resizable, Closable, Maximizable, Iconifiable
        super("Messenger", true, true, true, true);
        setSize(400, 500);
        setLayout(new BorderLayout());

        // 1. Chat-Verlauf (Oben/Mitte)
        chatArea = new JTextArea();
        chatArea.setEditable(false); // Nutzer soll hier nicht direkt tippen
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 2. Eingabe-Bereich (Unten)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("Senden");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 3. Logik für den Button
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction); // Senden bei Enter-Taste

        setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            chatArea.append("Du: " + text + "\n");
            System.out.println(text);
            inputField.setText(""); // Feld leeren
        }
    }
}