package com.app.subly.controller;

import com.app.subly.common.SublyApplicationStage;
import com.app.subly.component.Projector;
import com.app.subly.model.Subtitle;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ControlPanelController {

    @FXML
    private ToggleButton toggleShowScreenBtn;
    @FXML
    private Label currentSubtitleLabel;
    @FXML
    private TableView<Subtitle> subtitleTable;
    @FXML
    private TableColumn<Subtitle, Integer> indexColumn;
    @FXML
    private TableColumn<Subtitle, String> subtitleColumn;
    @FXML
    private Button prevBtn;
    @FXML
    private Button nextBtn;
    @FXML
    private Button settingsButton;
    private Projector projector;

    public void setShowScreen(Projector projector) {
        this.projector = projector;
    }

    @FXML
    private void initialize() {
        subtitleTable();
        toggleButton();
        navigationButtons();
        settingsPopup();
    }

    private void settingsPopup() {
        settingsButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings_popup_view.fxml"));
                Parent root = loader.load();

                SettingsController settingsController = loader.getController();
                settingsController.setProjector(projector);

                Stage popup = new SublyApplicationStage();
                popup.initOwner(toggleShowScreenBtn.getScene().getWindow()); // or any parent stage
                popup.initModality(Modality.WINDOW_MODAL);
                popup.setTitle("Settings");
                popup.setScene(new Scene(root));
                popup.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void navigationButtons() {
        prevBtn.setOnAction(e -> {
            int currentIndex = subtitleTable.getSelectionModel().getSelectedIndex();
            if (currentIndex > 0) {
                subtitleTable.getSelectionModel().selectPrevious();
                subtitleTable.scrollTo(subtitleTable.getSelectionModel().getSelectedIndex());
            }
        });

        nextBtn.setOnAction(e -> {
            int currentIndex = subtitleTable.getSelectionModel().getSelectedIndex();
            if (currentIndex < subtitleTable.getItems().size() - 1) {
                subtitleTable.getSelectionModel().selectNext();
                subtitleTable.scrollTo(subtitleTable.getSelectionModel().getSelectedIndex());
            }
        });
    }

    private void subtitleTable() {
        // Map columns to fields
        indexColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        subtitleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getText()));

        // Create test data
        var subtitleData = FXCollections.observableArrayList(
                new Subtitle(1, "Hello world"),
                new Subtitle(2, "This is a subtitle"),
                new Subtitle(3, "Final line!")
        );

        // Push data into table
        subtitleTable.setItems(subtitleData);

        if (!subtitleData.isEmpty()) {
            subtitleTable.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(subtitleData.getFirst().getText());
        }

        subtitleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                currentSubtitleLabel.setText(newSel.getText());

                if (projector != null) {
                    projector.setText(newSel.getText());
                }
            }
        });
    }

    private void toggleButton() {
        if (toggleShowScreenBtn.isSelected()) {
            toggleShowScreenBtn.setText("Close Show Screen");
        } else {
            toggleShowScreenBtn.setText("Open Show Screen");
        }

        toggleShowScreenBtn.setOnAction(e -> {
            if (toggleShowScreenBtn.isSelected()) {
                // Open show screen
                if (projector == null) {
                    projector = new Projector();
                }
                projector.show();
                toggleShowScreenBtn.setText("Close Show Screen");
                // immediately set the text from currently selected row
                String selected = subtitleTable.getSelectionModel().getSelectedItem().getText();
                if (selected != null) {
                    projector.setText(selected);
                }
            } else {
                if (projector != null) {
                    projector.hide();
                    toggleShowScreenBtn.setText("Open Show Screen");
                }
            }
        });
    }
}
