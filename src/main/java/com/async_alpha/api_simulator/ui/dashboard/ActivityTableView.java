package com.async_alpha.api_simulator.ui.dashboard;

import com.async_alpha.api_simulator.model.RequestType;
import com.async_alpha.api_simulator.service.ClientActivityTracker.ActivityRecord;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class ActivityTableView extends VBox {

    private final TableView<ActivityRecord> activityTable = new TableView<>();
    private final ObservableList<ActivityRecord> activityData = FXCollections.observableArrayList();

    public ActivityTableView() {
        super(10);
        initLayout();
    }

    private void initLayout() {
        Label title = new Label("Request Activity Log");
        title.getStyleClass().add("section-title");

        activityTable.setItems(activityData);

        TableColumn<ActivityRecord, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClientId()));
        clientCol.setPrefWidth(100);

        TableColumn<ActivityRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedTime()));
        timeCol.setPrefWidth(100);

        TableColumn<ActivityRecord, RequestType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("requestType"));
        typeCol.setPrefWidth(100);

        TableColumn<ActivityRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(120);

        statusCol.setCellFactory(column -> new TableCell<>() {
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

        activityTable.getColumns().addAll(timeCol, clientCol, typeCol, statusCol);
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        activityTable.setPlaceholder(new Label("No activity recorded yet"));

        VBox.setVgrow(activityTable, Priority.ALWAYS);
        this.getStyleClass().add("card");
        this.getChildren().addAll(title, activityTable);
    }

    public void setRecords(List<ActivityRecord> records) {
        if (records == null || records.isEmpty()) {
            activityData.clear();
            return;
        }
        activityData.setAll(records);
        activityTable.scrollTo(activityData.size() - 1);
    }

    public void clear() {
        activityData.clear();
    }
}
