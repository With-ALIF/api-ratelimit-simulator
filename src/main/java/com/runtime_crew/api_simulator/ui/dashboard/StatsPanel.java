package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class StatsPanel extends VBox {

    private final Label totalReqLabel = new Label("0");
    private final Label allowedReqLabel = new Label("0");
    private final Label blockedReqLabel = new Label("0");
    private final Label successRateLabel = new Label("100%");

    public StatsPanel() {
        super(8);
        initLayout();
    }

    private void initLayout() {
        Label statsTitle = new Label("Statistics");
        statsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Total Requests:"), 0, 0);
        grid.add(totalReqLabel, 1, 0);

        grid.add(new Label("Allowed:"), 0, 1);
        grid.add(allowedReqLabel, 1, 1);

        grid.add(new Label("Blocked:"), 0, 2);
        grid.add(blockedReqLabel, 1, 2);

        grid.add(new Label("Success Rate:"), 0, 3);
        grid.add(successRateLabel, 1, 3);

        totalReqLabel.setStyle("-fx-font-weight: bold;");
        allowedReqLabel.setStyle("-fx-font-weight: bold;");
        allowedReqLabel.getStyleClass().add("status-ok");
        blockedReqLabel.setStyle("-fx-font-weight: bold;");
        blockedReqLabel.getStyleClass().add("status-danger");
        successRateLabel.setStyle("-fx-font-weight: bold;");

        this.getChildren().addAll(statsTitle, grid);
    }

    public void updateStats(ClientActivity activity) {
        if (activity == null) {
            resetStats();
            return;
        }

        totalReqLabel.setText(String.valueOf(activity.getTotalRequests()));
        allowedReqLabel.setText(String.valueOf(activity.getAllowedRequests()));
        blockedReqLabel.setText(String.valueOf(activity.getBlockedRequests()));
        successRateLabel.setText(String.format("%.1f%%", activity.getSuccessRate()));

        successRateLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        if (activity.getSuccessRate() >= 80) {
            successRateLabel.getStyleClass().add("status-ok");
        } else if (activity.getSuccessRate() >= 50) {
            successRateLabel.getStyleClass().add("status-warning");
        } else {
            successRateLabel.getStyleClass().add("status-danger");
        }
    }

    public void updateStats(int total, int allowed, int blocked) {
        totalReqLabel.setText(String.valueOf(total));
        allowedReqLabel.setText(String.valueOf(allowed));
        blockedReqLabel.setText(String.valueOf(blocked));
        double successRate = total == 0 ? 100.0 : (allowed * 100.0) / total;
        successRateLabel.setText(String.format("%.1f%%", successRate));

        successRateLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        if (successRate >= 80) {
            successRateLabel.getStyleClass().add("status-ok");
        } else if (successRate >= 50) {
            successRateLabel.getStyleClass().add("status-warning");
        } else {
            successRateLabel.getStyleClass().add("status-danger");
        }
    }

    public void resetStats() {
        totalReqLabel.setText("0");
        allowedReqLabel.setText("0");
        blockedReqLabel.setText("0");
        successRateLabel.setText("100%");
        successRateLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        successRateLabel.getStyleClass().add("status-ok");
    }
}
