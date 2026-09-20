package com.runtime_crew.api_simulator.multiservice.ui;

import com.runtime_crew.api_simulator.multiservice.model.ApiService;
import com.runtime_crew.api_simulator.multiservice.service.ServiceRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class ServiceSelectorView extends VBox {

    private final ServiceRegistry registry;
    private final Map<String, CheckBox> checkBoxes = new TreeMap<>();
    private final VBox checkBoxContainer = new VBox(6);
    private Consumer<List<String>> onSelectionChanged;

    public ServiceSelectorView(ServiceRegistry registry) {
        super(10);
        this.registry = registry;
        initLayout();
        refreshCheckboxes();
    }

    private void initLayout() {
        Label title = new Label("Select Services");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button selectAllBtn = createSmallButton("Select All");
        Button deselectAllBtn = createSmallButton("Deselect All");
        HBox btnRow = new HBox(6, selectAllBtn, deselectAllBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        selectAllBtn.setOnAction(e -> {
            checkBoxes.values().forEach(cb -> cb.setSelected(true));
            notifySelectionChanged();
        });
        deselectAllBtn.setOnAction(e -> {
            checkBoxes.values().forEach(cb -> cb.setSelected(false));
            notifySelectionChanged();
        });

        ScrollPane scrollPane = new ScrollPane(checkBoxContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.getChildren().addAll(title, btnRow, scrollPane);
    }

    public void refreshCheckboxes() {
        checkBoxContainer.getChildren().clear();
        checkBoxes.clear();

        for (ApiService service : registry.getAll()) {
            CheckBox cb = new CheckBox(service.getIcon() + " " + service.getName());
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px;");
            cb.selectedProperty().addListener((obs, old, val) -> notifySelectionChanged());

            if (service.isDefault()) {
                Label badge = new Label("Default");
                badge.setStyle("-fx-font-size: 9px; -fx-text-fill: #38bdf8; -fx-padding: 0 0 0 4;");
                HBox row = new HBox(4, cb, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                checkBoxes.put(service.getId(), cb);
                checkBoxContainer.getChildren().add(row);
            } else {
                Label badge = new Label("Custom");
                badge.setStyle("-fx-font-size: 9px; -fx-text-fill: #a78bfa; -fx-padding: 0 0 0 4;");
                HBox row = new HBox(4, cb, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                checkBoxes.put(service.getId(), cb);
                checkBoxContainer.getChildren().add(row);
            }
        }
    }

    public List<String> getSelectedServiceIds() {
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : checkBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    public void setOnSelectionChanged(Consumer<List<String>> handler) {
        this.onSelectionChanged = handler;
    }

    private void notifySelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(getSelectedServiceIds());
        }
    }

    private Button createSmallButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; " +
                "-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 4; " +
                "-fx-border-color: #475569; -fx-border-radius: 4; -fx-border-width: 1;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("#334155", "#475569")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("#475569", "#334155")));
        return btn;
    }
}
