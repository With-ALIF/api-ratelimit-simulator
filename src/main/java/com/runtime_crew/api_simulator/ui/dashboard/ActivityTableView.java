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
    private final TableColumn<ActivityRecord, String> serviceCol;
    private List<ActivityRecord> allRecords = new ArrayList<>();
    private String statusFilter = "ALL";
    private String typeFilter = "ALL";
    private String serviceFilter = "ALL_SERVICES";
    private boolean multiServiceMode = false;

    public ActivityTableView() {
        super(10);
        serviceCol = createServiceColumn();
        initLayout();
    }

    private TableColumn<ActivityRecord, String> createServiceColumn() {
        TableColumn<ActivityRecord, String> col = new TableColumn<>("Service");
        col.setCellValueFactory(data -> {
            String svc = data.getValue().getService();
            if (svc != null && !svc.trim().isEmpty()) {
                return new SimpleStringProperty(svc);
            }
            String clientId = data.getValue().getClientId();
            if (clientId != null && clientId.startsWith("svc_")) {
                return new SimpleStringProperty(clientId.substring(4));
            }
            return new SimpleStringProperty("-");
        });
        col.setPrefWidth(250);
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("-".equals(item)) {
                        setStyle("-fx-text-fill: #64748b;");
                    } else {
                        setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");
                    }
                }
            }
        });
        col.setVisible(false);
        return col;
    }

    public void setServiceColumnVisible(boolean visible) {
        serviceCol.setVisible(visible);
    }

    public void setMultiServiceMode(boolean multiServiceMode) {
        this.multiServiceMode = multiServiceMode;
        applyFilters();
    }

    private void initLayout() {
        Label title = new Label("Request Activity Log");
        title.getStyleClass().add("section-title");

        activityTable.setItems(activityData);

        TableColumn<ActivityRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedTime()));
        timeCol.setPrefWidth(80);

        TableColumn<ActivityRecord, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(data -> {
            String clientId = data.getValue().getClientId();
            if (clientId != null && clientId.startsWith("svc_")) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(clientId != null ? clientId : "-");
        });
        clientCol.setPrefWidth(100);

        TableColumn<ActivityRecord, RequestType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("requestType"));
        typeCol.setPrefWidth(80);

        TableColumn<ActivityRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(100);

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

        activityTable.getColumns().addAll(List.of(timeCol, clientCol, serviceCol, typeCol, statusCol));
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

    public void setServiceFilter(String service) {
        this.serviceFilter = (service != null && !service.isEmpty()) ? service : "ALL_SERVICES";
        applyFilters();
    }

    private void applyFilters() {
        activityData.clear();
        for (ActivityRecord r : allRecords) {
            boolean statusMatch = "ALL".equals(statusFilter) || r.getStatus().equals(statusFilter);
            boolean typeMatch = "ALL".equals(typeFilter) || r.getRequestType().toString().equals(typeFilter);
            if (multiServiceMode) {
                String svc = r.getService();
                boolean isSingleService = (svc == null || svc.trim().isEmpty() || "-".equals(svc));
                if (isSingleService) continue;
                if (!"ALL_SERVICES".equalsIgnoreCase(serviceFilter) && svc != null) {
                    boolean match = svc.equalsIgnoreCase(serviceFilter)
                            || svc.equalsIgnoreCase("svc_" + serviceFilter)
                            || serviceFilter.equalsIgnoreCase("svc_" + svc);
                    if (!match) continue;
                }
            }
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
