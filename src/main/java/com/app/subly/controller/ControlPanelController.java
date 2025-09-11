package com.app.subly.controller;

import com.app.subly.SublyApplication;
import com.app.subly.common.MultilineTableCell;
import com.app.subly.common.SublyApplicationStage;
import com.app.subly.component.Projector;
import com.app.subly.model.SublyProjectFile;
import com.app.subly.model.Subtitle;
import com.app.subly.persistence.AppSettingsManager;
import com.app.subly.persistence.SublyProjectFileManager;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ControlPanelController {

    private SublyApplication app;
    private Projector projector;

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
    private MenuBar menuBar;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem settingsMenuItem;
    @FXML
    private MenuItem exitMenuItem;

    public void setShowScreen(SublyApplication app, Projector projector) {
        this.app = app;
        this.projector = projector;
    }

    @FXML
    private void initialize() {
        subtitleTable();
        toggleButton();
        navigationButtons();
        settingsPopup();
        menuBar();
    }

    private void menuBar() {
        openMenuItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Subtitle File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Subtitle Files", "*.subly")
            );

            Stage stage = new SublyApplicationStage();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                try {
                    SublyProjectFileManager manager = new SublyProjectFileManager();
                    SublyProjectFile projectFile = manager.loadProject(selectedFile);

                    // Update settings + table
                    app.updateSetting(projectFile.getSettings());
                    app.updateTitle(projectFile.getFileName());
                    reloadSubtitleTable(projectFile.getSubtitles());
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO: show alert dialog to user
                }

            }
        });

        saveMenuItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Subly project");

            // suggest default extension
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Subly Project Files", "*.subly")
            );

            // default file name
            fileChooser.setInitialFileName("my_project.subly");

            Stage stage = new SublyApplicationStage();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                // ensure it ends with .subly
                String path = file.getAbsolutePath();
                if (!path.endsWith(".subly")) {
                    file = new File(path + ".subly");
                }

                try {
                    SublyProjectFile currentProjectFile = new SublyProjectFile(
                            file.getAbsolutePath(),
                            AppSettingsManager.load(),
                            subtitleTable.getItems()
                    );

                    SublyProjectFileManager manager = new SublyProjectFileManager();
                    manager.saveProject(currentProjectFile);
                    System.out.println("Project saved to: " + file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        exitMenuItem.setOnAction(event -> {
            // Exit logic here
        });
    }

    private void reloadSubtitleTable(List<Subtitle> subtitles) {
        var subtitleData = FXCollections.observableArrayList(subtitles);

        subtitleTable.setItems(subtitleData);

        if (!subtitleData.isEmpty()) {
            subtitleTable.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(subtitleData.getFirst().getPrimaryText().replace("\\n", "\n"));
            if (projector != null) {
                projector.setText(subtitleData.getFirst().getPrimaryText().replace("\\n", "\n"));
            }
        }
    }

    private void settingsPopup() {
        settingsMenuItem.setOnAction(e -> {
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
        subtitleTable.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                int row = subtitleTable.getSelectionModel().getSelectedIndex();
                int lastRow = subtitleTable.getItems().size() - 1;

                // If we’re at the last row, add a new empty Subtitle
                if (row == lastRow) {
                    Subtitle newSubtitle = new Subtitle(
                            lastRow + 2, // generate id (simple increment)
                            "",          // primary empty
                            ""           // secondary empty
                    );
                    subtitleTable.getItems().add(newSubtitle);

                    // Move selection to new row, primaryTextColumn
                    subtitleTable.getSelectionModel().select(newSubtitle);
                    subtitleTable.getFocusModel().focus(lastRow + 1, primaryTextColumn);

                    // Put the new cell into edit mode
                    subtitleTable.edit(lastRow + 1, primaryTextColumn);

                    event.consume(); // swallow the Tab key
                }
            }
        });


        primaryTextColumn.setCellFactory(col -> new MultilineTableCell());
        secondaryTextColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        primaryTextColumn.setEditable(true);
        secondaryTextColumn.setEditable(true);

        primaryTextColumn.setOnEditCommit(event -> {
            Subtitle subtitle = event.getRowValue();
            subtitle.setPrimaryText(event.getNewValue());

            // Update label if this row is selected
            if (subtitleTable.getSelectionModel().getSelectedItem() == subtitle) {
                currentSubtitleLabel.setText(event.getNewValue().replace("\\n", "\n"));
                if (projector != null) {
                    projector.setText(event.getNewValue().replace("\\n", "\n"));
                }
            }
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
                new Subtitle(1, "This line will be showed on screen", "This line is for translation")
        );

        // Push data into table
        subtitleTable.setItems(subtitleData);

        if (!subtitleData.isEmpty()) {
            subtitleTable.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(subtitleData.getFirst().getPrimaryText().replace("\\n", "\n"));
        }

        subtitleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                currentSubtitleLabel.setText(newSel.getPrimaryText().replace("\\n", "\n"));

                if (projector != null) {
                    projector.setText(newSel.getPrimaryText().replace("\\n", "\n"));
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
