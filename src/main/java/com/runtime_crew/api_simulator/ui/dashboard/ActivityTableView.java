package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.RequestType;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ActivityRecord;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ActivityTableView extends VBox {

    private final TableView<ActivityRecord> activityTable = new TableView<>();
    private final ObservableList<ActivityRecord> activityData = FXCollections.observableArrayList();
    private List<ActivityRecord> allRecords = new ArrayList<>();
    private String statusFilter = "ALL";
    private String typeFilter = "ALL";

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

        activityTable.getColumns().addAll(List.of(timeCol, clientCol, typeCol, statusCol));
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        activityTable.setPlaceholder(new Label("No activity recorded yet"));

        VBox.setVgrow(activityTable, Priority.ALWAYS);
        this.getStyleClass().add("card");
        this.getChildren().addAll(title, activityTable);
    }

    public void setRecords(List<ActivityRecord> records) {
        if (records == null || records.isEmpty()) {
            allRecords = new ArrayList<>();
            activityData.clear();
            return;
        }
        allRecords = new ArrayList<>(records);
        applyFilters();
    }

    public void setAllRecords(List<ActivityRecord> records) {
        if (records == null || records.isEmpty()) {
            allRecords = new ArrayList<>();
            activityData.clear();
            return;
        }
        allRecords = new ArrayList<>(records);
        allRecords.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        applyFilters();
    }

    public void setStatusFilter(String status) {
        this.statusFilter = status != null ? status : "ALL";
        applyFilters();
    }

    public void setTypeFilter(String type) {
        this.typeFilter = type != null ? type : "ALL";
        applyFilters();
    }

    private void applyFilters() {
        activityData.clear();
        for (ActivityRecord r : allRecords) {
            boolean statusMatch = "ALL".equals(statusFilter) || r.getStatus().equals(statusFilter);
            boolean typeMatch = "ALL".equals(typeFilter) || r.getRequestType().toString().equals(typeFilter);
            if (statusMatch && typeMatch) {
                activityData.add(r);
            }
        }
    }

    public void clear() {
        allRecords = new ArrayList<>();
        activityData.clear();
    }
}
