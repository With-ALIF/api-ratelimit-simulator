package com.async_alpha.api_simulator;

import com.async_alpha.api_simulator.ui.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        DashboardView root = new DashboardView();

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