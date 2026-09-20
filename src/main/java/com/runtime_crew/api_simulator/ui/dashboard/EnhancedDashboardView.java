package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.multiservice.model.ApiService;
import com.runtime_crew.api_simulator.multiservice.service.MultiServiceSimulator;
import com.runtime_crew.api_simulator.multiservice.service.ServiceRegistry;
import com.runtime_crew.api_simulator.multiservice.ui.ServiceManagementDialog;
import com.runtime_crew.api_simulator.multiservice.ui.ServiceSelectorView;
import com.runtime_crew.api_simulator.policy.*;
import com.runtime_crew.api_simulator.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
    private final RequestLogService requestLogService = new RequestLogService();
    private final RequestLogService multiRequestLogService = new RequestLogService(java.nio.file.Path.of("data", "multi_requests.csv"));
    private final ClientRegistryService clientRegistry = new ClientRegistryService();
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

    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private MultiServiceSimulator multiSimulator;
    private ServiceSelectorView serviceSelectorView;
    private VBox serviceSelectorBox;

    public EnhancedDashboardView() {
        enforcer = new RateLimitEnforcer(10, java.time.Duration.ofSeconds(10), java.time.Duration.ofSeconds(3), logger);
        analyzer = new RateLimitAnalyzer(List.of(
            new FixedWindowPolicy(5, java.time.Duration.ofSeconds(10)),
            new SlidingWindowPolicy(5, java.time.Duration.ofSeconds(10)),
            new BurstDetectionPolicy(4, java.time.Duration.ofSeconds(3)),
            new AbnormalPatternPolicy(3),
            new RetryAbusePolicy(8, java.time.Duration.ofSeconds(2))
        ));
        actionHandler = new DashboardActionHandler(logger, enforcer, activityTracker, analyzer, reportGenerator, requestLogService, clientRegistry);
        trafficChart = new TrafficChartView(activityTracker, controlPanel::getSelectedClient);

        multiSimulator = new MultiServiceSimulator(logger, enforcer, activityTracker, analyzer, multiRequestLogService);
        serviceSelectorView = new ServiceSelectorView(serviceRegistry);

        statusTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshClientViews()));
        statusTimer.setCycleCount(Timeline.INDEFINITE);
        statusTimer.play();

        initLayout();
        setupEvents();
        loadFromCsv();

        logPanel.append("API Rate-Limit & Abuse Simulator Started\n");
        logPanel.append("Advanced detection policies loaded\n");
        logPanel.append("System ready - Select a client or load a dataset to begin\n\n");
    }

    private void initLayout() {
        setTop(new TopBarView());

        Button manageServicesBtn = new Button("Manage Services");
        manageServicesBtn.setMaxWidth(Double.MAX_VALUE);
        manageServicesBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: #ffffff; -fx-font-size: 11px; " +
                "-fx-padding: 4 8; -fx-background-radius: 4;");
        manageServicesBtn.setOnAction(e -> {
            ServiceManagementDialog dialog = new ServiceManagementDialog(serviceRegistry);
            dialog.setOnServicesChanged(() -> {
                serviceSelectorView.refreshCheckboxes();
                loadServicesIntoDropdown();
            });
            dialog.show((Stage) getScene().getWindow());
        });

        serviceSelectorBox = new VBox(6,
            new Label("Multi-Service Mode") {{
                setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a78bfa;");
            }},
            serviceSelectorView,
            manageServicesBtn
        );
        serviceSelectorBox.setPadding(new Insets(8));
        serviceSelectorBox.setStyle("-fx-background-color: #1a1f35; -fx-background-radius: 8; " +
                "-fx-border-color: #6366f1; -fx-border-radius: 8; -fx-border-width: 1;");
        serviceSelectorBox.setVisible(false);
        serviceSelectorBox.setManaged(false);

        VBox leftPanel = new VBox(6, controlPanel, serviceSelectorBox);
        ScrollPane controlScroll = new ScrollPane(leftPanel);
        controlScroll.setFitToWidth(true);
        controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        controlScroll.setMinWidth(310);
        controlScroll.setPrefWidth(310);

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
        controlPanel.setOnSendRequest((c, t) -> {
            if (isMultiServiceMode()) {
                handleMultiServiceSendRequest(c, t);
            } else {
                actionHandler.handleSendRequest(c, t, logPanel, this::refreshClientViews);
            }
        });
        controlPanel.setOnSimulateBurst(c -> {
            if (isMultiServiceMode()) {
                handleMultiServiceBurst(c);
            } else {
                actionHandler.handleSimulateBurst(c, logPanel, this::refreshClientViews);
            }
        });
        controlPanel.setOnLoadDataset(() -> actionHandler.handleLoadDataset(getScene().getWindow(), controlPanel, logPanel, this::refreshClientViews));
        controlPanel.setOnRegisterClient(() -> actionHandler.handleRegisterClient(getScene().getWindow(), controlPanel, logPanel));
        controlPanel.setOnFullReport(c -> actionHandler.handleFullReport(c, isMultiServiceMode()));
        controlPanel.setOnQuickReport(c -> actionHandler.handleQuickReport(c, isMultiServiceMode()));
        controlPanel.setOnCompare(actionHandler::handleCompareAll);
        controlPanel.setOnExport(c -> actionHandler.handleExport(c, getScene().getWindow(), logPanel, isMultiServiceMode()));
        controlPanel.setOnClearHistory(this::handleClearHistory);
        controlPanel.setOnDeleteClient(this::handleDeleteClient);
        controlPanel.setOnRenameClient(this::handleRenameClient);
        controlPanel.setOnBarChart(this::handleBarChart);
        controlPanel.setOnSettings(this::handleSettings);
        controlPanel.setOnClose(this::handleClose);
        controlPanel.setOnMultiService(this::handleMultiService);
    }

    private void handleClearHistory(String clientId) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }

        if ("ALL_CLIENTS".equals(clientId)) {
            logger.clearAll();
            activityTracker.clearAll();
            requestLogService.clearAll();
            logPanel.append("History cleared for ALL clients\n");
        } else {
            logger.clearClient(clientId);
            activityTracker.clearClient(clientId);
            requestLogService.removeClient(clientId);
            logPanel.append("History cleared for " + clientId + "\n");
        }

        activityTable.clear();
        statsPanel.resetStats();
        controlPanel.resetQuota();
        controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
    }

    private void handleDeleteClient(String clientId) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }
        if ("ALL_CLIENTS".equals(clientId)) { ReportDialogHelper.showAlert("Cannot delete ALL_CLIENTS!"); return; }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Client");
        alert.setHeaderText("Are you sure?");
        String msg = clientRegistry.isDefaultClient(clientId)
                ? clientId + " is a default client. The program resets to its initial state whenever it is restarted.\nDelete anyway?"
                : "Delete \"" + clientId + "\" and all its logs permanently?";
        alert.setContentText(msg);
        alert.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            logger.clearClient(clientId);
            activityTracker.clearClient(clientId);
            requestLogService.removeClient(clientId);
            clientRegistry.removeClient(clientId);
            controlPanel.removeClientFromBox(clientId);
            activityTable.clear();
            statsPanel.resetStats();
            controlPanel.resetQuota();
            controlPanel.updateRiskLevel(ViolationLevel.NORMAL);
            logPanel.append("Deleted client: " + clientId + "\n");
        });
    }

    private void handleRenameClient(String oldId) {
        if (oldId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }
        if ("ALL_CLIENTS".equals(oldId)) { ReportDialogHelper.showAlert("Cannot rename ALL_CLIENTS!"); return; }

        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(oldId);
        dialog.setTitle("Rename Client");
        dialog.setHeaderText("Rename: " + oldId);
        dialog.setContentText("New name:");
        dialog.showAndWait().ifPresent(newId -> {
            newId = newId.trim();
            if (newId.isEmpty() || newId.equals(oldId)) return;

            requestLogService.moveClientLogs(oldId, newId);
            logger.clearClient(oldId);
            activityTracker.clearClient(oldId);

            List<CsvRequestLogEntry> entries = requestLogService.loadAll();
            for (CsvRequestLogEntry entry : entries) {
                if (entry.clientId.equals(newId)) {
                    ServiceRequest req = new ServiceRequest(newId, entry.getRequestType(), entry.timestamp);
                    logger.logRequest(req);
                    activityTracker.trackRequest(req, entry.isBlocked());
                }
            }

            if (clientRegistry.isDefaultClient(oldId)) {
                controlPanel.removeClientFromBox(newId);
                logPanel.append("Renamed (temporary): " + oldId + " -> " + newId + " (resets on restart)\n");
            } else {
                clientRegistry.renameClient(oldId, newId);
                logPanel.append("Renamed: " + oldId + " -> " + newId + "\n");
            }

            controlPanel.renameClientInBox(oldId, newId);
            refreshClientViews();
        });
    }

    private void loadFromCsv() {
        List<String> savedClients = clientRegistry.loadAll();
        for (String cid : savedClients) {
            controlPanel.addClientIfAbsent(cid);
        }
        if (!savedClients.isEmpty()) {
            logPanel.append(String.format("Loaded %d saved clients from data/clients.csv\n", savedClients.size()));
        }

        loadServicesIntoDropdown();

        // Load single-service records
        List<CsvRequestLogEntry> entries = requestLogService.loadAll();
        int loaded = 0;
        for (CsvRequestLogEntry entry : entries) {
            if (entry.clientId == null) continue;
            ServiceRequest req = new ServiceRequest(entry.clientId, entry.getRequestType(), entry.timestamp);
            logger.logRequest(req);
            activityTracker.trackRequest(req, entry.isBlocked());
            controlPanel.addClientIfAbsent(entry.clientId);
            loaded++;
        }
        if (loaded > 0) {
            logPanel.append(String.format("Loaded %d records from data/requests.csv\n", loaded));
        } else {
            logPanel.append("No previous data found in data/requests.csv\n");
        }

        // Load multi-service records
        List<CsvRequestLogEntry> multiEntries = multiRequestLogService.loadAll();
        int multiLoaded = 0;
        for (CsvRequestLogEntry entry : multiEntries) {
            if (entry.clientId == null) continue;
            // serviceId holds the service name (e.g. "telegram"), clientId holds the real client
            ServiceRequest req = new ServiceRequest(
                entry.clientId,
                (entry.serviceId != null && !entry.serviceId.isEmpty()) ? entry.serviceId : null,
                entry.getRequestType(),
                entry.timestamp
            );
            activityTracker.trackRequest(req, entry.isBlocked());
            controlPanel.addClientIfAbsent(entry.clientId);
            multiLoaded++;
        }
        if (multiLoaded > 0) {
            logPanel.append(String.format("Loaded %d multi-service records from data/multi_requests.csv\n", multiLoaded));
        }

        if (loaded > 0 || multiLoaded > 0) {
            controlPanel.setSelectedClient("ALL_CLIENTS");
            refreshClientViews();
        }
    }


    private void loadServicesIntoDropdown() {
        List<ApiService> services = serviceRegistry.getAll();
        java.util.List<String> serviceNames = new java.util.ArrayList<>();
        for (ApiService svc : services) {
            serviceNames.add(svc.getName());
        }
        controlPanel.refreshServiceList(serviceNames);
        if (!serviceNames.isEmpty()) {
            logPanel.append(String.format("Loaded %d services from data/api_services.csv\n", serviceNames.size()));
        }
    }

    private void handleBarChart() {
        BarChartView barChartView = new BarChartView(activityTracker, controlPanel::getSelectedClient);

        Stage chartStage = new Stage();
        chartStage.initModality(Modality.NONE);
        chartStage.setTitle("Bar Chart - Request Distribution");
        chartStage.setMinWidth(600);
        chartStage.setMinHeight(350);

        Scene scene = new Scene(barChartView, 650, 380);
        scene.getStylesheets().add(
            getClass().getResource("/styles/main.css").toExternalForm()
        );
        chartStage.setScene(scene);
        chartStage.centerOnScreen();
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
        settingsStage.setMinWidth(400);
        settingsStage.setMinHeight(380);
        settingsStage.setScene(scene);
        settingsStage.centerOnScreen();
        settingsStage.showAndWait();
    }

    private void handleClose() {
        javafx.application.Platform.exit();
    }

    private void handleMultiService() {
        boolean visible = serviceSelectorBox.isVisible();
        serviceSelectorBox.setVisible(!visible);
        serviceSelectorBox.setManaged(!visible);
        activityTable.setServiceColumnVisible(!visible);
        activityTable.setMultiServiceMode(!visible);
        if (!visible) {
            controlPanel.setMultiServiceButtonText("Single-Service Mode");
            logPanel.append("Multi-Service Mode ENABLED - Select services and send requests\n");
        } else {
            controlPanel.setMultiServiceButtonText("Multi-Service Mode");
            logPanel.append("Multi-Service Mode DISABLED\n");
        }
        refreshClientViews();
    }

    private boolean isMultiServiceMode() {
        return serviceSelectorBox.isVisible();
    }

    private List<String> getSelectedServiceIds() {
        return serviceSelectorView.getSelectedServiceIds();
    }

    private List<String> resolveClientsForRequest(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return List.of("WebApp");
        }
        if ("ALL_CLIENTS".equals(clientId)) {
            List<String> clients = clientRegistry.loadAll();
            if (clients.isEmpty()) {
                return List.of("SuspiciousBot", "WebApp", "PartnerAPI");
            }
            return clients;
        }
        return List.of(clientId);
    }

    private void handleMultiServiceSendRequest(String clientId, RequestType type) {
        List<String> selected = getSelectedServiceIds();
        if (selected.isEmpty()) {
            logPanel.append("[MultiService] No services selected!\n");
            return;
        }

        if (type == null) {
            RequestType[] types = RequestType.values();
            type = types[new java.util.Random().nextInt(types.length)];
        }

        List<String> clients = resolveClientsForRequest(clientId);
        for (String client : clients) {
            logPanel.append(String.format("\n✉️ [%s] Sending %s request to %d service(s)...\n", client, type, selected.size()));

            var results = multiSimulator.sendRequest(client, selected, type);
            int allowed = 0, blocked = 0;

            for (var entry : results.entrySet()) {
                var r = entry.getValue();
                String icon = r.isBlocked() ? "⛔" : "✅";
                String time = r.request.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                String eventTypeLabel = r.eventType != null ? r.eventType.getDisplayName() : "UNKNOWN";

                String line = String.format("%s [%s] %s -> %s | %s | Quota: %d/%d | Risk: %d | %s\n",
                        icon, time, client, entry.getKey(), eventTypeLabel,
                        r.getRemainingQuota(), multiSimulator.getMaxRequests(),
                        r.riskScore, r.reason);
                logPanel.append(line);

                if (r.isBlocked()) blocked++; else allowed++;
            }

            logPanel.append(String.format("✅ [%s] Sent! Allowed: %d | Blocked: %d\n\n", client, allowed, blocked));
        }
        refreshClientViews();
    }

    private void handleMultiServiceBurst(String clientId) {
        List<String> selected = getSelectedServiceIds();
        if (selected.isEmpty()) {
            logPanel.append("[MultiService] No services selected!\n");
            return;
        }

        List<String> clients = resolveClientsForRequest(clientId);
        for (String client : clients) {
            logPanel.append(String.format("\n⚡ [%s] Burst simulation: %d requests over %ds for %d service(s)...\n",
                    client, burstCount, burstDuration, selected.size()));

            var results = multiSimulator.simulateBurst(client, selected, burstCount, java.time.Duration.ofSeconds(burstDuration));
            int totalAllowed = 0, totalBlocked = 0;

            for (var entry : results.entrySet()) {
                var r = entry.getValue();
                logPanel.append(String.format("  %s -> %s - Allowed: %d | Blocked: %d\n", client, r.serviceId, r.allowed, r.blocked));
                totalAllowed += r.allowed;
                totalBlocked += r.blocked;
            }

            int total = burstCount * selected.size();
            logPanel.append(String.format("⚡ [%s] Burst Done! Total: %d | Allowed: %d | Blocked: %d\n\n",
                    client, total, totalAllowed, totalBlocked));
        }
        refreshClientViews();
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
