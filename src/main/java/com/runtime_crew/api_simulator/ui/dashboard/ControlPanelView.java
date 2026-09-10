package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.RequestType;
import com.runtime_crew.api_simulator.model.ViolationLevel;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ControlPanelView extends VBox {

    private final ComboBox<String> clientBox = new ComboBox<>();
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final ComboBox<String> statusFilterBox = new ComboBox<>();
    private final Label quotaLabel = new Label("Quota: --");
    private final Label riskLevelLabel = new Label("NORMAL");
    private final Label blockLabel = new Label();
    private final StatsPanel statsPanel;

    private final Button sendBtn = createButton("Send Request", "btn-primary");
    private final Button burstBtn = createButton("Simulate Burst", "btn-warning");
    private final Button loadDatasetBtn = createButton("Load Dataset (.txt/.csv)", "btn-accent");
    private final Button registerBtn = createButton("+ Register Client", "");
    private final Button fullReportBtn = createButton("Full Report", "btn-success");
    private final Button quickReportBtn = createButton("Quick Summary", "");
    private final Button barChartBtn = createButton("Bar Chart", "btn-accent");
    private final Button compareBtn = createButton("Compare All", "");
    private final Button exportBtn = createButton("Export Report", "");
    private final Button clearBtn = createButton("Clear History", "btn-danger");
    private final Button settingsBtn = createButton("Settings", "");

    public ControlPanelView(StatsPanel statsPanel) {
        super(12);
        this.statsPanel = statsPanel;
        initLayout();
    }

    private void initLayout() {
        Label clientTitle = new Label("Select Client");
        clientTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        clientBox.getItems().addAll("ALL_CLIENTS", "CLIENT_A", "CLIENT_B", "CLIENT_C", "CLIENT_D");
        clientBox.setPromptText("Select Client");
        clientBox.setMaxWidth(Double.MAX_VALUE);
        VBox clientSection = new VBox(4, clientTitle, clientBox);

        Label typeTitle = new Label("Request Type");
        typeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        typeBox.getItems().add("ALL");
        for (RequestType rt : RequestType.values()) {
            typeBox.getItems().add(rt.name());
        }
        typeBox.setValue("ALL");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        VBox typeSection = new VBox(4, typeTitle, typeBox);

        Label statusFilterTitle = new Label("Filter Table Status");
        statusFilterTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        statusFilterBox.getItems().addAll("ALL", "ALLOWED", "BLOCKED");
        statusFilterBox.setValue("ALL");
        statusFilterBox.setMaxWidth(Double.MAX_VALUE);
        VBox filterSection = new VBox(4, statusFilterTitle, statusFilterBox);

        Label statusHeader = new Label("Client Status & Quota");
        statusHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        quotaLabel.getStyleClass().addAll("badge-pill", "badge-ok");
        riskLevelLabel.getStyleClass().addAll("badge-pill", "badge-ok");
        HBox statusBox = new HBox(8, quotaLabel, riskLevelLabel);
        statusBox.setAlignment(Pos.CENTER);
        blockLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #ef4444;");
        blockLabel.setVisible(false);
        blockLabel.setManaged(false);
        VBox statusSection = new VBox(4, statusHeader, statusBox, blockLabel);

        VBox headerBox = new VBox(2, new Label("Control Panel"), new Label("Traffic & Rate-Limit Controller"));
        headerBox.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #ffffff;");
        headerBox.getChildren().get(1).setStyle("-fx-font-size: 12px; -fx-text-fill: #e2e8f0;");

        Label actionsHeader = new Label("Traffic Actions");
        actionsHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label reportsHeader = new Label("Reports & Diagnostics");
        reportsHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        this.getStyleClass().add("card");
        this.setPrefWidth(260);
        this.getChildren().addAll(
            headerBox, new Separator(),
            clientSection, typeSection, filterSection, statusSection, new Separator(),
            actionsHeader, sendBtn, burstBtn, loadDatasetBtn, registerBtn, new Separator(),
            reportsHeader, fullReportBtn, quickReportBtn, barChartBtn, compareBtn, exportBtn, clearBtn, new Separator(),
            settingsBtn, statsPanel
        );
    }

    private Button createButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        if (!styleClass.isEmpty()) btn.getStyleClass().add(styleClass);
        return btn;
    }

    public void setOnClientSelected(Consumer<String> consumer) { clientBox.setOnAction(e -> consumer.accept(clientBox.getValue())); }
    public void setOnStatusFilter(Consumer<String> consumer) { statusFilterBox.setOnAction(e -> consumer.accept(statusFilterBox.getValue())); }
    public void setOnTypeFilter(Consumer<Object> consumer) { typeBox.setOnAction(e -> consumer.accept(typeBox.getValue())); }
    public void setOnSendRequest(BiConsumer<String, RequestType> c) {
        sendBtn.setOnAction(e -> {
            String val = typeBox.getValue();
            RequestType type = (val != null && !"ALL".equals(val)) ? RequestType.valueOf(val) : null;
            c.accept(clientBox.getValue(), type);
        });
    }
    public void setOnSimulateBurst(Consumer<String> consumer) { burstBtn.setOnAction(e -> consumer.accept(clientBox.getValue())); }
    public void setOnLoadDataset(Runnable action) { loadDatasetBtn.setOnAction(e -> action.run()); }
    public void setOnRegisterClient(Runnable action) { registerBtn.setOnAction(e -> action.run()); }
    public void setOnFullReport(Consumer<String> c) { fullReportBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnQuickReport(Consumer<String> c) { quickReportBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnCompare(Runnable action) { compareBtn.setOnAction(e -> action.run()); }
    public void setOnBarChart(Runnable action) { barChartBtn.setOnAction(e -> action.run()); }
    public void setOnExport(Consumer<String> c) { exportBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnClearHistory(Consumer<String> c) { clearBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnSettings(Runnable action) { settingsBtn.setOnAction(e -> action.run()); }

    public void updateBurstLabel(int count, int duration) {
        burstBtn.setText(String.format("Simulate Burst (%d Req / %ds)", count, duration));
    }

    public void addClientIfAbsent(String clientId) {
        if (!clientBox.getItems().contains(clientId)) clientBox.getItems().add(clientId);
    }
    public void setSelectedClient(String clientId) { clientBox.setValue(clientId); }
    public String getSelectedClient() { return clientBox.getValue(); }
    public String getSelectedStatus() { return statusFilterBox.getValue(); }
    public String getSelectedType() { return typeBox.getValue() != null ? typeBox.getValue().toString() : "ALL"; }

    public void updateQuota(int remaining, int max) {
        quotaLabel.setText(String.format("Quota: %d/%d", remaining, max));
        quotaLabel.getStyleClass().removeAll("badge-ok", "badge-warning", "badge-danger");
        if (remaining == 0) quotaLabel.getStyleClass().add("badge-danger");
        else if (remaining <= 2) quotaLabel.getStyleClass().add("badge-warning");
        else quotaLabel.getStyleClass().add("badge-ok");
    }

    public void resetQuota() {
        quotaLabel.setText("Quota: --");
        quotaLabel.getStyleClass().removeAll("badge-ok", "badge-warning", "badge-danger");
        quotaLabel.getStyleClass().add("badge-ok");
    }

    public void updateRiskLevel(ViolationLevel level) {
        riskLevelLabel.setText(level != null ? level.toString() : "NORMAL");
        riskLevelLabel.getStyleClass().removeAll("badge-ok", "badge-warning", "badge-danger");
        if (level == null || level == ViolationLevel.NORMAL) riskLevelLabel.getStyleClass().add("badge-ok");
        else if (level == ViolationLevel.WARNING) riskLevelLabel.getStyleClass().add("badge-warning");
        else riskLevelLabel.getStyleClass().add("badge-danger");
    }

    public void updateBlockCountdown(long secondsRemaining) {
        if (secondsRemaining > 0) {
            blockLabel.setText(String.format("BLOCKED - Waiting %ds", secondsRemaining));
            blockLabel.setVisible(true);
            blockLabel.setManaged(true);
        } else {
            blockLabel.setVisible(false);
            blockLabel.setManaged(false);
        }
    }

    public void clearBlockCountdown() {
        blockLabel.setVisible(false);
        blockLabel.setManaged(false);
    }
}
