package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReportViewDialog {

    public static void show(Window owner, String title, String content) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(title);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label timeLabel = new Label("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        VBox headerText = new VBox(3, titleLabel, timeLabel);

        Button copyBtn = new Button("Copy Text");
        copyBtn.getStyleClass().add("btn-accent");
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(content);
            Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("Copied!");
        });

        Button exportBtn = new Button("Export File");
        exportBtn.getStyleClass().add("btn-success");
        exportBtn.setOnAction(e -> exportToFile(stage, content));

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());

        HBox actions = new HBox(8, copyBtn, exportBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, headerText, spacer, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1e293b; -fx-padding: 16 20; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setStyle(
            "-fx-control-inner-background: #090d16; " +
            "-fx-text-fill: #38bdf8; " +
            "-fx-font-family: 'JetBrains Mono', 'Consolas', 'Courier New', monospace; " +
            "-fx-font-size: 12.5px; " +
            "-fx-border-color: #334155; " +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        VBox.setVgrow(textArea, Priority.ALWAYS);

        VBox contentBox = new VBox(textArea);
        contentBox.setPadding(new Insets(16));
        contentBox.setStyle("-fx-background-color: #0f172a;");
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(contentBox);
        root.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(root, 760, 580);
        try {
            scene.getStylesheets().add(ReportViewDialog.class.getResource("/styles/main.css").toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.showAndWait();
    }

    public static void showComparisonTable(Window owner, String title, Map<String, ClientActivity> allActivities) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(title);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label timeLabel = new Label("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        VBox headerText = new VBox(3, titleLabel, timeLabel);

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());
        HBox actions = new HBox(closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, headerText, spacer, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1e293b; -fx-padding: 16 20; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        int sumTotal = 0, sumAllowed = 0, sumBlocked = 0;
        for (ClientActivity a : allActivities.values()) {
            sumTotal += a.getTotalRequests();
            sumAllowed += a.getAllowedRequests();
            sumBlocked += a.getBlockedRequests();
        }
        double sumRate = sumTotal == 0 ? 100.0 : (sumAllowed * 100.0) / sumTotal;

        String allowedPct = String.format("%.0f%%", sumTotal > 0 ? (sumAllowed * 100.0) / sumTotal : 0);
        String blockedPct = String.format("%.0f%%", sumTotal > 0 ? (sumBlocked * 100.0) / sumTotal : 0);

        TableView<String[]> table = new TableView<>();
        ObservableList<String[]> rows = FXCollections.observableArrayList();
        for (Map.Entry<String, ClientActivity> entry : allActivities.entrySet()) {
            ClientActivity a = entry.getValue();
            String aPct = String.format("%.0f%%", a.getSuccessRate());
            String bPct = String.format("%.0f%%", 100.0 - a.getSuccessRate());
            rows.add(new String[]{
                entry.getKey(),
                String.valueOf(a.getTotalRequests()),
                a.getAllowedRequests() + " (" + aPct + ")",
                a.getBlockedRequests() + " (" + bPct + ")",
                String.format("%.1f%%", a.getSuccessRate()),
                a.getLastActivityTime()
            });
        }
        rows.add(new String[]{
            "TOTAL",
            String.valueOf(sumTotal),
            sumAllowed + " (" + allowedPct + ")",
            sumBlocked + " (" + blockedPct + ")",
            String.format("%.1f%%", sumRate),
            "-"
        });

        table.setItems(rows);

        String[] headers = {"CLIENT", "REQUESTS", "ALLOWED", "BLOCKED", "SUCCESS %", "LAST ACT."};
        int[] widths = {140, 100, 130, 130, 110, 130};

        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[idx]));
            col.setPrefWidth(widths[i]);
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        int rowIdx = getIndex();
                        boolean isTotal = rowIdx >= 0 && rowIdx < table.getItems().size()
                            && "TOTAL".equals(table.getItems().get(rowIdx)[0]);
                        boolean isHeader = rowIdx < 0;
                        if (isTotal) {
                            setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-alignment: CENTER; -fx-background-color: #1e293b;");
                        } else if (idx == 4 && !isHeader) {
                            try {
                                double val = Double.parseDouble(item.replace("%", ""));
                                String color = (val >= 80) ? "#22c55e" : (val >= 50) ? "#f59e0b" : "#ef4444";
                                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
                            } catch (Exception e) {
                                setStyle("-fx-alignment: CENTER;");
                            }
                        } else if (idx == 0) {
                            setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 12;");
                        } else {
                            setStyle("-fx-alignment: CENTER;");
                        }
                    }
                }
            });
            table.getColumns().add(col);
        }

        table.setFixedCellSize(36);
        table.setPlaceholder(new Label("No data"));

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if ("TOTAL".equals(item[0])) {
                    setStyle("-fx-background-color: #1e293b; -fx-border-color: #38bdf8; -fx-border-width: 1 0 0 0;");
                } else {
                    setStyle("-fx-background-color: #090d16;");
                }
            }
        });

        table.setStyle(
            "-fx-control-inner-background: #090d16; " +
            "-fx-table-cell-border-color: transparent; " +
            "-fx-border-color: #334155; " +
            "-fx-border-radius: 8; -fx-background-radius: 8;"
        );

        VBox.setVgrow(table, Priority.ALWAYS);

        VBox contentBox = new VBox(table);
        contentBox.setPadding(new Insets(16));
        contentBox.setStyle("-fx-background-color: #0f172a;");
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(contentBox);
        root.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(root, 760, 480);
        try {
            scene.getStylesheets().add(ReportViewDialog.class.getResource("/styles/main.css").toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private static void exportToFile(Stage stage, String content) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Report");
        fc.setInitialFileName("report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
                ReportDialogHelper.showAlert("Report successfully exported to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                ReportDialogHelper.showAlert("Export failed: " + ex.getMessage());
            }
        }
    }
}
