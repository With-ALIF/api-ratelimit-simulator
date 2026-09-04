package com.async_alpha.api_simulator.ui.dashboard;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.policy.*;
import com.async_alpha.api_simulator.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.Duration;
import java.util.List;

public class EnhancedDashboardView extends BorderPane {

    private final RequestLogger logger = new RequestLogger();
    private final RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker = new ClientActivityTracker();
    private final RateLimitAnalyzer analyzer;
    private final EnhancedReportGenerator reportGenerator = new EnhancedReportGenerator();
    private final DashboardActionHandler actionHandler;

    private final StatsPanel statsPanel = new StatsPanel();
    private final ControlPanelView controlPanel = new ControlPanelView(statsPanel);
    private final ActivityTableView activityTable = new ActivityTableView();
    private final LogPanel logPanel = new LogPanel();
    private Timeline quotaRefreshTimeline;

    public EnhancedDashboardView() {
        enforcer = new RateLimitEnforcer(5, Duration.ofSeconds(10), logger);
        analyzer = new RateLimitAnalyzer(List.of(
            new FixedWindowPolicy(5, Duration.ofSeconds(10)),
            new SlidingWindowPolicy(5, Duration.ofSeconds(10)),
            new BurstDetectionPolicy(4, Duration.ofSeconds(3)),
            new AbnormalPatternPolicy(3),
            new RetryAbusePolicy(8, Duration.ofSeconds(2))
        ));
        actionHandler = new DashboardActionHandler(logger, enforcer, activityTracker, analyzer, reportGenerator);

        initLayout();
        setupEvents();

        quotaRefreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            String selected = controlPanel.getSelectedClient();
            if (selected != null) {
                controlPanel.updateQuota(enforcer.getRemainingQuota(selected), enforcer.getMaxRequests());

                RequestLog log = logger.getLog(selected);
                ViolationLevel level = (log != null && !log.getRequests().isEmpty()) ? analyzer.analyze(log).getLevel() : ViolationLevel.NORMAL;
                controlPanel.updateRiskLevel(level);
            }
        }));
        quotaRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        quotaRefreshTimeline.play();

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

        HBox.setHgrow(activityTable, Priority.ALWAYS);
        setCenter(new HBox(15, controlScroll, activityTable));
        setBottom(logPanel);
        setPadding(new Insets(12));
    }

    private void setupEvents() {
        controlPanel.setOnClientSelected(this::refreshClientViews);
        controlPanel.setOnSendRequest((c, t) -> actionHandler.handleSendRequest(c, t, logPanel, this::refreshClientViews));
        controlPanel.setOnSimulateBurst(c -> actionHandler.handleSimulateBurst(c, logPanel, this::refreshClientViews));
        controlPanel.setOnLoadDataset(() -> actionHandler.handleLoadDataset(getScene().getWindow(), controlPanel, logPanel, this::refreshClientViews));
        controlPanel.setOnFullReport(actionHandler::handleFullReport);
        controlPanel.setOnQuickReport(c -> actionHandler.handleQuickReport(c, logPanel));
        controlPanel.setOnCompare(actionHandler::handleCompareAll);
        controlPanel.setOnExport(c -> actionHandler.handleExport(c, getScene().getWindow(), logPanel));
        controlPanel.setOnClearHistory(this::handleClearHistory);
    }

    private void handleClearHistory(String clientId) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }
        logger.clearClient(clientId);
        activityTracker.clearClient(clientId);
        logPanel.append("History cleared for " + clientId + "\n");
        activityTable.clear();
        statsPanel.resetStats();
        controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
    }

    private void refreshClientViews() {
        refreshClientViews(controlPanel.getSelectedClient());
    }

    private void refreshClientViews(String clientId) {
        if (clientId == null) {
            controlPanel.resetQuota();
            statsPanel.resetStats();
            controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
            activityTable.clear();
            return;
        }

        controlPanel.updateQuota(enforcer.getRemainingQuota(clientId), enforcer.getMaxRequests());
        statsPanel.updateStats(activityTracker.getActivity(clientId));

        RequestLog log = logger.getLog(clientId);
        ViolationLevel level = (log != null && !log.getRequests().isEmpty()) ? analyzer.analyze(log).getLevel() : ViolationLevel.NORMAL;
        controlPanel.updateRiskLevel(level);

        var activity = activityTracker.getActivity(clientId);
        activityTable.setRecords(activity != null ? activity.getRecords() : null);
    }
}
