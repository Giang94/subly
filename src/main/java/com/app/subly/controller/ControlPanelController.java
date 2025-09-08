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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class ControlPanelController {

    @FXML
    private ToggleButton toggleShowScreenButton;
    @FXML
    private Label currentSubtitleLabel;
    @FXML
    private TableView<Subtitle> subtitleTable;
    @FXML
    private TableColumn<Subtitle, Integer> indexColumn;
    @FXML
    private TableColumn<Subtitle, String> primaryTextColumn;
    @FXML
    private TableColumn<Subtitle, String> secondaryTextColumn;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button openFileButton;

    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem exitMenuItem;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;

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
        openFilePopup();
        menuBar();
    }

    private void menuBar() {
        openMenuItem.setOnAction(event -> {
            // Open file logic here
        });

        exitMenuItem.setOnAction(event -> {
            // Exit logic here
        });
    }

    private void openFilePopup() {
        openFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Subtitle File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Subtitle Files", "*.srt", "*.vtt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            Stage stage = (Stage) openFileButton.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                reloadSubtitleTable(selectedFile);
            }
        });
    }

    private void reloadSubtitleTable(File file) {
        // Placeholder: In a real application, parse the file and load subtitles
        // For now, just print the file path and reload with dummy data
        System.out.println("Reloading subtitles from: " + file.getAbsolutePath());

        var subtitleData = FXCollections.observableArrayList(
                new Subtitle(1, "Loaded subtitle 1 from file", "Additional info 1"),
                new Subtitle(2, "Loaded subtitle 2 from file", "Additional info 2"),
                new Subtitle(3, "Loaded subtitle 3 from file", "Additional info 3")
        );

        subtitleTable.setItems(subtitleData);

        if (!subtitleData.isEmpty()) {
            subtitleTable.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(subtitleData.getFirst().getPrimaryText());
            if (projector != null) {
                projector.setText(subtitleData.getFirst().getPrimaryText());
            }
        }
    }

    private void settingsPopup() {
        settingsButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings_popup_view.fxml"));
                Parent root = loader.load();

                SettingsController settingsController = loader.getController();
                settingsController.setProjector(projector);

                Stage popup = new SublyApplicationStage();
                popup.initOwner(toggleShowScreenButton.getScene().getWindow());
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
        prevButton.setOnAction(e -> {
            int currentIndex = subtitleTable.getSelectionModel().getSelectedIndex();
            if (currentIndex > 0) {
                subtitleTable.getSelectionModel().selectPrevious();
                subtitleTable.scrollTo(subtitleTable.getSelectionModel().getSelectedIndex());
            }
        });

        nextButton.setOnAction(e -> {
            int currentIndex = subtitleTable.getSelectionModel().getSelectedIndex();
            if (currentIndex < subtitleTable.getItems().size() - 1) {
                subtitleTable.getSelectionModel().selectNext();
                subtitleTable.scrollTo(subtitleTable.getSelectionModel().getSelectedIndex());
            }
        });
    }

    private void subtitleTable() {
        subtitleTable.setEditable(true);
        primaryTextColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        secondaryTextColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        primaryTextColumn.setEditable(true);
        secondaryTextColumn.setEditable(true);

        primaryTextColumn.setOnEditCommit(event -> {
            Subtitle subtitle = event.getRowValue();
            subtitle.setPrimaryText(event.getNewValue());
        });
        secondaryTextColumn.setOnEditCommit(event -> {
            Subtitle subtitle = event.getRowValue();
            subtitle.setSecondaryText(event.getNewValue());
        });

        // Map columns to fields
        indexColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        primaryTextColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPrimaryText()));
        secondaryTextColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSecondaryText()));

        // Create test data
        var subtitleData = FXCollections.observableArrayList(
                new Subtitle(1, "Hello world", "Additional info"),
                new Subtitle(2, "This is a subtitle", "More info"),
                new Subtitle(3, "Final line!", "End info")
        );

        // Push data into table
        subtitleTable.setItems(subtitleData);

        if (!subtitleData.isEmpty()) {
            subtitleTable.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(subtitleData.getFirst().getPrimaryText());
        }

        subtitleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                currentSubtitleLabel.setText(newSel.getPrimaryText());

                if (projector != null) {
                    projector.setText(newSel.getPrimaryText());
                }
            }
        });
    }

    private void toggleButton() {
        if (toggleShowScreenButton.isSelected()) {
            toggleShowScreenButton.setText("Close Show Screen");
        } else {
            toggleShowScreenButton.setText("Open Show Screen");
        }

        toggleShowScreenButton.setOnAction(e -> {
            if (toggleShowScreenButton.isSelected()) {
                // Open show screen
                if (projector == null) {
                    projector = new Projector();
                }
                projector.show();
                toggleShowScreenButton.setText("Close Show Screen");
                // immediately set the text from currently selected row
                String selected = subtitleTable.getSelectionModel().getSelectedItem().getPrimaryText();
                if (selected != null) {
                    projector.setText(selected);
                }
            } else {
                if (projector != null) {
                    projector.hide();
                    toggleShowScreenButton.setText("Open Show Screen");
                }
            }
        });
    }
}
