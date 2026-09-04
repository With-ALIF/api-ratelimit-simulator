package com.async_alpha.api_simulator.ui.dashboard;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class ReportDialogHelper {

    public static void showReport(String title, String content) {
        showReport(null, title, content);
    }

    public static void showReport(Window owner, String title, String content) {
        ReportViewDialog.show(owner, title, content);
    }

    public static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            alert.getDialogPane().getStylesheets().add(ReportDialogHelper.class.getResource("/styles/main.css").toExternalForm());
        } catch (Exception ignored) {}
        alert.showAndWait();
    }

    public static File chooseExportFile(Window window, String clientId, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Report");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
        );
        return fileChooser.showSaveDialog(window);
    }
}
