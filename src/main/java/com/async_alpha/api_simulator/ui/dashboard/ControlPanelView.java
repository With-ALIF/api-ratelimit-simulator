package com.async_alpha.api_simulator.ui.dashboard;

import com.async_alpha.api_simulator.model.RequestType;
import com.async_alpha.api_simulator.model.ViolationLevel;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ControlPanelView extends VBox {

    private final ComboBox<String> clientBox = new ComboBox<>();
    private final ComboBox<RequestType> typeBox = new ComboBox<>();
    private final Label quotaLabel = new Label("Quota: --");
    private final Label riskLevelLabel = new Label("NORMAL");
    private final StatsPanel statsPanel;

    private final Button sendBtn = createButton("Send Request", "btn-primary");
    private final Button burstBtn = createButton("Simulate Burst (20 Req / 10s)", "btn-warning");
    private final Button loadDatasetBtn = createButton("Load Dataset (.txt/.csv)", "btn-accent");
    private final Button fullReportBtn = createButton("Full Report", "btn-success");
    private final Button quickReportBtn = createButton("Quick Summary", "");
    private final Button compareBtn = createButton("Compare All", "");
    private final Button exportBtn = createButton("Export Report", "");
    private final Button clearBtn = createButton("Clear History", "btn-danger");

    public ControlPanelView(StatsPanel statsPanel) {
        super(12);
        this.statsPanel = statsPanel;
        initLayout();
    }

    private void initLayout() {
        clientBox.getItems().addAll("CLIENT_A", "CLIENT_B", "CLIENT_C", "CLIENT_D");
        clientBox.setPromptText("Select Client");
        clientBox.setMaxWidth(Double.MAX_VALUE);

        typeBox.getItems().addAll(RequestType.values());
        typeBox.setPromptText("Request Type");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        quotaLabel.getStyleClass().addAll("badge-pill", "badge-ok");
        riskLevelLabel.getStyleClass().addAll("badge-pill", "badge-ok");

        HBox statusBox = new HBox(8, quotaLabel, riskLevelLabel);
        statusBox.setAlignment(Pos.CENTER);

        VBox headerBox = new VBox(2, new Label("Control Panel"), new Label("Traffic & Rate-Limit Controller"));
        headerBox.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #f8fafc;");
        headerBox.getChildren().get(1).getStyleClass().add("sub-title");

        this.getStyleClass().add("card");
        this.setPrefWidth(260);
        this.getChildren().addAll(
            headerBox, new Separator(),
            clientBox, typeBox, statusBox, new Separator(),
            sendBtn, burstBtn, loadDatasetBtn, fullReportBtn, quickReportBtn, compareBtn, exportBtn, clearBtn,
            new Separator(), statsPanel
        );
    }

    private Button createButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        if (!styleClass.isEmpty()) btn.getStyleClass().add(styleClass);
        return btn;
    }

    public void setOnClientSelected(Consumer<String> consumer) { clientBox.setOnAction(e -> consumer.accept(clientBox.getValue())); }
    public void setOnSendRequest(BiConsumer<String, RequestType> c) { sendBtn.setOnAction(e -> c.accept(clientBox.getValue(), typeBox.getValue())); }
    public void setOnSimulateBurst(Consumer<String> consumer) { burstBtn.setOnAction(e -> consumer.accept(clientBox.getValue())); }
    public void setOnLoadDataset(Runnable action) { loadDatasetBtn.setOnAction(e -> action.run()); }
    public void setOnFullReport(Consumer<String> c) { fullReportBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnQuickReport(Consumer<String> c) { quickReportBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnCompare(Runnable action) { compareBtn.setOnAction(e -> action.run()); }
    public void setOnExport(Consumer<String> c) { exportBtn.setOnAction(e -> c.accept(clientBox.getValue())); }
    public void setOnClearHistory(Consumer<String> c) { clearBtn.setOnAction(e -> c.accept(clientBox.getValue())); }

    public void addClientIfAbsent(String clientId) {
        if (!clientBox.getItems().contains(clientId)) clientBox.getItems().add(clientId);
    }
    public void setSelectedClient(String clientId) { clientBox.setValue(clientId); }
    public String getSelectedClient() { return clientBox.getValue(); }

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
}
