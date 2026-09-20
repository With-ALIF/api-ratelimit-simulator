package com.runtime_crew.api_simulator.multiservice.ui;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.multiservice.service.MultiServiceSimulator;
import com.runtime_crew.api_simulator.multiservice.service.ServiceRegistry;
import com.runtime_crew.api_simulator.policy.*;
import com.runtime_crew.api_simulator.service.*;
import com.runtime_crew.api_simulator.ui.dashboard.LogPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class MultiServiceDashboardView extends BorderPane {

    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private final RequestLogger logger = new RequestLogger();
    private final ClientActivityTracker activityTracker = new ClientActivityTracker();
    private final RateLimitEnforcer enforcer;
    private final RateLimitAnalyzer analyzer;
    private final RequestLogService requestLogService = new RequestLogService(Path.of("data", "multi_requests.csv"));
    private final MultiServiceSimulator simulator;
    private final MultiServiceActionHandler actionHandler;

    private final ServiceSelectorView selectorView;
    private final LogPanel logPanel = new LogPanel();
    private final Label statsLabel = new Label("No requests yet");
    private final MultiServiceBarChartView barChartView;
    private final Button manageBtn;
    private ComboBox<String> typeBox;

    public MultiServiceDashboardView() {
        enforcer = new RateLimitEnforcer(10, Duration.ofSeconds(10), Duration.ofSeconds(3), logger);
        analyzer = new RateLimitAnalyzer(List.of(
                new FixedWindowPolicy(5, Duration.ofSeconds(10)),
                new SlidingWindowPolicy(5, Duration.ofSeconds(10)),
                new BurstDetectionPolicy(4, Duration.ofSeconds(3)),
                new AbnormalPatternPolicy(3),
                new RetryAbusePolicy(8, Duration.ofSeconds(2))
        ));
        simulator = new MultiServiceSimulator(logger, enforcer, activityTracker, analyzer, requestLogService);
        actionHandler = new MultiServiceActionHandler(simulator, activityTracker);
        selectorView = new ServiceSelectorView(serviceRegistry);
        barChartView = new MultiServiceBarChartView(activityTracker);
        manageBtn = createButton("\u2699\uFE0F Manage Services", "#6366f1");

        initLayout();
        setupEvents();
        loadPreviousLogs();
        logPanel.append("\uD83D\uDE80 Multi-Service Simulation Mode\n");
        logPanel.append("Select services from the left panel and send requests.\n");
        logPanel.append("Each service tracks requests independently with its own rate limiting.\n\n");
    }

    private void loadPreviousLogs() {
        List<CsvRequestLogEntry> entries = requestLogService.loadAll();
        if (!entries.isEmpty()) {
            activityTracker.restoreFromLogEntries(entries);
            logPanel.append("Loaded " + entries.size() + " previous log entries from CSV.\n");
            refreshStats();
        }
    }

    private void initLayout() {
        setTop(createTopBar());
        setLeft(createLeftPanel());
        setCenter(createCenterPanel());
        setBottom(createBottomPanel());
        setPadding(new Insets(10));
    }

    private HBox createTopBar() {
        Label title = new Label("\uD83D\uDD17 Multi-Service Simulation");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subtitle = new Label("Simulate API traffic across multiple services simultaneously");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label teamLabel = new Label("Team - Runtime Crew");
        teamLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        VBox titleBox = new VBox(2, title, subtitle);
        HBox topBar = new HBox(12, titleBox, spacer, teamLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(8, 16, 8, 16));
        topBar.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        return topBar;
    }

    private VBox createLeftPanel() {
        manageBtn.setMaxWidth(Double.MAX_VALUE);

        VBox leftBox = new VBox(10, selectorView, manageBtn);
        leftBox.setPrefWidth(280);
        leftBox.setMinWidth(280);
        leftBox.setPadding(new Insets(10));
        leftBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; " +
                "-fx-border-color: #334155; -fx-border-radius: 8; -fx-border-width: 1;");
        return leftBox;
    }

    private VBox createCenterPanel() {
        Label statsHeader = new Label("Statistics");
        statsHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        statsLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        statsLabel.setWrapText(true);

        VBox statsBox = new VBox(4, statsHeader, statsLabel);
        statsBox.setPadding(new Insets(8));
        statsBox.setStyle("-fx-background-color: #182234; -fx-background-radius: 6;");

        ScrollPane logScroll = new ScrollPane(logPanel);
        logScroll.setFitToWidth(true);
        logScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(logScroll, Priority.ALWAYS);

        VBox.setVgrow(barChartView, Priority.ALWAYS);
        VBox centerBox = new VBox(8, statsBox, barChartView, logScroll);
        centerBox.setPadding(new Insets(0, 0, 0, 10));
        return centerBox;
    }

    private VBox createBottomPanel() {
        typeBox = new ComboBox<>();
        typeBox.getItems().add("RANDOM");
        for (RequestType rt : RequestType.values()) {
            typeBox.getItems().add(rt.name());
        }
        typeBox.setValue("RANDOM");
        typeBox.setStyle("-fx-background-color: #0f172a; -fx-border-color: #334155; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #f8fafc;");

        Button sendBtn = createButton("\u2709\uFE0F Send Request", "#2563eb");
        Button burstBtn = createButton("\u26A1 Simulate Burst", "#d97706");
        Button clearBtn = createButton("\uD83D\uDDD1\uFE0F Clear", "#dc2626");

        sendBtn.setOnAction(e -> {
            List<String> selected = selectorView.getSelectedServiceIds();
            RequestType type = "RANDOM".equals(typeBox.getValue()) ? null : RequestType.valueOf(typeBox.getValue());
            actionHandler.handleSendRequest(selected, type, logPanel, this::refreshStats);
        });

        burstBtn.setOnAction(e -> {
            List<String> selected = selectorView.getSelectedServiceIds();
            actionHandler.handleSimulateBurst(selected, logPanel, this::refreshStats);
        });

        clearBtn.setOnAction(e -> {
            logPanel.clear();
            activityTracker.clearAll();
            refreshStats();
        });

        HBox controls = new HBox(8, typeBox, sendBtn, burstBtn, clearBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10));

        VBox bottomBox = new VBox(controls);
        bottomBox.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; " +
                "-fx-border-width: 1 0 0 0;");
        return bottomBox;
    }

    private void setupEvents() {
        manageBtn.setOnAction(e -> {
            ServiceManagementDialog dialog = new ServiceManagementDialog(serviceRegistry);
            dialog.setOnServicesChanged(() -> selectorView.refreshCheckboxes());
            dialog.show((javafx.stage.Stage) getScene().getWindow());
        });
        selectorView.setOnSelectionChanged(ids -> refreshStats());
    }

    private void refreshStats() {
        List<String> selected = selectorView.getSelectedServiceIds();
        if (selected.isEmpty()) {
            statsLabel.setText("No services selected");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Selected: ").append(selected.size()).append(" service(s)\n");
        for (String svcId : selected) {
            int quota = simulator.getRemainingQuota(svcId);
            int max = simulator.getMaxRequests();
            ClientActivityTracker.ClientActivity activity = activityTracker.getActivity(svcId);
            int total = activity != null ? activity.getTotalRequests() : 0;
            int allowed = activity != null ? activity.getAllowedRequests() : 0;
            int blocked = activity != null ? activity.getBlockedRequests() : 0;

            sb.append(String.format("  %s: Quota %d/%d | Req: %d | A: %d | B: %d\n",
                    svcId, quota, max, total, allowed, blocked));
        }
        statsLabel.setText(sb.toString());
        barChartView.refresh(selected);
    }

    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-size: 12px; " +
                "-fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;", color));
        return btn;
    }
}
