package com.runtime_crew.api_simulator.multiservice.ui;

import com.runtime_crew.api_simulator.multiservice.model.ApiService;
import com.runtime_crew.api_simulator.multiservice.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceManagementDialog {

    private final ServiceRegistry registry;
    private Runnable onServicesChanged;

    public ServiceManagementDialog(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void setOnServicesChanged(Runnable handler) {
        this.onServicesChanged = handler;
    }

    public void show(Stage owner) {
        Stage dialog = new Stage();
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Service Management - Admin");
        dialog.setMinWidth(540);
        dialog.setMinHeight(480);

        ListView<ApiService> listView = new ListView<>();
        listView.getItems().addAll(registry.getAll());
        listView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        TextField searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D  Search services by name or ID...");
        searchField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; " +
                "-fx-prompt-text-fill: #64748b; -fx-font-size: 12px; " +
                "-fx-padding: 8 12; -fx-background-radius: 8; " +
                "-fx-border-color: #334155; -fx-border-radius: 8; -fx-border-width: 1;");
        searchField.textProperty().addListener((obs, old, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            if (q.isEmpty()) {
                listView.getItems().setAll(registry.getAll());
            } else {
                listView.getItems().setAll(
                        registry.getAll().stream()
                                .filter(s -> s.getName().toLowerCase().contains(q)
                                        || s.getId().toLowerCase().contains(q))
                                .collect(Collectors.toList())
                );
            }
        });

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        updateCountLabel(countLabel, listView);
        listView.getItems().addListener((javafx.collections.ListChangeListener<ApiService>) c ->
                updateCountLabel(countLabel, listView));

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ApiService item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    Label iconLabel = new Label(item.getIcon());
                    iconLabel.setStyle("-fx-font-size: 20px; -fx-padding: 0 4 0 0;");

                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px; -fx-font-weight: bold;");

                    Label idLabel = new Label(item.getId());
                    idLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

                    VBox nameBox = new VBox(1, nameLabel, idLabel);
                    nameBox.setAlignment(Pos.CENTER_LEFT);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label eventCount = new Label(item.getEnabledEventTypes().size() + " events");
                    eventCount.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

                    Label badge;
                    if (item.isDefault()) {
                        badge = new Label("Default");
                        badge.getStyleClass().addAll("badge-pill", "badge-ok");
                    } else {
                        badge = new Label("Custom");
                        badge.getStyleClass().addAll("badge-pill", "badge-warning");
                    }

                    VBox rightBox = new VBox(2, eventCount, badge);
                    rightBox.setAlignment(Pos.CENTER_RIGHT);
                    rightBox.setMinWidth(80);

                    HBox row = new HBox(10, iconLabel, nameBox, spacer, rightBox);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(8, 12, 8, 8));
                    row.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; " +
                            "-fx-border-color: #334155; -fx-border-radius: 8; -fx-border-width: 1;");

                    setGraphic(row);
                    setText(null);

                    row.setOnMouseEntered(e -> row.setStyle(row.getStyle()
                            .replace("#1e293b", "#253349")));
                    row.setOnMouseExited(e -> row.setStyle(row.getStyle()
                            .replace("#253349", "#1e293b")));
                }
            }
        });

        Button addBtn = new Button("+ Add Service");
        addBtn.getStyleClass().addAll("btn-primary");
        addBtn.setStyle(addBtn.getStyle() + "-fx-font-size: 12px; -fx-padding: 7 16;");

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().addAll("btn-danger");
        deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-font-size: 12px; -fx-padding: 7 16;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: #f8fafc; " +
                "-fx-font-size: 12px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-border-color: #64748b; -fx-border-radius: 6; -fx-border-width: 1;");

        addBtn.setOnAction(e -> showAddDialog(dialog, listView, searchField));
        deleteBtn.setOnAction(e -> {
            ApiService selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Please select a service to delete.");
                return;
            }
            if (selected.isDefault()) {
                showAlert("Cannot delete default services! They are always available.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Service");
            confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    registry.removeService(selected.getId());
                    refreshList(listView, searchField);
                    if (onServicesChanged != null) onServicesChanged.run();
                }
            });
        });
        closeBtn.setOnAction(e -> dialog.close());

        HBox buttonBar = new HBox(8, addBtn, deleteBtn, RegionBuilder(), closeBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 12, 10, 12));
        buttonBar.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; " +
                "-fx-border-width: 1 0 0 0;");

        HBox searchRow = new HBox(8, searchField, countLabel);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(0, 0, 4, 0));

        VBox headerBox = new VBox(4,
                new Label("\u2699\uFE0F  Service Management"),
                new Label("Add, remove, and configure API services")
        );
        headerBox.getChildren().get(0).setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");
        headerBox.getChildren().get(1).setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        headerBox.setPadding(new Insets(12, 14, 8, 14));
        headerBox.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        VBox.setVgrow(listView, Priority.ALWAYS);
        VBox root = new VBox(0, headerBox, searchRow, listView, buttonBar);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(root, 540, 480);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();
    }

    private Region RegionBuilder() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void refreshList(ListView<ApiService> listView, TextField searchField) {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            listView.getItems().setAll(registry.getAll());
        } else {
            listView.getItems().setAll(
                    registry.getAll().stream()
                            .filter(s -> s.getName().toLowerCase().contains(query)
                                    || s.getId().toLowerCase().contains(query))
                            .collect(Collectors.toList())
            );
        }
    }

    private void updateCountLabel(Label countLabel, ListView<ApiService> listView) {
        int total = registry.getAll().size();
        int showing = listView.getItems().size();
        if (showing == total) {
            countLabel.setText(total + " service" + (total != 1 ? "s" : ""));
        } else {
            countLabel.setText(showing + " of " + total + " services");
        }
    }

    private void showAddDialog(Stage parent, ListView<ApiService> listView, TextField searchField) {
        Dialog<ApiService> dialog = new Dialog<>();
        dialog.initOwner(parent);
        dialog.setTitle("Add New Service");
        dialog.setHeaderText("Register a new API Service");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 30, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("e.g. Twitter");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Twitter API");
        TextField iconField = new TextField();
        iconField.setPromptText("e.g. \uD83D\uDC26 (emoji)");
        iconField.setText("\uD83D\uDDA5\uFE0F");

        grid.add(new Label("Service ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Display Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Icon (emoji):"), 0, 2);
        grid.add(iconField, 1, 2);

        Label eventLabel = new Label("Event Types:");
        eventLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        grid.add(eventLabel, 0, 3);

        VBox eventCheckBoxes = new VBox(4);
        Map<String, CheckBox> eventMap = new LinkedHashMap<>();
        for (String et : ApiService.defaultEventTypes()) {
            CheckBox cb = new CheckBox(et);
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");
            eventMap.put(et, cb);
            eventCheckBoxes.getChildren().add(cb);
        }
        grid.add(eventCheckBoxes, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String id = idField.getText() != null ? idField.getText().trim() : "";
                String name = nameField.getText() != null ? nameField.getText().trim() : id;
                String icon = iconField.getText() != null ? iconField.getText().trim() : "\uD83D\uDDA5\uFE0F";
                if (!id.isEmpty()) {
                    Set<String> selectedEvents = new LinkedHashSet<>();
                    for (Map.Entry<String, CheckBox> e : eventMap.entrySet()) {
                        if (e.getValue().isSelected()) selectedEvents.add(e.getKey());
                    }
                    if (selectedEvents.isEmpty()) selectedEvents = ApiService.defaultEventTypes();
                    return new ApiService(id, name, false, icon, selectedEvents);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(service -> {
            boolean added = registry.addService(service.getId(), service.getName(),
                    service.getIcon(), service.getEnabledEventTypes());
            if (added) {
                refreshList(listView, searchField);
                if (onServicesChanged != null) onServicesChanged.run();
            } else {
                showAlert("Service with ID \"" + service.getId() + "\" already exists!");
            }
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Service Management");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
