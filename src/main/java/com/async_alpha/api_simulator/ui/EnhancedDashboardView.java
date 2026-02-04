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

public class EnhancedDashboardView extends BorderPane {

    private final RequestLogger logger = new RequestLogger();
    private final RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker;
    private final RateLimitAnalyzer analyzer;
    private final EnhancedReportGenerator reportGenerator;

    private final TextArea logArea = new TextArea();
    private final Label quotaLabel = new Label("Quota: --");
    private final Label totalReqLabel = new Label("0");
    private final Label allowedReqLabel = new Label("0");
    private final Label blockedReqLabel = new Label("0");
    private final Label successRateLabel = new Label("100%");
    private final Label riskLevelLabel = new Label("NORMAL");
    
    private TableView<ActivityRecord> activityTable;
    private ObservableList<ActivityRecord> activityData;
    
    private String currentClient = null;

    public EnhancedDashboardView() {
        // Rate limit: 5 requests per 10 seconds
        enforcer = new RateLimitEnforcer(5, Duration.ofSeconds(10), logger);
        activityTracker = new ClientActivityTracker();
        reportGenerator = new EnhancedReportGenerator();

        // Initialize with multiple detection policies
        List<RatePolicy> policies = List.of(
            new FixedWindowPolicy(5, Duration.ofSeconds(10)),
            new SlidingWindowPolicy(5, Duration.ofSeconds(10)),
            new BurstDetectionPolicy(4, Duration.ofSeconds(3)),  // 4 requests in 3 seconds = burst
            new AbnormalPatternPolicy(3),  // Max 3 off-hours requests
            new RetryAbusePolicy(8, Duration.ofSeconds(2))  // 8 consecutive rapid requests
        );
        analyzer = new RateLimitAnalyzer(policies);

        setTop(createTopBar());
        setCenter(createMainContent());
        setBottom(createLogPanel());

        setPadding(new Insets(12));
    }

    /* ================= UI SECTIONS ================= */

    private HBox createTopBar() {
        Label title = new Label("🚀 Smart API Rate-Limit & Abuse Simulator");
        title.getStyleClass().add("app-title");

        HBox top = new HBox(title);
        top.getStyleClass().add("top-bar");
        return top;
    }

    private HBox createMainContent() {
        VBox leftPanel = createControlPanel();
        VBox rightPanel = createActivityPanel();

        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        HBox main = new HBox(15, leftPanel, rightPanel);
        return main;
    }

    private VBox createControlPanel() {
        // Client Selection
        ComboBox<String> clientBox = new ComboBox<>();
        clientBox.getItems().addAll("CLIENT_A", "CLIENT_B", "CLIENT_C");
        clientBox.setPromptText("Select Client");
        clientBox.setPrefWidth(200);

        // Request Type Selection
        ComboBox<RequestType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(RequestType.values());
        typeBox.setPromptText("Request Type");
        typeBox.setPrefWidth(200);

        // Quota Display
        quotaLabel.getStyleClass().add("status-ok");
        quotaLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Risk Level Display
        riskLevelLabel.getStyleClass().add("status-ok");
        riskLevelLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // Buttons
        Button addBtn = new Button("📤 Send Request");
        addBtn.setPrefWidth(200);
        
        Button reportBtn = new Button("📊 Full Report");
        reportBtn.setPrefWidth(200);
        
        Button quickReportBtn = new Button("📋 Quick Summary");
        quickReportBtn.setPrefWidth(200);
        
        Button exportBtn = new Button("💾 Export Report");
        exportBtn.setPrefWidth(200);
        
        Button clearBtn = new Button("🗑️ Clear History");
        clearBtn.setPrefWidth(200);

        Button compareBtn = new Button("📈 Compare All");
        compareBtn.setPrefWidth(200);

        // Statistics Panel
        VBox statsBox = createStatsPanel();

        // Event Handlers
        clientBox.setOnAction(e -> {
            currentClient = clientBox.getValue();
            updateQuotaDisplay();
            updateStatistics();
            updateActivityTable();
            updateRiskLevel();
        });

        addBtn.setOnAction(e -> {
            if (clientBox.getValue() == null || typeBox.getValue() == null) {
                showAlert("Please select both Client and Request Type!");
                return;
            }

            ServiceRequest req = new ServiceRequest(
                    clientBox.getValue(),
                    typeBox.getValue(),
                    LocalDateTime.now()
            );
            
            // Process with rate limiter
            RateLimitEnforcer.RequestResult result = enforcer.processRequest(req);
            
            // Track activity
            activityTracker.trackRequest(req, result.isBlocked());
            
            // Update UI
            updateQuotaDisplay();
            updateStatistics();
            updateActivityTable();
            updateRiskLevel();
            
            // Log result
            String statusIcon = result.isBlocked() ? "⛔" : "✅";
            
            logArea.appendText(String.format(
                "%s [%s] %s - %s | Quota: %d/%d\n",
                statusIcon,
                req.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                req.getClientId(),
                result.isBlocked() ? "BLOCKED" : "ALLOWED",
                result.getRemainingQuota(),
                enforcer.getMaxRequests()
            ));
        });

        reportBtn.setOnAction(e -> {
            if (clientBox.getValue() == null) {
                showAlert("Please select a client first!");
                return;
            }

            generateFullReport(clientBox.getValue());
        });

        quickReportBtn.setOnAction(e -> {
            if (clientBox.getValue() == null) {
                showAlert("Please select a client first!");
                return;
            }

            generateQuickReport(clientBox.getValue());
        });

        exportBtn.setOnAction(e -> {
            if (clientBox.getValue() == null) {
                showAlert("Please select a client first!");
                return;
            }

            exportReportToFile(clientBox.getValue());
        });

        compareBtn.setOnAction(e -> {
            generateComparisonReport();
        });

        clearBtn.setOnAction(e -> {
            if (currentClient != null) {
                logArea.appendText("⚠️ History cleared for " + currentClient + "\n");
                activityData.clear();
                updateStatistics();
                updateRiskLevel();
            }
        });

        VBox card = new VBox(10,
                new Label("🎛️ Control Panel"),
                new Separator(),
                clientBox,
                typeBox,
                quotaLabel,
                new HBox(5, new Label("Risk Level:"), riskLevelLabel),
                new Separator(),
                addBtn,
                reportBtn,
                quickReportBtn,
                exportBtn,
                compareBtn,
                clearBtn,
                new Separator(),
                statsBox
        );

        card.getStyleClass().add("card");
        card.setPrefWidth(250);
        return card;
    }

    private VBox createStatsPanel() {
        Label statsTitle = new Label("📈 Statistics");
        statsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // Labels
        grid.add(new Label("Total Requests:"), 0, 0);
        grid.add(totalReqLabel, 1, 0);
        
        grid.add(new Label("Allowed:"), 0, 1);
        grid.add(allowedReqLabel, 1, 1);
        
        grid.add(new Label("Blocked:"), 0, 2);
        grid.add(blockedReqLabel, 1, 2);
        
        grid.add(new Label("Success Rate:"), 0, 3);
        grid.add(successRateLabel, 1, 3);

        totalReqLabel.setStyle("-fx-font-weight: bold;");
        allowedReqLabel.setStyle("-fx-font-weight: bold;");
        allowedReqLabel.getStyleClass().add("status-ok");
        blockedReqLabel.setStyle("-fx-font-weight: bold;");
        blockedReqLabel.getStyleClass().add("status-danger");
        successRateLabel.setStyle("-fx-font-weight: bold;");

        VBox box = new VBox(8, statsTitle, grid);
        return box;
    }

    private VBox createActivityPanel() {
        Label title = new Label("📋 Request Activity Log");
        title.getStyleClass().add("section-title");

        // Create table
        activityTable = new TableView<>();
        activityData = FXCollections.observableArrayList();
        activityTable.setItems(activityData);

        // Time column
        TableColumn<ActivityRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedTime())
        );
        timeCol.setPrefWidth(100);

        // Type column
        TableColumn<ActivityRecord, RequestType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("requestType"));
        typeCol.setPrefWidth(100);

        // Status column
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

    /* ================= HELPER METHODS ================= */

    private void updateQuotaDisplay() {
        if (currentClient == null) {
            quotaLabel.setText("Quota: --");
            return;
        }

        int remaining = enforcer.getRemainingQuota(currentClient);
        int max = enforcer.getMaxRequests();
        
        quotaLabel.setText(String.format("Quota: %d / %d", remaining, max));
        
        // Color coding
        quotaLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        if (remaining == 0) {
            quotaLabel.getStyleClass().add("status-danger");
        } else if (remaining <= 2) {
            quotaLabel.getStyleClass().add("status-warning");
        } else {
            quotaLabel.getStyleClass().add("status-ok");
        }
    }

    private void updateStatistics() {
        if (currentClient == null) {
            totalReqLabel.setText("0");
            allowedReqLabel.setText("0");
            blockedReqLabel.setText("0");
            successRateLabel.setText("100%");
            return;
        }

        ClientActivity activity = activityTracker.getActivity(currentClient);
        if (activity == null) {
            totalReqLabel.setText("0");
            allowedReqLabel.setText("0");
            blockedReqLabel.setText("0");
            successRateLabel.setText("100%");
            return;
        }

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

    private void updateRiskLevel() {
        if (currentClient == null) {
            riskLevelLabel.setText("--");
            return;
        }

        RequestLog log = logger.getLog(currentClient);
        if (log == null || log.getRequests().isEmpty()) {
            riskLevelLabel.setText("NORMAL");
            riskLevelLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
            riskLevelLabel.getStyleClass().add("status-ok");
            return;
        }

        AbuseReport report = analyzer.analyze(log);
        ViolationLevel level = report.getLevel();
        
        riskLevelLabel.setText(level.toString());
        riskLevelLabel.getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
        
        switch (level) {
            case NORMAL -> riskLevelLabel.getStyleClass().add("status-ok");
            case WARNING -> riskLevelLabel.getStyleClass().add("status-warning");
            case CRITICAL -> riskLevelLabel.getStyleClass().add("status-danger");
        }
    }

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
        activityTable.scrollTo(activityData.size() - 1);
    }

    private void generateFullReport(String clientId) {
        RequestLog log = logger.getLog(clientId);
        ClientActivity activity = activityTracker.getActivity(clientId);
        
        if (log == null && activity == null) {
            logArea.appendText("No data found for " + clientId + "\n");
            return;
        }
        
        AbuseReport abuseReport = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        String report = reportGenerator.generateViolationReport(abuseReport, log, activity);
        
        // Display in a new dialog
        showReportDialog("Full Violation Report - " + clientId, report);
    }

    private void generateQuickReport(String clientId) {
        RequestLog log = logger.getLog(clientId);
        ClientActivity activity = activityTracker.getActivity(clientId);
        
        String report = reportGenerator.generateUsageReport(clientId, activity, log);
        
        logArea.appendText("\n" + report);
    }

    private void generateComparisonReport() {
        String report = reportGenerator.generateComparisonReport(activityTracker.getAllActivities());
        showReportDialog("Multi-Client Comparison", report);
    }

    private void exportReportToFile(String clientId) {
        RequestLog log = logger.getLog(clientId);
        ClientActivity activity = activityTracker.getActivity(clientId);
        
        if (log == null && activity == null) {
            showAlert("No data found for " + clientId);
            return;
        }
        
        AbuseReport abuseReport = log != null ? analyzer.analyze(log) : new AbuseReport(clientId);
        String report = reportGenerator.generateViolationReport(abuseReport, log, activity);
        
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
                logArea.appendText("✅ Report exported to: " + file.getAbsolutePath() + "\n");
            } catch (IOException e) {
                showAlert("Error exporting report: " + e.getMessage());
            }
        }
    }

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

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}