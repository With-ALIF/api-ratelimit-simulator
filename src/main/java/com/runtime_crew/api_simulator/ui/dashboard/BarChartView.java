package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.service.ClientActivityTracker;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

public class BarChartView extends VBox {

    private final ClientActivityTracker activityTracker;
    private final Supplier<String> selectedClientSupplier;

    public BarChartView(ClientActivityTracker activityTracker, Supplier<String> selectedClientSupplier) {
        super(8);
        this.activityTracker = activityTracker;
        this.selectedClientSupplier = selectedClientSupplier;

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

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Client");
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

        XYChart.Series<String, Number> allowedSeries = new XYChart.Series<>();
        allowedSeries.setName("Allowed");
        XYChart.Series<String, Number> blockedSeries = new XYChart.Series<>();
        blockedSeries.setName("Blocked");

        if ("ALL_CLIENTS".equals(clientId)) {
            Map<String, ClientActivity> allActivities = new TreeMap<>(activityTracker.getAllActivities());
            if (allActivities.isEmpty()) {
                getChildren().add(new Label("No data available"));
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
                getChildren().add(new Label("No data for " + clientId));
                return;
            }
            allowedSeries.getData().add(new XYChart.Data<>(clientId, activity.getAllowedRequests()));
            blockedSeries.getData().add(new XYChart.Data<>(clientId, activity.getBlockedRequests()));
        }

        barChart.getData().addAll(java.util.List.of(allowedSeries, blockedSeries));

        Label title = new Label("Request Distribution - " + ("ALL_CLIENTS".equals(clientId) ? "All Clients" : clientId));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        getChildren().addAll(title, barChart);
    }
}
