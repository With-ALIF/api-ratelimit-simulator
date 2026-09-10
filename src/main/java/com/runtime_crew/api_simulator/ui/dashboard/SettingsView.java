package com.runtime_crew.api_simulator.ui.dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SettingsView extends VBox {

    private final TextField maxRequestsField;
    private final TextField timeWindowField;
    private final TextField blockDurationField;
    private final TextField burstCountField;
    private final TextField burstDurationField;
    private final Label errorLabel = new Label();
    private final Label successLabel = new Label();

    private static final String DEF_MAX = "10";
    private static final String DEF_WINDOW = "10";
    private static final String DEF_BLOCK = "3";
    private static final String DEF_BURST_COUNT = "20";
    private static final String DEF_BURST_DUR = "10";

    private final Button saveBtn = createButton("Save", "btn-success");
    private final Button resetBtn = createButton("Reset", "btn-warning");
    private Runnable onSaveAction;

    public SettingsView(int maxRequests, int timeWindow, int blockDuration, int burstCount, int burstDuration) {
        super(10);
        getStyleClass().add("card");
        setPadding(new Insets(16));
        this.maxRequestsField = new TextField(String.valueOf(maxRequests));
        this.timeWindowField = new TextField(String.valueOf(timeWindow));
        this.blockDurationField = new TextField(String.valueOf(blockDuration));
        this.burstCountField = new TextField(String.valueOf(burstCount));
        this.burstDurationField = new TextField(String.valueOf(burstDuration));
        initLayout();
    }

    private void initLayout() {
        Label title = new Label("Settings");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #f8fafc;");

        Label rateLimitHeader = new Label("Rate Limit");
        rateLimitHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38bdf8;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        addField(grid, "Max Requests:", maxRequestsField, 0, "Total requests allowed per window");
        addField(grid, "Time Window (sec):", timeWindowField, 1, "Sliding window duration");
        addField(grid, "Block Duration (sec):", blockDurationField, 2, "Cooldown after quota full");

        Label burstHeader = new Label("Burst Simulation");
        burstHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38bdf8;");

        GridPane burstGrid = new GridPane();
        burstGrid.setHgap(10);
        burstGrid.setVgap(10);

        addField(burstGrid, "Request Count:", burstCountField, 0, "Number of requests in burst");
        addField(burstGrid, "Duration (sec):", burstDurationField, 1, "Time span for burst");

        saveBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);
        HBox buttons = new HBox(10, saveBtn, resetBtn);
        buttons.setAlignment(Pos.CENTER);

        errorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-wrap-text: true; -fx-padding: 6 8; -fx-background-color: rgba(239,68,68,0.1); -fx-background-radius: 6;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        successLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #22c55e; -fx-wrap-text: true; -fx-padding: 6 8; -fx-background-color: rgba(34,197,94,0.1); -fx-background-radius: 6;");
        successLabel.setWrapText(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);

        saveBtn.setOnAction(e -> {
            hideMessages();
            if (validate()) {
                if (onSaveAction != null) onSaveAction.run();
            }
        });

        resetBtn.setOnAction(e -> {
            resetToDefaults();
            hideMessages();
        });

        getChildren().addAll(title, new Separator(), rateLimitHeader, grid, new Separator(), burstHeader, burstGrid, new Separator(), buttons, errorLabel, successLabel);
    }

    private void addField(GridPane grid, String labelText, TextField field, int row, String tooltip) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
        field.setTooltip(new Tooltip(tooltip));
        field.setPrefWidth(120);
        field.setMaxWidth(120);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private Button createButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        return btn;
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    public void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private boolean validate() {
        StringBuilder error = new StringBuilder();

        if (!isPositiveInt(maxRequestsField.getText())) error.append("- Max Requests must be a positive number\n");
        if (!isPositiveInt(timeWindowField.getText())) error.append("- Time Window must be a positive number\n");
        if (!isPositiveInt(blockDurationField.getText())) error.append("- Block Duration must be a positive number\n");
        if (!isPositiveInt(burstCountField.getText())) error.append("- Burst Count must be a positive number\n");
        if (!isPositiveInt(burstDurationField.getText())) error.append("- Burst Duration must be a positive number\n");

        if (error.length() > 0) {
            errorLabel.setText("Invalid input:\n" + error.toString().trim());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return false;
        }
        return true;
    }

    private boolean isPositiveInt(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        try {
            return Integer.parseInt(text.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setOnSave(Runnable action) { this.onSaveAction = action; }

    public void resetToDefaults() {
        maxRequestsField.setText(DEF_MAX);
        timeWindowField.setText(DEF_WINDOW);
        blockDurationField.setText(DEF_BLOCK);
        burstCountField.setText(DEF_BURST_COUNT);
        burstDurationField.setText(DEF_BURST_DUR);
    }

    public int getMaxRequests() {
        try { return Math.max(1, Integer.parseInt(maxRequestsField.getText().trim())); }
        catch (Exception e) { return 10; }
    }

    public int getTimeWindow() {
        try { return Math.max(1, Integer.parseInt(timeWindowField.getText().trim())); }
        catch (Exception e) { return 10; }
    }

    public int getBlockDuration() {
        try { return Math.max(1, Integer.parseInt(blockDurationField.getText().trim())); }
        catch (Exception e) { return 3; }
    }

    public int getBurstCount() {
        try { return Math.max(1, Integer.parseInt(burstCountField.getText().trim())); }
        catch (Exception e) { return 20; }
    }

    public int getBurstDuration() {
        try { return Math.max(1, Integer.parseInt(burstDurationField.getText().trim())); }
        catch (Exception e) { return 10; }
    }
}
