package com.smartratelimit_abuse;

import com.runtime_crew.api_simulator.ui.dashboard.EnhancedDashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        EnhancedDashboardView root = new EnhancedDashboardView();
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
            getClass().getResource("/styles/main.css").toExternalForm()
        );
        stage.setTitle("Smart API Rate-Limit & Abuse Simulator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
