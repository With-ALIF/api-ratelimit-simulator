package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.service.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public class DashboardActionHandler {

    private final RequestLogger logger;
    private RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker;
    private RateLimitAnalyzer analyzer;
    private final EnhancedReportGenerator reportGenerator;
    private final DatasetLoader datasetLoader = new DatasetLoader();
    private final BurstTrafficGenerator burstGenerator = new BurstTrafficGenerator();
    private final Random random = new Random();
    private int burstCount = 20;
    private int burstDuration = 10;

    public DashboardActionHandler(RequestLogger logger, RateLimitEnforcer enforcer,
                                  ClientActivityTracker activityTracker, RateLimitAnalyzer analyzer,
                                  EnhancedReportGenerator reportGenerator) {
        this.logger = logger;
        this.enforcer = enforcer;
        this.activityTracker = activityTracker;
        this.analyzer = analyzer;
        this.reportGenerator = reportGenerator;
    }

    public void handleSendRequest(String clientId, RequestType type, LogPanel logPanel, Runnable onUpdated) {
        if (clientId == null) {
            ReportDialogHelper.showAlert("Please select a client!");
            return;
        }

        if (type == null) {
            RequestType[] types = RequestType.values();
            type = types[random.nextInt(types.length)];
        }

        if ("ALL_CLIENTS".equals(clientId)) {
            for (String cid : new String[]{"CLIENT_A", "CLIENT_B", "CLIENT_C", "CLIENT_D"}) {
                ServiceRequest req = new ServiceRequest(cid, type, LocalDateTime.now());
                RateLimitEnforcer.RequestResult result = enforcer.processRequest(req);
                activityTracker.trackRequest(req, result.isBlocked());
                String icon = result.isBlocked() ? "⛔" : "✅";
                logPanel.append(String.format("%s [%s] %s - %s\n",
                    icon, req.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    cid, result.isBlocked() ? "BLOCKED" : "ALLOWED"));
            }
            onUpdated.run();
            return;
        }

        ServiceRequest req = new ServiceRequest(clientId, type, LocalDateTime.now());
        RateLimitEnforcer.RequestResult result = enforcer.processRequest(req);
        activityTracker.trackRequest(req, result.isBlocked());
        onUpdated.run();

        String icon = result.isBlocked() ? "⛔" : "✅";
        logPanel.append(String.format("%s [%s] %s - %s | Quota: %d/%d%s\n",
            icon, req.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            req.getClientId(), result.isBlocked() ? "BLOCKED" : "ALLOWED",
            result.getRemainingQuota(), enforcer.getMaxRequests(),
            result.isBlocked() ? " ⚠️ RATE LIMIT EXCEEDED" : ""));
    }

    public void handleSimulateBurst(String clientId, LogPanel logPanel, Runnable onUpdated) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }

        if ("ALL_CLIENTS".equals(clientId)) {
            int totalAllowed = 0, totalBlocked = 0;
            logPanel.append(String.format("\n⚡ Burst simulation for ALL clients (%d req each, %ds)...\n", burstCount, burstDuration));
            for (String cid : new String[]{"CLIENT_A", "CLIENT_B", "CLIENT_C", "CLIENT_D"}) {
                List<ServiceRequest> burst = burstGenerator.generateBurst(cid, burstCount, Duration.ofSeconds(burstDuration));
                int allowed = 0, blocked = 0;
                for (ServiceRequest req : burst) {
                    RateLimitEnforcer.RequestResult res = enforcer.processRequest(req);
                    activityTracker.trackRequest(req, res.isBlocked());
                    if (res.isBlocked()) blocked++; else allowed++;
                }
                totalAllowed += allowed;
                totalBlocked += blocked;
                logPanel.append(String.format("  %s - Allowed: %d | Blocked: %d\n", cid, allowed, blocked));
            }
            onUpdated.run();
            logPanel.append(String.format("⚡ Burst Done! Total: %d | Allowed: %d | Blocked: %d\n\n", burstCount * 4, totalAllowed, totalBlocked));
            return;
        }

        List<ServiceRequest> burst = burstGenerator.generateBurst(clientId, burstCount, Duration.ofSeconds(burstDuration));
        int allowed = 0, blocked = 0;
        logPanel.append(String.format("\n⚡ Burst simulation: %d requests in %ds for %s...\n", burstCount, burstDuration, clientId));

        for (ServiceRequest req : burst) {
            RateLimitEnforcer.RequestResult res = enforcer.processRequest(req);
            activityTracker.trackRequest(req, res.isBlocked());
            if (res.isBlocked()) blocked++; else allowed++;
            logPanel.append(String.format("%s [%s] %s %s - %s\n",
                res.isBlocked() ? "⛔" : "✅", req.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                req.getClientId(), req.getRequestType(), res.isBlocked() ? "BLOCKED" : "ALLOWED"));
        }
        onUpdated.run();
        logPanel.append(String.format("⚡ Burst Done! Total: %d | Allowed: %d | Blocked: %d\n\n", burstCount, allowed, blocked));
    }

    public void handleLoadDataset(Window window, ControlPanelView controlPanel, LogPanel logPanel, Runnable onUpdated) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Predefined Dataset (.txt or .csv)");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Dataset Files (*.txt, *.csv)", "*.txt", "*.csv"), new FileChooser.ExtensionFilter("All Files", "*.*"));
        File sampleDir = new File("sample_data");
        if (sampleDir.exists() && sampleDir.isDirectory()) fc.setInitialDirectory(sampleDir);

        File file = fc.showOpenDialog(window);
        if (file == null) return;
        try {
            DatasetLoader.LoadResult result = datasetLoader.loadFromFile(file);
            if (result.getRequests().isEmpty()) { ReportDialogHelper.showAlert("No valid request records found in file!"); return; }
            int allowed = 0, blocked = 0;
            logPanel.append("\n📂 Loading dataset: " + file.getName() + " (" + result.getRequests().size() + " records)\n");
            for (ServiceRequest req : result.getRequests()) {
                controlPanel.addClientIfAbsent(req.getClientId());
                RateLimitEnforcer.RequestResult res = enforcer.processRequest(req);
                activityTracker.trackRequest(req, res.isBlocked());
                if (res.isBlocked()) blocked++; else allowed++;
                logPanel.append(String.format("%s [%s] %s %s - %s\n",
                    res.isBlocked() ? "⛔" : "✅", req.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    req.getClientId(), req.getRequestType(), res.isBlocked() ? "BLOCKED" : "ALLOWED"));
            }
            if (controlPanel.getSelectedClient() == null && !result.getRequests().isEmpty()) controlPanel.setSelectedClient(result.getRequests().get(0).getClientId());
            onUpdated.run();
            logPanel.append(String.format("✅ Complete! Total: %d | Allowed: %d | Blocked: %d\n\n", result.getRequests().size(), allowed, blocked));
        } catch (Exception ex) { ReportDialogHelper.showAlert("Failed to load dataset: " + ex.getMessage()); }
    }

    public void handleFullReport(String clientId) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }

        if ("ALL_CLIENTS".equals(clientId)) {
            var allActivities = activityTracker.getAllActivities();
            if (allActivities.isEmpty()) { ReportDialogHelper.showAlert("No data available!"); return; }
            String report = reportGenerator.generateAllClientsFullReport(allActivities);
            ReportDialogHelper.showReport("Full Violation Report - ALL CLIENTS", report);
            return;
        }

        RequestLog log = logger.getLog(clientId);
        AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        ReportDialogHelper.showReport("Full Violation Report - " + clientId, reportGenerator.generateViolationReport(abuse, log, activityTracker.getActivity(clientId)));
    }

    public void handleQuickReport(String clientId) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }

        if ("ALL_CLIENTS".equals(clientId)) {
            var allActivities = activityTracker.getAllActivities();
            if (allActivities.isEmpty()) { ReportDialogHelper.showAlert("No data available!"); return; }
            int totalReqs = 0, totalAllowed = 0, totalBlocked = 0;
            for (var activity : allActivities.values()) {
                totalReqs += activity.getTotalRequests();
                totalAllowed += activity.getAllowedRequests();
                totalBlocked += activity.getBlockedRequests();
            }
            double rate = totalReqs == 0 ? 100.0 : (totalAllowed * 100.0) / totalReqs;
            ReportDialogHelper.showReport("Quick Summary - ALL CLIENTS",
                reportGenerator.generateAllClientsUsageReport(totalReqs, totalAllowed, totalBlocked, rate, allActivities));
            return;
        }

        RequestLog log = logger.getLog(clientId);
        AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        ReportDialogHelper.showReport("Quick Summary - " + clientId, reportGenerator.generateUsageReport(clientId, activityTracker.getActivity(clientId), log, abuse));
    }

    public void handleCompareAll() {
        if (activityTracker.getAllActivities().isEmpty()) { ReportDialogHelper.showAlert("No client data available for comparison!"); return; }
        ReportDialogHelper.showComparisonTable("Multi-Client Comparison Report", activityTracker.getAllActivities());
    }

    public void handleExport(String clientId, Window window, LogPanel logPanel) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }

        if ("ALL_CLIENTS".equals(clientId)) {
            var allActivities = activityTracker.getAllActivities();
            if (allActivities.isEmpty()) { ReportDialogHelper.showAlert("No data available!"); return; }
            String report = reportGenerator.generateAllClientsFullReport(allActivities);
            String fileName = "ALL_CLIENTS_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            File file = ReportDialogHelper.chooseExportFile(window, fileName);
            if (file != null) {
                try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                    writer.write(report);
                    logPanel.append("Report exported to: " + file.getName() + "\n");
                } catch (Exception e) { ReportDialogHelper.showAlert("Error exporting report: " + e.getMessage()); }
            }
            return;
        }

        RequestLog log = logger.getLog(clientId);
        AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        String report = reportGenerator.generateViolationReport(abuse, log, activityTracker.getActivity(clientId));
        String fileName = clientId + "_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        File file = ReportDialogHelper.chooseExportFile(window, fileName);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(report);
                logPanel.append("Report exported to: " + file.getName() + "\n");
            } catch (Exception e) { ReportDialogHelper.showAlert("Error exporting report: " + e.getMessage()); }
        }
    }

    public void handleRegisterClient(Window window, ControlPanelView controlPanel, LogPanel logPanel) {
        Dialog<Client> dialog = new Dialog<>();
        if (window != null) dialog.initOwner(window);
        dialog.setTitle("Register API Client");
        dialog.setHeaderText("Register a new API Client");

        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 30, 10, 10));

        TextField clientIdField = new TextField();
        clientIdField.setPromptText("e.g. CLIENT_E");

        grid.add(new Label("Client ID:"), 0, 0);
        grid.add(clientIdField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                String id = clientIdField.getText() != null ? clientIdField.getText().trim() : "";
                if (!id.isEmpty()) {
                    return new Client(id, id);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(client -> {
            activityTracker.registerClient(client);
            controlPanel.addClientIfAbsent(client.getClientId());
            controlPanel.setSelectedClient(client.getClientId());
            logPanel.append(String.format("Registered new API client: %s (%s)\n", client.getClientId(), client.getName()));
        });
    }

    public void updateEnforcer(RateLimitEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    public void updateAnalyzer(RateLimitAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void updateBurstConfig(int burstCount, int burstDuration) {
        this.burstCount = burstCount;
        this.burstDuration = burstDuration;
    }
}
