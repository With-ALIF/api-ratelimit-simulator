package com.runtime_crew.api_simulator.ui.dashboard;

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
        logArea.setPrefRowCount(6);
        logArea.setWrapText(true);

        this.getStyleClass().add("card");
        this.getChildren().addAll(title, logArea);
    }

    public void append(String message) {
        logArea.appendText(message);
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    public void clear() {
        logArea.clear();
    }

    public TextArea getLogArea() {
        return logArea;
    }
}
