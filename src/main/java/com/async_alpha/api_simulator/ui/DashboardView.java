package com.async_alpha.api_simulator.ui;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.policy.*;
import com.async_alpha.api_simulator.service.*;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

public class DashboardView extends BorderPane {

    private final RequestLogger logger = new RequestLogger();
    private final RateLimitAnalyzer analyzer;

    private final TextArea logArea = new TextArea();

    public DashboardView() {

        RatePolicy fixed = new FixedWindowPolicy(5, Duration.ofSeconds(10));
        analyzer = new RateLimitAnalyzer(List.of(fixed));

        setTop(createTopBar());
        setCenter(createCenterPanel());
        setBottom(createLogPanel());

        setPadding(new Insets(12));
    }

    /* ================= UI SECTIONS ================= */

    private HBox createTopBar() {
        Label title = new Label("Smart API Rate-Limit & Abuse Simulator");
        title.getStyleClass().add("app-title");

        HBox top = new HBox(title);
        top.getStyleClass().add("top-bar");
        return top;
    }

    private VBox createCenterPanel() {

        ComboBox<String> clientBox = new ComboBox<>();
        clientBox.getItems().addAll("CLIENT_A", "CLIENT_B");
        clientBox.setPromptText("Select Client");

        ComboBox<RequestType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(RequestType.values());
        typeBox.setPromptText("Request Type");

        Button addBtn = new Button("Add Request");
        Button reportBtn = new Button("Generate Report");

        addBtn.setOnAction(e -> {
            if (clientBox.getValue() == null || typeBox.getValue() == null) return;

            ServiceRequest req = new ServiceRequest(
                    clientBox.getValue(),
                    typeBox.getValue(),
                    LocalDateTime.now()
            );
            logger.logRequest(req);
            logArea.appendText("Request added for " + clientBox.getValue() + "\n");
        });

        reportBtn.setOnAction(e -> {
            if (clientBox.getValue() == null) return;

            RequestLog log = logger.getLog(clientBox.getValue());
            AbuseReport report = analyzer.analyze(log);

            logArea.appendText(
                "Violations: " + report.getViolations().size() + "\n"
            );
        });

        VBox card = new VBox(10,
                new Label("Traffic Simulator"),
                clientBox,
                typeBox,
                addBtn,
                reportBtn
        );

        card.getStyleClass().add("card");
        return card;
    }

    private VBox createLogPanel() {
        Label title = new Label("System Logs");
        title.getStyleClass().add("section-title");

        logArea.setEditable(false);
        logArea.getStyleClass().add("log-area");
        logArea.setPrefRowCount(4);

        VBox box = new VBox(8, title, logArea);
        box.getStyleClass().add("card");
        return box;
    }
}
