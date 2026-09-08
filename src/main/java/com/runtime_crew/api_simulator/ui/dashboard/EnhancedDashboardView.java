package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.policy.*;
import com.runtime_crew.api_simulator.service.*;
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
    private boolean showingAllClients = false;

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

        if ("ALL_CLIENTS".equals(clientId)) {
            showingAllClients = true;
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
            for (var activity : allActivities.values()) {
                allRecords.addAll(activity.getRecords());
                totalReqs += activity.getTotalRequests();
                totalAllowed += activity.getAllowedRequests();
                totalBlocked += activity.getBlockedRequests();
            }
            activityTable.setAllRecords(allRecords);
            applyFilters();
            statsPanel.updateStats(totalReqs, totalAllowed, totalBlocked);
            controlPanel.resetQuota();
            controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
            return;
        }

        showingAllClients = false;
        controlPanel.updateQuota(enforcer.getRemainingQuota(clientId), enforcer.getMaxRequests());
        statsPanel.updateStats(activityTracker.getActivity(clientId));

        RequestLog log = logger.getLog(clientId);
        ViolationLevel level = (log != null && !log.getRequests().isEmpty()) ? analyzer.analyze(log).getLevel() : ViolationLevel.NORMAL;
        controlPanel.updateRiskLevel(level);

        var activity = activityTracker.getActivity(clientId);
        activityTable.setRecords(activity != null ? activity.getRecords() : null);
        applyFilters();
    }

    private void handleFilterChange(Object ignored) {
        refreshClientViews();
    }

    private void applyFilters() {
        activityTable.setStatusFilter(controlPanel.getSelectedStatus());
        activityTable.setTypeFilter(controlPanel.getSelectedType());
    }
}
