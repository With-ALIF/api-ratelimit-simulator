package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.EventType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class LogPanel extends VBox {

    private final TextArea logArea = new TextArea();

    public LogPanel() {
        super(8);
        initLayout();
    }

    private void initLayout() {
        Label title = new Label("System Logs");
        title.getStyleClass().add("section-title");

        logArea.setEditable(false);
        logArea.getStyleClass().add("log-area");
        logArea.setPrefRowCount(4);
        logArea.setWrapText(true);

        this.getStyleClass().add("card");
        this.getChildren().addAll(title, logArea);
    }

    public void append(String message) {
        logArea.appendText(message);
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    public void appendEvent(EventType event, String clientId, String details) {
        String icon = (event == null) ? "" : event.getDisplayName().split(" ")[0];
        String label = (event == null) ? "UNKNOWN" : event.name();
        String tag = icon.isEmpty() ? label : icon + " " + label;
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText(String.format("\n%s [%s] %s: %s", time, clientId, tag, details));
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    public void clear() {
        logArea.clear();
    }

    public TextArea getLogArea() {
        return logArea;
    }
}
