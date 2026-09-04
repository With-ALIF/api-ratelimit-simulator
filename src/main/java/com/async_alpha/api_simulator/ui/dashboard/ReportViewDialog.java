package com.async_alpha.api_simulator.ui.dashboard;

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
