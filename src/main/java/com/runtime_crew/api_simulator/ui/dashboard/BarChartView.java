package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.service.ClientActivityTracker;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ActivityRecord;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

public class BarChartView extends VBox {

    private static final String ALL_CLIENTS = "ALL_CLIENTS";
    private static final String NO_SERVICE = "-";

    private final ClientActivityTracker activityTracker;
    private final Supplier<String> selectedClientSupplier;
    private final boolean multiServiceMode;

    public BarChartView(ClientActivityTracker activityTracker, Supplier<String> selectedClientSupplier) {
        this(activityTracker, selectedClientSupplier, false);
    }

    public BarChartView(ClientActivityTracker activityTracker, Supplier<String> selectedClientSupplier,
                        boolean multiServiceMode) {
        super(8);
        this.activityTracker = activityTracker;
        this.selectedClientSupplier = selectedClientSupplier;
        this.multiServiceMode = multiServiceMode;

        getStyleClass().add("card");
        setPadding(new Insets(10));
        refresh();
    }

    public void refresh() {
        getChildren().clear();

        String clientId = selectedClientSupplier.get();
        if (clientId == null) {
            getChildren().add(new Label("No client selected"));
            return;
        }

        String scope = ALL_CLIENTS.equals(clientId) ? "All Clients" : clientId;
        String titleText = multiServiceMode
                ? "Allowed vs Blocked per Service - " + scope
                : "Request Distribution - " + scope;

        XYChart.Series<String, Number> allowedSeries = new XYChart.Series<>();
        allowedSeries.setName("Allowed");
        XYChart.Series<String, Number> blockedSeries = new XYChart.Series<>();
        blockedSeries.setName("Blocked");

        if (multiServiceMode) {
            Map<String, int[]> serviceStats = collectServiceStats(clientId);
            if (serviceStats.isEmpty()) {
                showMessage(titleText, hasActivity(clientId)
                        ? "No multi-service requests recorded for " + scope
                        : "No data for " + scope);
                return;
            }
            for (Map.Entry<String, int[]> entry : serviceStats.entrySet()) {
                allowedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
                blockedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
            }
        } else if (ALL_CLIENTS.equals(clientId)) {
            Map<String, ClientActivity> allActivities = new TreeMap<>(activityTracker.getAllActivities());
            if (allActivities.isEmpty()) {
                showMessage(titleText, "No data available");
                return;
            }
            for (var entry : allActivities.entrySet()) {
                ClientActivity activity = entry.getValue();
                allowedSeries.getData().add(new XYChart.Data<>(activity.getClientId(), activity.getAllowedRequests()));
                blockedSeries.getData().add(new XYChart.Data<>(activity.getClientId(), activity.getBlockedRequests()));
            }
        } else {
            ClientActivity activity = activityTracker.getActivity(clientId);
            if (activity == null) {
                showMessage(titleText, "No data for " + clientId);
                return;
            }
            allowedSeries.getData().add(new XYChart.Data<>(clientId, activity.getAllowedRequests()));
            blockedSeries.getData().add(new XYChart.Data<>(clientId, activity.getBlockedRequests()));
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(multiServiceMode ? "Service" : "Client");
        yAxis.setLabel("Requests");
        yAxis.setMinorTickCount(0);
        yAxis.setLowerBound(0);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setAnimated(false);
        barChart.setLegendVisible(true);
        barChart.setPrefHeight(300);
        barChart.setMinHeight(250);
        barChart.setPadding(new Insets(0));
        barChart.getStyleClass().add("bar-chart-dark");

        barChart.getData().addAll(List.of(allowedSeries, blockedSeries));

        Label title = new Label(titleText);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        getChildren().addAll(title, barChart);
    }

    private Map<String, int[]> collectServiceStats(String clientId) {
        Map<String, int[]> serviceStats = new TreeMap<>();
        if (ALL_CLIENTS.equals(clientId)) {
            for (ClientActivity activity : activityTracker.getAllActivities().values()) {
                aggregateServiceStats(activity.getRecords(), serviceStats);
            }
            return serviceStats;
        }
        ClientActivity activity = activityTracker.getActivity(clientId);
        if (activity != null) {
            aggregateServiceStats(activity.getRecords(), serviceStats);
        }
        return serviceStats;
    }

    /**
     * Aggregates multi-service activity records into per-service counters.
     * Each entry maps "service name" to {@code [allowed, blocked]}.
     * Records without a service (plain single-service requests) are skipped,
     * matching the multi-service filtering of {@link ActivityTableView}.
     */
    static void aggregateServiceStats(List<ActivityRecord> records, Map<String, int[]> serviceStats) {
        if (records == null || serviceStats == null) return;
        for (ActivityRecord record : records) {
            String service = record.getService();
            if (service == null || service.trim().isEmpty() || NO_SERVICE.equals(service.trim())) {
                continue;
            }
            int[] counters = serviceStats.computeIfAbsent(service.trim(), key -> new int[]{0, 0});
            if (record.isBlocked()) {
                counters[1]++;
            } else {
                counters[0]++;
            }
        }
    }

    private boolean hasActivity(String clientId) {
        if (ALL_CLIENTS.equals(clientId)) {
            return !activityTracker.getAllActivities().isEmpty();
        }
        return activityTracker.getActivity(clientId) != null;
    }

    private void showMessage(String titleText, String message) {
        Label title = new Label(titleText);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        Label info = new Label(message);
        info.setStyle("-fx-text-fill: #64748b;");

        getChildren().addAll(title, info);
    }
}
