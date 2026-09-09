package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.service.ClientActivityTracker;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ActivityRecord;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TrafficChartView extends VBox {

    private static final int WINDOW_SECONDS = 60;
    private static final int INTERVAL_SECONDS = 3;

    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final LineChart<String, Number> chart;
    private final XYChart.Series<String, Number> allowedSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> blockedSeries = new XYChart.Series<>();

    private final ClientActivityTracker activityTracker;
    private final Supplier<String> selectedClientSupplier;
    private final Timeline timeline;
    private final DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Label allowedCount = new Label("0");
    private final Label blockedCount = new Label("0");

    public TrafficChartView(ClientActivityTracker activityTracker, Supplier<String> selectedClientSupplier) {
        super(4);
        this.activityTracker = activityTracker;
        this.selectedClientSupplier = selectedClientSupplier;

        xAxis.setAnimated(false);
        xAxis.setAutoRanging(true);
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLength(0);
        xAxis.setLabel(null);

        yAxis.setAnimated(false);
        yAxis.setMinorTickCount(0);
        yAxis.setLowerBound(0);
        yAxis.setTickUnit(1);
        yAxis.setLabel(null);

        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(140);
        chart.setMinHeight(100);
        chart.setMaxHeight(180);
        chart.setPadding(new Insets(0));
        chart.getStyleClass().add("traffic-chart");

        allowedSeries.setName("Allowed");
        blockedSeries.setName("Blocked");
        chart.getData().add(allowedSeries);
        chart.getData().add(blockedSeries);

        Label title = new Label("Live Traffic");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        allowedCount.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #22c55e;");
        blockedCount.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #ef4444;");

        Label allowedLbl = new Label("Allowed:");
        allowedLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        Label blockedLbl = new Label("Blocked:");
        blockedLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        HBox legend = new HBox(12, allowedLbl, allowedCount, blockedLbl, blockedCount);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(0, 0, 2, 4));

        HBox header = new HBox(8, title, legend);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        getChildren().addAll(header, chart);

        setPadding(new Insets(6, 8, 4, 8));
        getStyleClass().add("card");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateData()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Platform.runLater(this::updateData);
    }

    public void updateData() {
        String clientId = selectedClientSupplier.get();
        List<ActivityRecord> records = getRecordsForClient(clientId);

        LocalDateTime now = LocalDateTime.now();
        int bucketCount = WINDOW_SECONDS / INTERVAL_SECONDS;
        Map<String, int[]> buckets = new LinkedHashMap<>();

        for (int i = bucketCount - 1; i >= 0; i--) {
            LocalDateTime bucketTime = now.minusSeconds((long) i * INTERVAL_SECONDS);
            String label = bucketTime.format(labelFmt);
            buckets.put(label, new int[]{0, 0});
        }

        int totalAllowed = 0, totalBlocked = 0;
        for (ActivityRecord rec : records) {
            long secondsAgo = java.time.Duration.between(rec.getTimestamp(), now).getSeconds();
            if (secondsAgo >= 0 && secondsAgo < WINDOW_SECONDS) {
                int bucketIndex = (int) (secondsAgo / INTERVAL_SECONDS);
                if (bucketIndex >= bucketCount) continue;
                LocalDateTime bucketTime = now.minusSeconds((long) bucketIndex * INTERVAL_SECONDS);
                String label = bucketTime.format(labelFmt);
                int[] bucket = buckets.get(label);
                if (bucket != null) {
                    if (rec.isBlocked()) { bucket[1]++; totalBlocked++; }
                    else { bucket[0]++; totalAllowed++; }
                }
            }
        }

        allowedCount.setText(String.valueOf(totalAllowed));
        blockedCount.setText(String.valueOf(totalBlocked));

        List<XYChart.Data<String, Number>> allowedData = new ArrayList<>();
        List<XYChart.Data<String, Number>> blockedData = new ArrayList<>();

        for (Map.Entry<String, int[]> entry : buckets.entrySet()) {
            allowedData.add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
            blockedData.add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
        }

        allowedSeries.getData().setAll(allowedData);
        blockedSeries.getData().setAll(blockedData);

        int maxVal = 1;
        for (int[] bucket : buckets.values()) {
            maxVal = Math.max(maxVal, Math.max(bucket[0], bucket[1]));
        }
        yAxis.setUpperBound(maxVal + 2);
        yAxis.setTickUnit(Math.max(1, (maxVal + 1) / 5));
    }

    private List<ActivityRecord> getRecordsForClient(String clientId) {
        if (clientId == null) return List.of();

        if ("ALL_CLIENTS".equals(clientId)) {
            List<ActivityRecord> all = new ArrayList<>();
            for (var activity : activityTracker.getAllActivities().values()) {
                all.addAll(activity.getRecords());
            }
            return all;
        }

        var activity = activityTracker.getActivity(clientId);
        return activity != null ? activity.getRecords() : List.of();
    }

    public void stop() {
        timeline.stop();
    }
}
