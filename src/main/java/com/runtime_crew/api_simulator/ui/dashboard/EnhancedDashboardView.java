package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.policy.*;
import com.runtime_crew.api_simulator.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class EnhancedDashboardView extends BorderPane {

    private final RequestLogger logger = new RequestLogger();
    private RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker = new ClientActivityTracker();
    private RateLimitAnalyzer analyzer;
    private final EnhancedReportGenerator reportGenerator = new EnhancedReportGenerator();
    private final DashboardActionHandler actionHandler;

    private final StatsPanel statsPanel = new StatsPanel();
    private final ControlPanelView controlPanel = new ControlPanelView(statsPanel);
    private final ActivityTableView activityTable = new ActivityTableView();
    private final TrafficChartView trafficChart;
    private final LogPanel logPanel = new LogPanel();
    private final Timeline statusTimer;
    private int burstCount = 20;
    private int burstDuration = 10;
    private int currentMaxRequests = 10;
    private int currentTimeWindow = 10;
    private int currentBlockDuration = 3;

    public EnhancedDashboardView() {
        enforcer = new RateLimitEnforcer(10, java.time.Duration.ofSeconds(10), java.time.Duration.ofSeconds(3), logger);
        analyzer = new RateLimitAnalyzer(List.of(
            new FixedWindowPolicy(5, java.time.Duration.ofSeconds(10)),
            new SlidingWindowPolicy(5, java.time.Duration.ofSeconds(10)),
            new BurstDetectionPolicy(4, java.time.Duration.ofSeconds(3)),
            new AbnormalPatternPolicy(3),
            new RetryAbusePolicy(8, java.time.Duration.ofSeconds(2))
        ));
        actionHandler = new DashboardActionHandler(logger, enforcer, activityTracker, analyzer, reportGenerator);
        trafficChart = new TrafficChartView(activityTracker, controlPanel::getSelectedClient);

        statusTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshClientViews()));
        statusTimer.setCycleCount(Timeline.INDEFINITE);
        statusTimer.play();

        initLayout();
        setupEvents();

        logPanel.append("API Rate-Limit & Abuse Simulator Started\n");
        logPanel.append("Advanced detection policies loaded\n");
        logPanel.append("System ready - Select a client or load a dataset to begin\n\n");
    }

    private void initLayout() {
        setTop(new TopBarView());

        ScrollPane controlScroll = new ScrollPane(controlPanel);
        controlScroll.setFitToWidth(true);
        controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        controlScroll.setMinWidth(270);
        controlScroll.setPrefWidth(280);

        VBox centerArea = new VBox(8, activityTable, trafficChart);
        HBox.setHgrow(centerArea, Priority.ALWAYS);
        VBox.setVgrow(activityTable, Priority.ALWAYS);
        VBox.setVgrow(trafficChart, Priority.SOMETIMES);

        setCenter(new HBox(15, controlScroll, centerArea));
        setBottom(logPanel);
        setPadding(new Insets(12));
    }

    private void setupEvents() {
        controlPanel.setOnClientSelected(this::refreshClientViews);
        controlPanel.setOnStatusFilter(this::handleFilterChange);
        controlPanel.setOnTypeFilter(this::handleFilterChange);
        controlPanel.setOnSendRequest((c, t) -> actionHandler.handleSendRequest(c, t, logPanel, this::refreshClientViews));
        controlPanel.setOnSimulateBurst(c -> actionHandler.handleSimulateBurst(c, logPanel, this::refreshClientViews));
        controlPanel.setOnLoadDataset(() -> actionHandler.handleLoadDataset(getScene().getWindow(), controlPanel, logPanel, this::refreshClientViews));
        controlPanel.setOnRegisterClient(() -> actionHandler.handleRegisterClient(getScene().getWindow(), controlPanel, logPanel));
        controlPanel.setOnFullReport(actionHandler::handleFullReport);
        controlPanel.setOnQuickReport(actionHandler::handleQuickReport);
        controlPanel.setOnCompare(actionHandler::handleCompareAll);
        controlPanel.setOnExport(c -> actionHandler.handleExport(c, getScene().getWindow(), logPanel));
        controlPanel.setOnClearHistory(this::handleClearHistory);
        controlPanel.setOnBarChart(this::handleBarChart);
        controlPanel.setOnSettings(this::handleSettings);
    }

    private void handleClearHistory(String clientId) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }

        if ("ALL_CLIENTS".equals(clientId)) {
            logger.clearAll();
            activityTracker.clearAll();
            logPanel.append("History cleared for ALL clients\n");
        } else {
            logger.clearClient(clientId);
            activityTracker.clearClient(clientId);
            logPanel.append("History cleared for " + clientId + "\n");
        }

        activityTable.clear();
        statsPanel.resetStats();
        controlPanel.resetQuota();
        controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
    }

    private void handleBarChart() {
        BarChartView barChartView = new BarChartView(activityTracker, controlPanel::getSelectedClient);

        Stage chartStage = new Stage();
        chartStage.initModality(Modality.NONE);
        chartStage.setTitle("Bar Chart - Request Distribution");

        Scene scene = new Scene(barChartView, 650, 380);
        scene.getStylesheets().add(
            getClass().getResource("/styles/main.css").toExternalForm()
        );
        chartStage.setScene(scene);
        chartStage.show();
    }

    private void handleSettings() {
        SettingsView settingsView = new SettingsView(currentMaxRequests, currentTimeWindow, currentBlockDuration, burstCount, burstDuration);

        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setTitle("Settings");

        settingsView.setOnSave(() -> applySettings(settingsView, settingsStage));

        Scene scene = new Scene(settingsView, 400, 380);
        scene.getStylesheets().add(
            getClass().getResource("/styles/main.css").toExternalForm()
        );
        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    private void applySettings(SettingsView settingsView, Stage settingsStage) {
        currentMaxRequests = settingsView.getMaxRequests();
        currentTimeWindow = settingsView.getTimeWindow();
        currentBlockDuration = settingsView.getBlockDuration();
        burstCount = settingsView.getBurstCount();
        burstDuration = settingsView.getBurstDuration();

        enforcer = new RateLimitEnforcer(currentMaxRequests, java.time.Duration.ofSeconds(currentTimeWindow), java.time.Duration.ofSeconds(currentBlockDuration), logger);
        analyzer = new RateLimitAnalyzer(List.of(
            new FixedWindowPolicy(currentMaxRequests, java.time.Duration.ofSeconds(currentTimeWindow)),
            new SlidingWindowPolicy(currentMaxRequests, java.time.Duration.ofSeconds(currentTimeWindow)),
            new BurstDetectionPolicy(4, java.time.Duration.ofSeconds(3)),
            new AbnormalPatternPolicy(3),
            new RetryAbusePolicy(8, java.time.Duration.ofSeconds(2))
        ));

        actionHandler.updateEnforcer(enforcer);
        actionHandler.updateAnalyzer(analyzer);
        actionHandler.updateBurstConfig(burstCount, burstDuration);

        settingsView.showSuccess("Settings saved successfully!");
        controlPanel.updateBurstLabel(burstCount, burstDuration);
        logPanel.append(String.format("Settings saved: Max=%d, Window=%ds, Block=%ds, Burst=%d/%ds\n",
            currentMaxRequests, currentTimeWindow, currentBlockDuration, burstCount, burstDuration));
        refreshClientViews();

        javafx.animation.Timeline closeTimer = new javafx.animation.Timeline(
            new KeyFrame(Duration.seconds(1), e -> settingsStage.close())
        );
        closeTimer.play();
    }

    private void refreshClientViews() {
        refreshClientViews(controlPanel.getSelectedClient());
    }

    private void refreshClientViews(String clientId) {
        if (clientId == null) {
            controlPanel.resetQuota();
            statsPanel.resetStats();
            controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
            controlPanel.clearBlockCountdown();
            activityTable.clear();
            trafficChart.updateData();
            return;
        }

        if ("ALL_CLIENTS".equals(clientId)) {
            var allActivities = activityTracker.getAllActivities();
            if (allActivities.isEmpty()) {
                controlPanel.resetQuota();
                statsPanel.resetStats();
                controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
                activityTable.clear();
                return;
            }
            List<ClientActivityTracker.ActivityRecord> allRecords = new java.util.ArrayList<>();
            int totalReqs = 0, totalAllowed = 0, totalBlocked = 0;
            int totalQuota = 0, totalMax = 0;
            for (var activity : allActivities.values()) {
                allRecords.addAll(activity.getRecords());
                totalReqs += activity.getTotalRequests();
                totalAllowed += activity.getAllowedRequests();
                totalBlocked += activity.getBlockedRequests();
                totalQuota += enforcer.getRemainingQuota(activity.getClientId());
                totalMax += enforcer.getMaxRequests();
            }
            activityTable.setAllRecords(allRecords);
            applyFilters();
            statsPanel.updateStats(totalReqs, totalAllowed, totalBlocked);
            controlPanel.updateQuota(totalQuota, totalMax);

            ViolationLevel worstLevel = ViolationLevel.NORMAL;
            for (var activity : allActivities.values()) {
                RequestLog log = logger.getLog(activity.getClientId());
                if (log != null && !log.getRequests().isEmpty()) {
                    ViolationLevel level = analyzer.analyze(log).getLevel();
                    if (level.isMoreSevereThan(worstLevel)) {
                        worstLevel = level;
                    }
                }
            }
            controlPanel.updateRiskLevel(worstLevel);

            long maxBlockRemaining = 0;
            for (var activity : allActivities.values()) {
                if (enforcer.getRemainingQuota(activity.getClientId()) == 0) {
                    long blockTime = enforcer.getTimeUntilReset(activity.getClientId()).getSeconds();
                    if (blockTime > maxBlockRemaining) {
                        maxBlockRemaining = blockTime;
                    }
                }
            }
            if (maxBlockRemaining > 0) {
                controlPanel.updateBlockCountdown(maxBlockRemaining);
            } else {
                controlPanel.clearBlockCountdown();
            }

            trafficChart.updateData();
            return;
        }

        int remaining = enforcer.getRemainingQuota(clientId);
        controlPanel.updateQuota(remaining, enforcer.getMaxRequests());
        statsPanel.updateStats(activityTracker.getActivity(clientId));

        if (remaining == 0) {
            long blockRemaining = enforcer.getTimeUntilReset(clientId).getSeconds();
            controlPanel.updateBlockCountdown(blockRemaining);
        } else {
            controlPanel.clearBlockCountdown();
        }

        RequestLog log = logger.getLog(clientId);
        ViolationLevel level = (log != null && !log.getRequests().isEmpty()) ? analyzer.analyze(log).getLevel() : ViolationLevel.NORMAL;
        controlPanel.updateRiskLevel(level);

        var activity = activityTracker.getActivity(clientId);
        activityTable.setRecords(activity != null ? activity.getRecords() : null);
        applyFilters();
        trafficChart.updateData();
    }

    private void handleFilterChange(Object ignored) {
        refreshClientViews();
    }

    private void applyFilters() {
        activityTable.setStatusFilter(controlPanel.getSelectedStatus());
        activityTable.setTypeFilter(controlPanel.getSelectedType());
    }
}
