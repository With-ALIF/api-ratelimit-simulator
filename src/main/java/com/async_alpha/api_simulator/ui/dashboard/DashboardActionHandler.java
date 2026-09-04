package com.async_alpha.api_simulator.ui.dashboard;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.service.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardActionHandler {

    private final RequestLogger logger;
    private final RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker;
    private final RateLimitAnalyzer analyzer;
    private final EnhancedReportGenerator reportGenerator;
    private final DatasetLoader datasetLoader = new DatasetLoader();
    private final BurstTrafficGenerator burstGenerator = new BurstTrafficGenerator();

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
        if (clientId == null || type == null) {
            ReportDialogHelper.showAlert("Please select both Client and Request Type!");
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
        List<ServiceRequest> burst = burstGenerator.generateBurst(clientId, 20, Duration.ofSeconds(10));
        int allowed = 0, blocked = 0;
        logPanel.append("\n⚡ Burst simulation: 20 requests in 10s for " + clientId + "...\n");

        for (ServiceRequest req : burst) {
            RateLimitEnforcer.RequestResult res = enforcer.processRequest(req);
            activityTracker.trackRequest(req, res.isBlocked());
            if (res.isBlocked()) blocked++; else allowed++;
            logPanel.append(String.format("%s [%s] %s %s - %s\n",
                res.isBlocked() ? "⛔" : "✅", req.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                req.getClientId(), req.getRequestType(), res.isBlocked() ? "BLOCKED" : "ALLOWED"));
        }
        onUpdated.run();
        logPanel.append(String.format("⚡ Burst Done! Total: 20 | Allowed: %d | Blocked: %d\n\n", allowed, blocked));
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
        RequestLog log = logger.getLog(clientId);
        AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        ReportDialogHelper.showReport("Full Violation Report - " + clientId, reportGenerator.generateViolationReport(abuse, log, activityTracker.getActivity(clientId)));
    }

    public void handleQuickReport(String clientId, LogPanel logPanel) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }
        logPanel.append("\n" + reportGenerator.generateUsageReport(clientId, activityTracker.getActivity(clientId), logger.getLog(clientId)));
    }

    public void handleCompareAll() {
        if (activityTracker.getAllActivities().isEmpty()) { ReportDialogHelper.showAlert("No client data available for comparison!"); return; }
        ReportDialogHelper.showReport("Multi-Client Comparison Report", reportGenerator.generateComparisonReport(activityTracker.getAllActivities()));
    }

    public void handleExport(String clientId, Window window, LogPanel logPanel) {
        if (clientId == null) { ReportDialogHelper.showAlert("Please select a client first!"); return; }
        RequestLog log = logger.getLog(clientId);
        AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        String report = reportGenerator.generateViolationReport(abuse, log, activityTracker.getActivity(clientId));
        String fileName = clientId + "_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        File file = ReportDialogHelper.chooseExportFile(window, clientId, fileName);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(report);
                logPanel.append("✅ Report exported to: " + file.getName() + "\n");
            } catch (Exception e) { ReportDialogHelper.showAlert("Error exporting report: " + e.getMessage()); }
        }
    }
}
