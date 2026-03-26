    package MyOS;

    import javax.swing.*;
    import java.awt.*;
    import java.awt.event.ActionEvent;
    import java.awt.event.ActionListener;
    import java.awt.event.KeyAdapter;
    import java.awt.event.KeyEvent;

    public class Messenger extends JInternalFrame {
        private JTextArea chatArea;
        private JTextArea inputField; // Geändert von JTextField zu JTextArea

        public Messenger() {
            // Titel, Resizable, Closable, Maximizable, Iconifiable
            super("Messenger", true, true, true, true);
            setSize(400, 500);
            setLayout(new BorderLayout());

            // 1. Chat-Verlauf (Oben/Mitte)
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            add(new JScrollPane(chatArea), BorderLayout.CENTER);

            // 2. Eingabe-Bereich (Unten)
            JPanel bottomPanel = new JPanel(new BorderLayout());

            inputField = new JTextArea(3, 20); // 3 Zeilen sichtbare Höhe
            inputField.setLineWrap(true);
            inputField.setWrapStyleWord(true);

            // Scrollbar für das Eingabefeld, falls der Text sehr lang wird
            JScrollPane inputScroll = new JScrollPane(inputField);

            JButton sendButton = new JButton("Senden");

            bottomPanel.add(inputScroll, BorderLayout.CENTER);
            bottomPanel.add(sendButton, BorderLayout.EAST);
            add(bottomPanel, BorderLayout.SOUTH);

            // 3. Logik für das Senden
            ActionListener sendAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendMessage();
                }
            };

            sendButton.addActionListener(sendAction);

            // KeyListener, um Enter zum Senden zu nutzen
            inputField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (e.isShiftDown()) {
                            // Bei Shift + Enter: Normaler Zeilenumbruch
                            inputField.append("\n");
                        } else {
                            // Bei nur Enter: Senden
                            e.consume(); // Verhindert, dass ein Zeilenumbruch getippt wird
                            sendMessage();
                        }
                    }
                }
            });

            setVisible(true);
        }

        private void sendMessage() {
            String text = inputField.getText().trim();

            if (text.isEmpty()) {
                return;
            }

            // 1. Anzeige im GUI-Fenster (Chat-Area)
            chatArea.append("Du:\n" + text + "\n\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());

            // 2. Ausgabe in die Konsole
            System.out.println("--- Neue Nachricht ---");
            System.out.println(text);
            System.out.println("----------------------");

            // Eingabefeld leeren
            inputField.setText("");
        }
    }