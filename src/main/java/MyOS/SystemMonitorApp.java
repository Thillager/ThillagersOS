package MyOS;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import com.sun.management.OperatingSystemMXBean;

public class SystemMonitorApp extends JInternalFrame {

    // 1. Deine Komponenten (Variablen), auf die alle Methoden zugreifen müssen
    private JProgressBar ramBar = new JProgressBar();
    private JProgressBar cpuBar = new JProgressBar();
    private JProgressBar ramProcessBar = new JProgressBar();
    private JProgressBar cpuBarGlobal = new JProgressBar();
    private JLabel cpuLabel = new JLabel("CPU: 0%");
    private GraphPanel cpuGraph = new GraphPanel();
    private JLabel cpuLabelGlobal = new JLabel("CPU (Global): 0%");
    private GraphPanel cpuGraphGlobal = new GraphPanel();
    private OperatingSystemMXBean osBean;
    private JLabel ramLabel = new JLabel("RAM (Global): 0%");
    private JLabel ramProcessLabel = new JLabel("RAM (Desktop): 0%");

    public SystemMonitorApp() {
        super("System Monitor", true, true, true, true);
        setSize(300, 400); // Etwas höher machen für den Graphen

        // Setup osBean
        osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // Layout aufbauen
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Komponenten hinzufügen
        ramLabel = new JLabel();
        ramProcessLabel = new JLabel();

        mainPanel.add(ramProcessLabel);
        mainPanel.add(ramProcessBar);
        mainPanel.add(Box.createVerticalStrut(10)); // Kleiner Abstand
        mainPanel.add(ramLabel);
        mainPanel.add(ramBar);
        mainPanel.add(Box.createVerticalStrut(10)); // Kleiner Abstand
        mainPanel.add(cpuLabel);
        mainPanel.add(cpuBar);
        mainPanel.add(Box.createVerticalStrut(10)); // Kleiner Abstand

        // Den Graphen hinzufügen
        cpuGraph.setPreferredSize(new Dimension(280, 100));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(cpuGraph);

        mainPanel.add(cpuLabelGlobal);
        mainPanel.add(cpuBarGlobal);

        // Den Graphen hinzufügen
        cpuGraphGlobal.setPreferredSize(new Dimension(280, 100));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(cpuGraphGlobal);
        
        add(mainPanel);


        // Timer starten (alle 1 Sekunde)
        Timer timer = new Timer(1000, e -> updateStats());
        timer.start();
    }

    private void updateStats() {
        try {
            // RAM berechnen
            long total = osBean.getTotalPhysicalMemorySize();
            long free = osBean.getFreePhysicalMemorySize();
            long totalProcesRam = osBean.getTotalMemorySize();
            long freeProcesRam = osBean.getFreeMemorySize();
            int ProcesRam = (int) ((totalProcesRam - freeProcesRam) / totalProcesRam);
            int ramPercent = (int) (((total - free) * 100) / total);
            ramBar.setValue(ramPercent);
            ramLabel.setText("RAM (Global): " + ramPercent + "%");
            ramProcessBar.setValue(ProcesRam);
            ramProcessLabel.setText("RAM (Desktop): " + ProcesRam + "%");

            // CPU (Global)
            double cpuGlobal = osBean.getSystemCpuLoad();
            if (cpuGlobal >= 0) {
                int cpuGlobalPercent = (int) (cpuGlobal * 100);
                cpuLabelGlobal.setText("CPU (Global): " + cpuGlobalPercent + "%");
                cpuBarGlobal.setValue(cpuGlobalPercent);

                // DEN GRAPH FÜTTERN
                cpuGraphGlobal.addValue(cpuGlobalPercent);
            }
            
            // CPU (Prozess-Last für Stabilität)    
            double cpu = osBean.getProcessCpuLoad();
            if (cpu >= 0) {
                int cpuPercent = (int) (cpu * 100);
                cpuBar.setValue(cpuPercent);
                cpuLabel.setText("CPU (Desktop): " + cpuPercent + "%");

                // DEN GRAPH FÜTTERN
                cpuGraph.addValue(cpuPercent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --- HIER KOMMT DIE HILFSKLASSE FÜR DEN GRAPHEN ---
    // Du kannst sie einfach UNTER deine updateStats Methode schreiben
    class GraphPanel extends JPanel {
        private ArrayList<Integer> values = new ArrayList<>();

        public synchronized void addValue(int val) {   if (values.size()>20) values.remove(0);   values.add(val);   repaint();   }  

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.RED);
            for (int i = 0; i < values.size() - 1; i++) {
                int x1 = i * (getWidth() / 19);
                int y1 = (getHeight() - 2) - (values.get(i) * getHeight() / 100);
                int x2 = (i + 1) * (getWidth() / 19);
                int y2 = (getHeight() - 2) - (values.get(i + 1) * getHeight() / 100);
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }
}
