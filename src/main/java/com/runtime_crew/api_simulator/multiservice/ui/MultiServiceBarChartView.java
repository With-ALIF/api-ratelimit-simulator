package com.runtime_crew.api_simulator.multiservice.ui;

import com.runtime_crew.api_simulator.service.ClientActivityTracker;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiServiceBarChartView extends VBox {

    private final ClientActivityTracker activityTracker;
    private BarChart<String, Number> barChart;

    public MultiServiceBarChartView(ClientActivityTracker activityTracker) {
        super(8);
        this.activityTracker = activityTracker;
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; " +
                "-fx-border-color: #334155; -fx-border-radius: 8; -fx-border-width: 1;");
        refresh(List.of());
    }

    public void refresh(List<String> serviceIds) {
        getChildren().clear();

        Label title = new Label("Allowed vs Blocked per Service");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        if (serviceIds == null || serviceIds.isEmpty()) {
            Label empty = new Label("No services selected");
            empty.setStyle("-fx-text-fill: #64748b;");
            getChildren().addAll(title, empty);
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Service");
        yAxis.setLabel("Requests");
        yAxis.setMinorTickCount(0);
        yAxis.setLowerBound(0);

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setAnimated(false);
        barChart.setLegendVisible(true);
        barChart.setPrefHeight(280);
        barChart.setMinHeight(220);
        barChart.setPadding(new Insets(0));
        barChart.setStyle("-fx-background-color: transparent;");
        barChart.lookup(".chart-plot-background").setStyle("-fx-background-color: #0f172a;");

        XYChart.Series<String, Number> allowedSeries = new XYChart.Series<>();
        allowedSeries.setName("Allowed");
        XYChart.Series<String, Number> blockedSeries = new XYChart.Series<>();
        blockedSeries.setName("Blocked");

        Map<String, int[]> serviceData = new LinkedHashMap<>();
        for (String svcId : serviceIds) {
            ClientActivity activity = activityTracker.getActivity(svcId);
            int allowed = activity != null ? activity.getAllowedRequests() : 0;
            int blocked = activity != null ? activity.getBlockedRequests() : 0;
            serviceData.put(svcId, new int[]{allowed, blocked});
        }

        for (Map.Entry<String, int[]> entry : serviceData.entrySet()) {
            allowedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
            blockedSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
        }

        barChart.getData().addAll(List.of(allowedSeries, blockedSeries));

        getChildren().addAll(title, barChart);
    }
}
