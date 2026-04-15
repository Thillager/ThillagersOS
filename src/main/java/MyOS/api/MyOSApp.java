package MyOS.api;

import javax.swing.JComponent;

public interface MyOSApp {
    String getAppName();
    JComponent createUI();
}