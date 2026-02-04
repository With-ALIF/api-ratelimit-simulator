package com.async_alpha.api_simulator.ui;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.policy.*;
import com.async_alpha.api_simulator.service.*;
import com.async_alpha.api_simulator.service.ClientActivityTracker.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

/**
 * Final Enhanced Dashboard View with complete abnormal pattern detection
 * and comprehensive violation reporting
 */
public class DashboardView extends BorderPane {

    // Core Services
    private final RequestLogger logger = new RequestLogger();
    private final RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker;
    private final RateLimitAnalyzer analyzer;
    private final EnhancedReportGenerator reportGenerator;

    // UI Components - Logs and Display
    private final TextArea logArea = new TextArea();
    
    // UI Components - Labels
    private final Label quotaLabel = new Label("Quota: --");
    private final Label totalReqLabel = new Label("0");
    private final Label allowedReqLabel = new Label("0");
    private final Label blockedReqLabel = new Label("0");
    private final Label successRateLabel = new Label("100%");
    private final Label riskLevelLabel = new Label("NORMAL");
    
    // UI Components - Table
    private TableView<ActivityRecord> activityTable;
    private ObservableList<ActivityRecord> activityData;
    
    // State
    private String currentClient = null;

    public DashboardView() {
        // Initialize Rate Limiter: 5 requests per 10 seconds
        enforcer = new RateLimitEnforcer(5, Duration.ofSeconds(10), logger);
        activityTracker = new ClientActivityTracker();
        reportGenerator = new EnhancedReportGenerator();

        // Initialize with MULTIPLE detection policies for comprehensive abuse detection
        List<RatePolicy> policies = List.of(
            // Basic rate limiting
            new FixedWindowPolicy(5, Duration.ofSeconds(10)),
            new SlidingWindowPolicy(5, Duration.ofSeconds(10)),
            
            // Advanced abuse detection
            new BurstDetectionPolicy(4, Duration.ofSeconds(3)),      // Detect bursts: 4 req in 3 sec
            new AbnormalPatternPolicy(3),                             // Detect unusual patterns
            new RetryAbusePolicy(8, Duration.ofSeconds(2))           // Detect retry abuse
        );
        analyzer = new RateLimitAnalyzer(policies);

        // Build UI
        setTop(createTopBar());
        setCenter(createMainContent());
        setBottom(createLogPanel());

        setPadding(new Insets(12));
        
        // Welcome message
        logArea.appendText("🚀 API Rate-Limit & Abuse Simulator Started\n");
        logArea.appendText("📊 Advanced detection policies loaded\n");
        logArea.appendText("✅ System ready - Select a client to begin\n\n");
    }

    /* ═══════════════════════════════════════════════════════════
     *                      UI CREATION METHODS
     * ═══════════════════════════════════════════════════════════ */

    /**
     * Create the top application bar
     */
    private HBox createTopBar() {
        Label title = new Label("🚀 Smart API Rate-Limit & Abuse Simulator");
        title.getStyleClass().add("app-title");

        HBox top = new HBox(title);
        top.getStyleClass().add("top-bar");
        return top;
    }

    /**
     * Create main content area (control panel + activity panel)
     */
    private HBox createMainContent() {
        VBox leftPanel = createControlPanel();
        VBox rightPanel = createActivityPanel();

        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        HBox main = new HBox(15, leftPanel, rightPanel);
        return main;
    }

    /**
     * Create the left control panel with client selection, buttons, and statistics
     */
    private VBox createControlPanel() {
        // ─────────────────────────────────────────
        // Client Selection Dropdown
        // ─────────────────────────────────────────
        ComboBox<String> clientBox = new ComboBox<>();
        clientBox.getItems().addAll("CLIENT_A", "CLIENT_B", "CLIENT_C");
        clientBox.setPromptText("Select Client");
        clientBox.setPrefWidth(200);

        // ─────────────────────────────────────────
        // Request Type Selection
        // ─────────────────────────────────────────
        ComboBox<RequestType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(RequestType.values());
        typeBox.setPromptText("Request Type");
        typeBox.setPrefWidth(200);

        // ─────────────────────────────────────────
        // Quota Display with Color Coding
        // ─────────────────────────────────────────
        quotaLabel.getStyleClass().add("status-ok");
        quotaLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // ─────────────────────────────────────────
        // Risk Level Display
        // ─────────────────────────────────────────
        riskLevelLabel.getStyleClass().add("status-ok");
        riskLevelLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        
        HBox riskBox = new HBox(5, new Label("Risk Level:"), riskLevelLabel);
        riskBox.setAlignment(Pos.CENTER_LEFT);

        // ─────────────────────────────────────────
        // Action Buttons
        // ─────────────────────────────────────────
        Button sendBtn = new Button("📤 Send Request");
        sendBtn.setPrefWidth(200);
        
        Button fullReportBtn = new Button("📊 Full Report");
        fullReportBtn.setPrefWidth(200);
        
        Button quickReportBtn = new Button("📋 Quick Summary");
        quickReportBtn.setPrefWidth(200);
        
        Button compareBtn = new Button("📈 Compare All");
        compareBtn.setPrefWidth(200);
        
        Button exportBtn = new Button("💾 Export Report");
        exportBtn.setPrefWidth(200);
        
        Button clearBtn = new Button("🗑️ Clear History");
        clearBtn.setPrefWidth(200);

        // ─────────────────────────────────────────
        // Statistics Panel
        // ─────────────────────────────────────────
        VBox statsBox = createStatsPanel();

        // ═════════════════════════════════════════
        // EVENT HANDLERS
        // ═════════════════════════════════════════

        // Client Selection Handler
        clientBox.setOnAction(e -> {
            currentClient = clientBox.getValue();
            updateQuotaDisplay();
            updateStatistics();
            updateActivityTable();
            updateRiskLevel();
            
            logArea.appendText(String.format("👤 Client selected: %s\n", currentClient));
        });

        // Send Request Handler
        sendBtn.setOnAction(e -> handleSendRequest(clientBox, typeBox));

        // Full Report Handler
        fullReportBtn.setOnAction(e -> {
            if (validateClientSelection(clientBox)) {
                generateFullReport(clientBox.getValue());
            }
        });

        // Quick Report Handler
        quickReportBtn.setOnAction(e -> {
            if (validateClientSelection(clientBox)) {
                generateQuickReport(clientBox.getValue());
            }
        });

        // Compare All Clients Handler
        compareBtn.setOnAction(e -> generateComparisonReport());

        // Export Report Handler
        exportBtn.setOnAction(e -> {
            if (validateClientSelection(clientBox)) {
                exportReportToFile(clientBox.getValue());
            }
        });

        // Clear History Handler
        clearBtn.setOnAction(e -> {
            if (currentClient != null) {
                logArea.appendText("⚠️ History cleared for " + currentClient + "\n");
                activityData.clear();
                updateStatistics();
                updateRiskLevel();
            } else {
                showAlert("Please select a client first!");
            }
        });

        // ─────────────────────────────────────────
        // Assemble Control Panel
        // ─────────────────────────────────────────
        VBox card = new VBox(10,
                new Label("🎛️ Control Panel"),
                new Separator(),
                clientBox,
                typeBox,
                quotaLabel,
                riskBox,
                new Separator(),
                sendBtn,
                fullReportBtn,
                quickReportBtn,
                compareBtn,
                exportBtn,
                clearBtn,
                new Separator(),
                statsBox
        );

        card.getStyleClass().add("card");
        card.setPrefWidth(250);
        return card;
    }

    /**
     * Create statistics panel showing request metrics
     */
    private VBox createStatsPanel() {
        Label statsTitle = new Label("📈 Statistics");
        statsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // Add statistics labels
        grid.add(new Label("Total Requests:"), 0, 0);
        grid.add(totalReqLabel, 1, 0);
        
        grid.add(new Label("Allowed:"), 0, 1);
        grid.add(allowedReqLabel, 1, 1);
        
        grid.add(new Label("Blocked:"), 0, 2);
        grid.add(blockedReqLabel, 1, 2);
        
        grid.add(new Label("Success Rate:"), 0, 3);
        grid.add(successRateLabel, 1, 3);

        // Style the statistics
        totalReqLabel.setStyle("-fx-font-weight: bold;");
        allowedReqLabel.setStyle("-fx-font-weight: bold;");
        allowedReqLabel.getStyleClass().add("status-ok");
        blockedReqLabel.setStyle("-fx-font-weight: bold;");
        blockedReqLabel.getStyleClass().add("status-danger");
        successRateLabel.setStyle("-fx-font-weight: bold;");

        VBox box = new VBox(8, statsTitle, grid);
        return box;
    }

    /**
     * Create the activity panel with request history table
     */
    private VBox createActivityPanel() {
        Label title = new Label("📋 Request Activity Log");
        title.getStyleClass().add("section-title");

        // ─────────────────────────────────────────
        // Create Activity Table
        // ─────────────────────────────────────────
        activityTable = new TableView<>();
        activityData = FXCollections.observableArrayList();
        activityTable.setItems(activityData);

        // Time Column
        TableColumn<ActivityRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedTime())
        );
        timeCol.setPrefWidth(100);

        // Type Column
        TableColumn<ActivityRecord, RequestType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("requestType"));
        typeCol.setPrefWidth(100);

        // Status Column with Color Coding
        TableColumn<ActivityRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus())
        );
        statusCol.setPrefWidth(120);

        // Custom cell factory for colored status
        statusCol.setCellFactory(column -> new TableCell<ActivityRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("BLOCKED")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    }
                }
            }
        });

        activityTable.getColumns().addAll(timeCol, typeCol, statusCol);
        activityTable.setPlaceholder(new Label("No activity recorded yet"));

        VBox box = new VBox(10, title, activityTable);
        box.getStyleClass().add("card");
        VBox.setVgrow(activityTable, Priority.ALWAYS);
        
        return box;
    }

    /**
     * Create the bottom log panel
     */
    private VBox createLogPanel() {
        Label title = new Label("📝 System Logs");
        title.getStyleClass().add("section-title");

        logArea.setEditable(false);
        logArea.getStyleClass().add("log-area");
        logArea.setPrefRowCount(6);
        logArea.setWrapText(true);

        VBox box = new VBox(8, title, logArea);
        box.getStyleClass().add("card");
        return box;
    }

    /* ═══════════════════════════════════════════════════════════
     *                    EVENT HANDLER METHODS
     * ═══════════════════════════════════════════════════════════ */

    /**
     * Handle sending a new request
     */
    private void handleSendRequest(ComboBox<String> clientBox, ComboBox<RequestType> typeBox) {
        // Validate selections
        if (clientBox.getValue() == null || typeBox.getValue() == null) {
            showAlert("Please select both Client and Request Type!");
            return;
        }

        // Create the request
        ServiceRequest req = new ServiceRequest(
                clientBox.getValue(),
                typeBox.getValue(),
                LocalDateTime.now()
        );
        
        // Process with rate limiter
        RateLimitEnforcer.RequestResult result = enforcer.processRequest(req);
        
        // Track activity
        activityTracker.trackRequest(req, result.isBlocked());
        
        // Update all UI components
        updateQuotaDisplay();
        updateStatistics();
        updateActivityTable();
        updateRiskLevel();
        
        // Log the result with detailed information
        String statusIcon = result.isBlocked() ? "⛔" : "✅";
        String typeIcon = getRequestTypeIcon(req.getRequestType());
        
        logArea.appendText(String.format(
            "%s [%s] %s %s %s - %s | Quota: %d/%d%s\n",
            statusIcon,
            req.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
            typeIcon,
            req.getClientId(),
            req.getRequestType(),
            result.isBlocked() ? "BLOCKED" : "ALLOWED",
            result.getRemainingQuota(),
            enforcer.getMaxRequests(),
            result.isBlocked() ? " ⚠️ RATE LIMIT EXCEEDED" : ""
        ));
        
        // Auto-scroll to bottom
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    /* ═══════════════════════════════════════════════════════════
     *                    REPORT GENERATION METHODS
     * ═══════════════════════════════════════════════════════════ */

    /**
     * Generate comprehensive violation report
     */
    private void generateFullReport(String clientId) {
        RequestLog log = logger.getLog(clientId);
        ClientActivity activity = activityTracker.getActivity(clientId);
        
        if (log == null && activity == null) {
            logArea.appendText("❌ No data found for " + clientId + "\n");
            return;
        }
        
        // Analyze for violations
        AbuseReport abuseReport = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        
        // Generate formatted report
        String report = reportGenerator.generateViolationReport(abuseReport, log, activity);
        
        // Display in dialog
        showReportDialog("Full Violation Report - " + clientId, report);
        
        // Log report generation
        logArea.appendText(String.format(
            "📊 Full report generated for %s - Severity: %s\n", 
            clientId, 
            abuseReport.getLevel()
        ));
    }

    /**
     * Generate quick summary report
     */
    private void generateQuickReport(String clientId) {
        RequestLog log = logger.getLog(clientId);
        ClientActivity activity = activityTracker.getActivity(clientId);
        
        String report = reportGenerator.generateUsageReport(clientId, activity, log);
        
        logArea.appendText("\n" + report);
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Generate multi-client comparison report
     */
    private void generateComparisonReport() {
        if (activityTracker.getAllActivities().isEmpty()) {
            showAlert("No client data available for comparison!");
            return;
        }
        
        String report = reportGenerator.generateComparisonReport(activityTracker.getAllActivities());
        showReportDialog("Multi-Client Comparison Report", report);
        
        logArea.appendText("📈 Multi-client comparison report generated\n");
    }

    /**
     * Export report to file
     */
    private void exportReportToFile(String clientId) {
        RequestLog log = logger.getLog(clientId);
        ClientActivity activity = activityTracker.getActivity(clientId);
        
        if (log == null && activity == null) {
            showAlert("No data found for " + clientId);
            return;
        }
        
        // Generate report
        AbuseReport abuseReport = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        String report = reportGenerator.generateViolationReport(abuseReport, log, activity);
        
        // Show file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Report");
        fileChooser.setInitialFileName(clientId + "_report_" + 
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(report);
                logArea.appendText("✅ Report exported to: " + file.getName() + "\n");
            } catch (IOException e) {
                showAlert("Error exporting report: " + e.getMessage());
                logArea.appendText("❌ Export failed: " + e.getMessage() + "\n");
            }
        }
    }

    /* ═══════════════════════════════════════════════════════════
     *                      UI UPDATE METHODS
     * ═══════════════════════════════════════════════════════════ */

    /**
     * Update quota display with color coding
     */
    private void updateQuotaDisplay() {
        if (currentClient == null) {
            quotaLabel.setText("Quota: --");
            return;
        }

        int remaining = enforcer.getRemainingQuota(currentClient);
        int max = enforcer.getMaxRequests();
        
        quotaLabel.setText(String.format("Quota: %d / %d", remaining, max));
        
        // Color coding based on remaining quota
        quotaLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        if (remaining == 0) {
            quotaLabel.getStyleClass().add("status-danger");
        } else if (remaining <= 2) {
            quotaLabel.getStyleClass().add("status-warning");
        } else {
            quotaLabel.getStyleClass().add("status-ok");
        }
    }

    /**
     * Update statistics display
     */
    private void updateStatistics() {
        if (currentClient == null) {
            resetStatistics();
            return;
        }

        ClientActivity activity = activityTracker.getActivity(currentClient);
        if (activity == null) {
            resetStatistics();
            return;
        }

        // Update values
        totalReqLabel.setText(String.valueOf(activity.getTotalRequests()));
        allowedReqLabel.setText(String.valueOf(activity.getAllowedRequests()));
        blockedReqLabel.setText(String.valueOf(activity.getBlockedRequests()));
        successRateLabel.setText(String.format("%.1f%%", activity.getSuccessRate()));
        
        // Color code success rate
        successRateLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        if (activity.getSuccessRate() >= 80) {
            successRateLabel.getStyleClass().add("status-ok");
        } else if (activity.getSuccessRate() >= 50) {
            successRateLabel.getStyleClass().add("status-warning");
        } else {
            successRateLabel.getStyleClass().add("status-danger");
        }
    }

    /**
     * Update risk level display
     */
    private void updateRiskLevel() {
        if (currentClient == null) {
            riskLevelLabel.setText("--");
            riskLevelLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
            return;
        }

        RequestLog log = logger.getLog(currentClient);
        if (log == null || log.getRequests().isEmpty()) {
            riskLevelLabel.setText("NORMAL");
            riskLevelLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
            riskLevelLabel.getStyleClass().add("status-ok");
            return;
        }

        // Analyze for violations
        AbuseReport report = analyzer.analyze(log);
        ViolationLevel level = report.getLevel();
        
        // Update text and color
        riskLevelLabel.setText(level.toString());
        riskLevelLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        
        switch (level) {
            case NORMAL -> riskLevelLabel.getStyleClass().add("status-ok");
            case WARNING -> riskLevelLabel.getStyleClass().add("status-warning");
            case CRITICAL -> riskLevelLabel.getStyleClass().add("status-danger");
        }
    }

    /**
     * Update activity table
     */
    private void updateActivityTable() {
        if (currentClient == null) {
            activityData.clear();
            return;
        }

        ClientActivity activity = activityTracker.getActivity(currentClient);
        if (activity == null) {
            activityData.clear();
            return;
        }

        activityData.setAll(activity.getRecords());
        
        // Scroll to bottom to show latest
        if (!activityData.isEmpty()) {
            activityTable.scrollTo(activityData.size() - 1);
        }
    }

    /**
     * Reset statistics to default values
     */
    private void resetStatistics() {
        totalReqLabel.setText("0");
        allowedReqLabel.setText("0");
        blockedReqLabel.setText("0");
        successRateLabel.setText("100%");
        successRateLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        successRateLabel.getStyleClass().add("status-ok");
    }

    /* ═══════════════════════════════════════════════════════════
     *                      UTILITY METHODS
     * ═══════════════════════════════════════════════════════════ */

    /**
     * Validate that a client is selected
     */
    private boolean validateClientSelection(ComboBox<String> clientBox) {
        if (clientBox.getValue() == null) {
            showAlert("Please select a client first!");
            return false;
        }
        return true;
    }

    /**
     * Show a report in a dialog window
     */
    private void showReportDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefWidth(700);
        textArea.setPrefHeight(500);
        textArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    /**
     * Show an alert dialog
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Get icon for request type
     */
    private String getRequestTypeIcon(RequestType type) {
        return switch (type) {
            case READ -> "📖";
            case WRITE -> "✍️";
            case UPDATE -> "🔄";
            case DELETE -> "🗑️";
        };
    }
}