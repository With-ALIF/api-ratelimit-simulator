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
        String icon = event == null ? "" : event.getDisplayName().split(" ")[0];
        String label = event == null ? "UNKNOWN" : event.name();
        String color = getEventColor(event);
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText(String.format("\n%s [%s] %s: %s", time, clientId, label, details));
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private String getEventColor(EventType event) {
        if (event == null) return "#94a3b8";
        switch (event) {
            case ALLOWED: return "#22c55e";
            case BLOCKED: return "#ef4444";
            case ABUSE_DETECTED: return "#f59e0b";
            case RATE_LIMITED: return "#3b82f6";
            case REQUEST: return "#38bdf8";
            default: return "#94a3b8";
        }
    }

    public void clear() {
        logArea.clear();
    }

    public TextArea getLogArea() {
        return logArea;
    }
}
