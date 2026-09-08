package com.runtime_crew.api_simulator.ui.dashboard;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TopBarView extends HBox {

    public TopBarView() {
        this("Smart API Rate-Limit & Abuse Simulator");
    }

    public TopBarView(String titleText) {
        super(12);
        this.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subtitle = new Label("Real-time In-Memory Traffic & Rule Engine");
        subtitle.setStyle("-fx-font-size: 17px; -fx-text-fill: #94a3b8;");

        Label teamLabel = new Label("Team - Runtime Crew");
        teamLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        VBox titleBox = new VBox(4, title, subtitle, teamLabel);
        titleBox.setAlignment(Pos.CENTER);

        this.getChildren().add(titleBox);
        this.getStyleClass().add("top-bar");
    }
}
