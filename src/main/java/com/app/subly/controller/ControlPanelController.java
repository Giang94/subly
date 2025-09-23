package com.app.subly.controller;

import com.app.subly.SublyApplication;
import com.app.subly.component.SublyApplicationStage;
import com.app.subly.component.*;
import com.app.subly.model.*;
import com.app.subly.persistence.SublyProjectFileManager;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ControlPanelController {

    private static final int MIN_FONT_SIZE = 24;
    private static final int MAX_FONT_SIZE = 240;

    private SublyApplication app;
    private Projector projector;
    private SublyProjectSession session;

    // Parts
    private final TrailingBlankRowPolicy trailingRowPolicy = new TrailingBlankRowPolicy();
    private PasteManager pasteManager;
    private StyleToolbarBinder styleBinder;
    private EditHistory history;

    // FXML
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

    // Toolbar
    // Text settings
    @FXML
    private TextField fontSizeField;
    @FXML
    private Button fontSizeDownButton;
    @FXML
    private Button fontSizeUpButton;
    @FXML
    private ColorPicker textColorPicker;
    // Background controls
    @FXML
    private RadioButton bgTransparentRadio;
    @FXML
    private RadioButton bgColorRadio;
    @FXML
    private RadioButton bgImageRadio;
    @FXML
    private ToggleGroup bgToggleGroup;
    @FXML
    private ColorPicker bgColorPicker;
    @FXML
    private Button chooseImageButton;
    @FXML
    private TextField imagePathField;

    // Menu
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem newMenuItem;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem saveAsMenuItem;
    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;

    @FXML
    private void initialize() {
        configureCurrentSubtitleLabel();
        setupTableBasics();
        setupNavigation();
        setupMenuBarBasics();
        toggleButton();
        setupEditMenu();
        backgroundControlsSetup();

        // Demo data + ensure trailing blank row
        ObservableList<Subtitle> data = FXCollections.observableArrayList(
                new Subtitle(1, "This line will be showed on screen", "This line is for translation")
        );
        subtitleTable.setItems(data);
        trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);

        // History: refresh table + enforce trailing blank after undo/redo; mark dirty on any change
        history = new EditHistory(
                () -> {
                    trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
                    RowIndexer.renumber(subtitleTable.getItems());
                    subtitleTable.refresh();
                },
                this::markDirty
        );

        // Paste manager with undo
        // Paste manager records into shared history
        pasteManager = new PasteManager(
                subtitleTable,
                primaryTextColumn,
                secondaryTextColumn,
                indexColumn,
                () -> new Subtitle(subtitleTable.getItems().size() + 1, "", ""),
                () -> trailingRowPolicy.ensureTrailingBlankRow(subtitleTable),
                this::markDirty,
                history
        );
        pasteManager.install();

        // Update projector and current label on selection
        subtitleTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            String text = n.getPrimaryText() == null ? "" : n.getPrimaryText().replace("\\n", "\n");
            currentSubtitleLabel.setText(text);
            if (projector != null) projector.setText(text);
        });
        if (!data.isEmpty()) {
            subtitleTable.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(data.getFirst().getPrimaryText().replace("\\n", "\n"));
        }

        Platform.runLater(this::ensureInitialChapter);
    }

    private void backgroundControlsSetup() {
        if (bgToggleGroup == null) {
            bgToggleGroup = new ToggleGroup();
            bgTransparentRadio.setToggleGroup(bgToggleGroup);
            bgColorRadio.setToggleGroup(bgToggleGroup);
            bgImageRadio.setToggleGroup(bgToggleGroup);
        }

        // Enable/disable controls based on selected background type
        bgColorPicker.disableProperty().bind(bgColorRadio.selectedProperty().not());
        chooseImageButton.disableProperty().bind(bgImageRadio.selectedProperty().not());
        imagePathField.disableProperty().bind(bgImageRadio.selectedProperty().not());
    }

    @FXML
    public void onChooseImage(ActionEvent event) {
        Window owner = (menuBar != null && menuBar.getScene() != null) ? menuBar.getScene().getWindow() : null;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Background Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = chooser.showOpenDialog(owner);
        if (file != null) {
            // Store as URI or absolute path as needed by your model
            imagePathField.setText(file.toURI().toString());
            applyBackgroundChange();
        }
    }

    private void applyBackgroundChange() {
        if (session == null) return;

        session.update(s -> {
            if (bgTransparentRadio != null && bgTransparentRadio.isSelected()) {
                s.setBackgroundType(BackgroundType.TRANSPARENT);
                s.setProjectorImageUri(null); // clear any previous image
            } else if (bgColorRadio != null && bgColorRadio.isSelected()) {
                s.setBackgroundType(BackgroundType.SOLID_COLOR);
                Color c = (bgColorPicker != null && bgColorPicker.getValue() != null)
                        ? bgColorPicker.getValue() : Color.BLACK;
                s.setProjectorColor(ColorConvertUtils.toHexString(c));
                s.setProjectorImageUri(null); // clear any previous image
            } else if (bgImageRadio != null && bgImageRadio.isSelected()) {
                s.setBackgroundType(BackgroundType.IMAGE);
                String uri = imagePathField != null ? imagePathField.getText() : null;
                s.setProjectorImageUri(uri);
            }
        });

        if (projector != null) {
            projector.applySettings(session.getSettings());
        }
        // applyCurrentLabelBackground(session.getSettings());
        markDirty();
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
                applyBackgroundChange();
            } else {
                if (projector != null) {
                    projector.hide();
                    toggleShowScreenButton.setText("Open Show Screen");
                }
            }
        });
    }

    private void setupEditMenu() {
        if (menuBar == null) return;

        // Accelerators
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        // Redo: Ctrl+Y (you can also add Ctrl+Shift+Z if desired)
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));

        // Actions
        undoMenuItem.setOnAction(e -> {
            if (history != null) history.undo();
        });
        redoMenuItem.setOnAction(e -> {
            if (history != null) history.redo();
        });
        if (history != null) {
            undoMenuItem.disableProperty().bind(history.canUndoProperty().not());
            redoMenuItem.disableProperty().bind(history.canRedoProperty().not());
        }
    }

    public void setShowScreen(SublyApplication app, Projector projector) {
        this.app = app;
        this.projector = projector;
        if (styleBinder != null) styleBinder.rebind(session, projector);
    }

    public void setSession(SublyProjectSession session) {
        this.session = session;

        if (styleBinder == null) {
            // Pass null for the old checkbox (now replaced by radio buttons)
            styleBinder = new StyleToolbarBinder(
                    MIN_FONT_SIZE, MAX_FONT_SIZE,
                    fontSizeField, fontSizeDownButton, fontSizeUpButton,
                    textColorPicker,
                    this::currentFontSize,
                    this::applySettingsToProjector,
                    this::markDirty
            );
        }

        // Reflect current settings into the radio group and color picker
        SublySettings settings = (session != null) ? session.getSettings() : null;
        if (settings != null) {
            if (settings.isProjectorTransparent()) {
                if (bgTransparentRadio != null) bgTransparentRadio.setSelected(true);
            } else {
                if (bgColorRadio != null) bgColorRadio.setSelected(true);
                if (bgColorPicker != null && settings.getProjectorColor() != null) {
                    bgColorPicker.setValue(ColorConvertUtils.toJavaFxColor(settings.getProjectorColor()));
                }
            }
        }

        // Propagate radio/color changes to session/projector
        if (bgToggleGroup != null) {
            bgToggleGroup.selectedToggleProperty().addListener((obs, o, n) -> applyBackgroundChange());
        }
        if (bgColorPicker != null) {
            bgColorPicker.valueProperty().addListener((obs, o, n) -> {
                if (bgColorRadio != null && bgColorRadio.isSelected()) applyBackgroundChange();
            });
        }

        styleBinder.rebind(session, projector);

        // Apply initial background based on the current selection
        applyBackgroundChange();
    }

    private void setupTableBasics() {
        subtitleTable.setEditable(true);

        // Row add via TAB (record as history)
        subtitleTable.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                int row = subtitleTable.getSelectionModel().getSelectedIndex();
                int last = subtitleTable.getItems().size() - 1;
                if (row == last && !trailingRowPolicy.isBlankRow(subtitleTable.getItems().get(last))) {
                    Subtitle s = new Subtitle(last + 2, "", "");
                    subtitleTable.getItems().add(s);
                    // Record: remove last row (undo) / add one row (redo)
                    if (history != null) {
                        history.push(EditHistory.of(
                                () -> {
                                    if (!subtitleTable.getItems().isEmpty())
                                        subtitleTable.getItems().remove(subtitleTable.getItems().size() - 1);
                                },
                                () -> subtitleTable.getItems().add(new Subtitle(subtitleTable.getItems().size() + 1, "", ""))
                        ));
                    }
                    subtitleTable.getSelectionModel().select(s);
                    subtitleTable.getFocusModel().focus(last + 1, primaryTextColumn);
                    subtitleTable.edit(last + 1, primaryTextColumn);
                    e.consume();
                    markDirty();
                }
            } else if (e.getCode() == KeyCode.DELETE && !e.isControlDown() && !e.isAltDown()) {
                // Delete selected rows (skip trailing blank), record as compound edit
                var items = subtitleTable.getItems();
                var indices = new ArrayList<>(subtitleTable.getSelectionModel().getSelectedIndices());
                if (indices.isEmpty()) return;
                indices.sort(Comparator.naturalOrder());

                // Snapshot rows to remove, excluding trailing blank row
                List<Integer> keptIdx = new ArrayList<>();
                List<Subtitle> keptRows = new ArrayList<>();
                for (int idx : indices) {
                    if (idx >= 0 && idx < items.size()) {
                        Subtitle s = items.get(idx);
                        if (trailingRowPolicy.isBlankRow(s) && idx == items.size() - 1) continue; // skip trailing blank
                        keptIdx.add(idx);
                        keptRows.add(s);
                    }
                }
                if (keptIdx.isEmpty()) return;

                // Apply removal now (desc order)
                for (int i = keptIdx.size() - 1; i >= 0; i--) {
                    int idx = keptIdx.get(i);
                    if (idx >= 0 && idx < items.size()) items.remove(idx);
                }
                trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
                RowIndexer.renumber(items);
                subtitleTable.refresh();
                markDirty();

                // Record undo/redo
                if (history != null) {
                    EditHistory.CompoundEdit ce = new EditHistory.CompoundEdit();
                    ce.add(EditHistory.of(
                            // undo: insert back in asc index order
                            () -> {
                                for (int i = 0; i < keptIdx.size(); i++) {
                                    int at = Math.min(keptIdx.get(i), subtitleTable.getItems().size());
                                    subtitleTable.getItems().add(at, keptRows.get(i));
                                }
                            },
                            // redo: remove again in desc order
                            () -> {
                                for (int i = keptIdx.size() - 1; i >= 0; i--) {
                                    int at = keptIdx.get(i);
                                    if (at >= 0 && at < subtitleTable.getItems().size()) {
                                        subtitleTable.getItems().remove(at);
                                    }
                                }
                            }
                    ));
                    history.push(ce);
                }
                e.consume();
            }
        });

        // Cell factories
        primaryTextColumn.setCellFactory(col -> new MultilineTableCell());
        secondaryTextColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        primaryTextColumn.setEditable(true);
        secondaryTextColumn.setEditable(true);

        // Track cell edits into history
        primaryTextColumn.setOnEditCommit(ev -> {
            int rowIndex = ev.getTablePosition().getRow();
            Subtitle row = ev.getRowValue();
            String oldV = ev.getOldValue();
            String newV = ev.getNewValue();
            row.setPrimaryText(newV);
            if (subtitleTable.getSelectionModel().getSelectedItem() == row) {
                String text = newV == null ? "" : newV.replace("\\n", "\n");
                currentSubtitleLabel.setText(text);
                if (projector != null) projector.setText(text);
            }
            if (history != null) {
                history.push(EditHistory.of(
                        () -> {
                            if (rowIndex < subtitleTable.getItems().size())
                                subtitleTable.getItems().get(rowIndex).setPrimaryText(oldV);
                        },
                        () -> {
                            if (rowIndex < subtitleTable.getItems().size())
                                subtitleTable.getItems().get(rowIndex).setPrimaryText(newV);
                        }
                ));
            }
            trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
            markDirty();
        });
        secondaryTextColumn.setOnEditCommit(ev -> {
            int rowIndex = ev.getTablePosition().getRow();
            String oldV = ev.getOldValue();
            String newV = ev.getNewValue();
            ev.getRowValue().setSecondaryText(newV);
            if (history != null) {
                history.push(EditHistory.of(
                        () -> {
                            if (rowIndex < subtitleTable.getItems().size())
                                subtitleTable.getItems().get(rowIndex).setSecondaryText(oldV);
                        },
                        () -> {
                            if (rowIndex < subtitleTable.getItems().size())
                                subtitleTable.getItems().get(rowIndex).setSecondaryText(newV);
                        }
                ));
            }
            trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
            markDirty();
        });

        // Value factories
        indexColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getId()).asObject());
        primaryTextColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrimaryText()));
        secondaryTextColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getSecondaryText()));
    }

    private void setupNavigation() {
        prevButton.setOnAction(e -> {
            int i = subtitleTable.getSelectionModel().getSelectedIndex();
            if (i > 0) {
                subtitleTable.getSelectionModel().selectPrevious();
                subtitleTable.scrollTo(subtitleTable.getSelectionModel().getSelectedIndex());
            }
        });
        nextButton.setOnAction(e -> {
            int i = subtitleTable.getSelectionModel().getSelectedIndex();
            if (i < subtitleTable.getItems().size() - 1) {
                subtitleTable.getSelectionModel().selectNext();
                subtitleTable.scrollTo(subtitleTable.getSelectionModel().getSelectedIndex());
            }
        });
    }

    private void setupMenuBarBasics() {
        newMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveAsMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        openMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.O, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        wireMenuActions();
    }

    private void wireMenuActions() {
        if (newMenuItem != null) {
            newMenuItem.setOnAction(e -> newProject());
        }
        if (openMenuItem != null) {
            openMenuItem.setOnAction(e -> openProject());
        }
        if (saveMenuItem != null) {
            saveMenuItem.setOnAction(e -> saveProject());
        }
        if (saveAsMenuItem != null) {
            saveAsMenuItem.setOnAction(e -> saveProjectAs());
        }
        if (exitMenuItem != null) {
            exitMenuItem.setOnAction(e -> requestExit());
        }
    }

    private void newProject() {
        if (!confirmNewWithUnsavedCheck()) return;

        // Reset table to a single blank row
        ObservableList<Subtitle> data = FXCollections.observableArrayList();
        data.add(new Subtitle(1, "", ""));
        subtitleTable.setItems(data);
        subtitleTable.getSelectionModel().selectFirst();
        subtitleTable.scrollTo(0);

        // Clear current display/projector
        if (currentSubtitleLabel != null) currentSubtitleLabel.setText("");
        if (projector != null) projector.setText("");

        // Clear project file and update title
        if (session != null) session.setProjectFile(null);
        if (app != null) app.updateTitle("Untitled");

        setDirty(false);
        refreshActions();
    }

    private void openProject() {
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

                if (session != null) {
                    session.setProjectFile(selectedFile);
                }

                // Update app title/settings and table
                app.updateSetting(projectFile.getSettings());
                app.updateTitle(projectFile.getFileName());
                reloadSubtitleTable(projectFile.getChapters().get(0).getSubtitles());

                // Read settings from file
                SublySettings loaded = projectFile.getSettings();
                final int sizeToApply = Math.clamp(loaded.getSubtitleFontSize(), MIN_FONT_SIZE, MAX_FONT_SIZE);
                final Color textFx = ColorConvertUtils.toJavaFxColor(loaded.getSubtitleColor());
                final boolean transparent = loaded.isProjectorTransparent();
                final Color bgFx = ColorConvertUtils.toJavaFxColor(loaded.getProjectorColor());

                // Reflect in UI
                if (fontSizeField != null) fontSizeField.setText(String.valueOf(sizeToApply));
                if (textColorPicker != null) textColorPicker.setValue(textFx);
                if (bgColorPicker != null) {
                    Color pickerBg = transparent
                            ? new Color(bgFx.getRed(), bgFx.getGreen(), bgFx.getBlue(), 0.0)
                            : bgFx;
                    bgColorPicker.setValue(pickerBg);
                }

                // Update session and apply to Projector
                if (session != null) {
                    session.update(s -> {
                        s.setSubtitleFontSize(sizeToApply);
                        s.setSubtitleColor(ColorConvertUtils.toHexString(textFx));
                        s.setProjectorTransparent(transparent);
                        if (!transparent) {
                            s.setProjectorColor(ColorConvertUtils.toHexString(bgFx));
                        }
                    });
                }
                if (projector != null && session != null) {
                    projector.applySettings(session.getSettings());
                }

                setDirty(false);
                refreshActions();
            } catch (IOException e) {
            }
        }
    }

    private void saveProject() {
        SublyProjectFileManager manager = new SublyProjectFileManager();
        manager.save(subtitleTable, session);
        setDirty(false);
    }

    private void saveProjectAs() {
        SublyProjectFileManager manager = new SublyProjectFileManager();
        manager.saveAs(subtitleTable, session);
        File f = (session != null) ? session.getProjectFile() : null;
        if (f != null && f.exists()) setDirty(false);
    }

    private void requestExit() {
        if (confirmExitWithUnsavedCheck()) {
            Platform.exit();
        }
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
        trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
    }

    public void refreshActions() {
        if (session == null) {
            saveMenuItem.setDisable(true);
            return;
        }
        File f = session.getProjectFile();
        saveMenuItem.setDisable(f == null || !f.exists());
    }

    private boolean confirmExitWithUnsavedCheck() {
        if (!dirty.get()) return true;

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType dontSaveBtn = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to save your changes before exiting?");
        alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);

        ButtonType chosen = alert.showAndWait().orElse(cancelBtn);
        if (chosen == saveBtn) {
            return attemptSaveForExit();
        } else if (chosen == dontSaveBtn) {
            return true;
        } else {
            return false;
        }
    }

    private boolean confirmNewWithUnsavedCheck() {
        if (!dirty.get()) return true;

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType dontSaveBtn = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to save your changes before creating a new project?");
        alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);

        ButtonType chosen = alert.showAndWait().orElse(cancelBtn);
        if (chosen == saveBtn) {
            return attemptSaveForExit(); // reuse same save path
        } else if (chosen == dontSaveBtn) {
            return true;
        } else {
            return false;
        }
    }

    private boolean attemptSaveForExit() {
        SublyProjectFileManager manager = new SublyProjectFileManager();

        // If we have a target, save; else prompt Save As
        File target = (session != null) ? session.getProjectFile() : null;
        if (target != null && target.exists()) {
            manager.save(subtitleTable, session);
        } else {
            manager.saveAs(subtitleTable, session);
        }

        // Consider successful if a project file exists after save
        File f = (session != null) ? session.getProjectFile() : null;
        boolean success = (f != null && f.exists());
        if (success) setDirty(false);
        return success;
    }

    private void configureCurrentSubtitleLabel() {
        if (currentSubtitleLabel == null) return;
        currentSubtitleLabel.setWrapText(false);
        currentSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        currentSubtitleLabel.setAlignment(Pos.TOP_CENTER);

        Rectangle clip = new Rectangle();
        currentSubtitleLabel.setClip(clip);
        final Text measurer = new Text("Wg\nWg\nWg\nWg");

        Runnable applyHeights = () -> {
            currentSubtitleLabel.applyCss();
            currentSubtitleLabel.layout();

            measurer.setFont(currentSubtitleLabel.getFont());
            double left = currentSubtitleLabel.getPadding().getLeft();
            double right = currentSubtitleLabel.getPadding().getRight();
            double top = currentSubtitleLabel.getPadding().getTop();
            double bottom = currentSubtitleLabel.getPadding().getBottom();

            double contentWidth = Math.max(0, currentSubtitleLabel.getWidth() - left - right);
            measurer.setWrappingWidth(contentWidth);

            double textHeight = Math.ceil(measurer.getLayoutBounds().getHeight());
            double threeLines = textHeight + top + bottom + 1;

            currentSubtitleLabel.setMinHeight(threeLines);
            currentSubtitleLabel.setPrefHeight(threeLines);
            currentSubtitleLabel.setMaxHeight(threeLines);

            clip.setWidth(currentSubtitleLabel.getWidth());
            clip.setHeight(threeLines);
        };

        currentSubtitleLabel.widthProperty().addListener((o, ov, nv) -> applyHeights.run());
        currentSubtitleLabel.fontProperty().addListener((o, ov, nv) -> applyHeights.run());
        currentSubtitleLabel.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) Platform.runLater(applyHeights);
        });
    }

    private void applySettingsToProjector(int size, Color textColor) {
        if (projector == null) return;
        projector.setFontSize(size);
        projector.applySettings(session != null ? session.getSettings() : null);
    }

    private void applyCurrentLabelBackground(SublySettings s) {
        if (currentSubtitleLabel == null || s == null) return;

        String style;
        switch (s.getBackgroundType()) {
            case TRANSPARENT -> {
                style = "-fx-background-color: transparent;";
            }
            case SOLID_COLOR -> {
                Color c = ColorConvertUtils.toJavaFxColor(s.getProjectorColor());
                int r = (int) Math.round(c.getRed() * 255);
                int g = (int) Math.round(c.getGreen() * 255);
                int b = (int) Math.round(c.getBlue() * 255);
                double a = c.getOpacity();
                style = String.format("-fx-background-color: rgba(%d, %d, %d, %.3f);", r, g, b, a);
            }
            case IMAGE -> {
                String uri = s.getProjectorImageUri();
                if (uri == null || uri.isBlank()) {
                    style = "-fx-background-color: transparent;";
                } else {
                    String cssUrl = uri.replace("\\", "/");
                    style = String.format(
                            "-fx-background-image: url(\"%s\"); " +
                                    "-fx-background-size: cover; " +
                                    "-fx-background-position: center center; " +
                                    "-fx-background-repeat: no-repeat;",
                            cssUrl
                    );
                }
            }
            default -> style = "-fx-background-color: transparent;";
        }
        currentSubtitleLabel.setStyle(style);
    }

    // Dirty handling (same semantics as before)
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    private void setDirty(boolean value) {
        dirty.set(value);
        if (app != null) app.setDirty(value);
    }

    private void markDirty() {
        setDirty(true);
    }

    private int currentFontSize() {
        if (fontSizeField == null) return MIN_FONT_SIZE;
        try {
            int v = Integer.parseInt(fontSizeField.getText() == null ? String.valueOf(MIN_FONT_SIZE) : fontSizeField.getText().trim());
            return Math.clamp(v, MIN_FONT_SIZE, MAX_FONT_SIZE);
        } catch (NumberFormatException ex) {
            return MIN_FONT_SIZE;
        }
    }

    // Handle chapters
    // Existing injections
    @FXML private ListView<Chapter> chapterListView;
    @FXML private Label chapterCountLabel;

    private void ensureInitialChapter() {
        if (chapterListView == null) {
            throw new IllegalStateException("chapterListView is not injected.");
        }
        ObservableList<Chapter> items = chapterListView.getItems();
        if (items.isEmpty()) {
            Chapter chapter = createDefaultChapter(1);
            items.add(chapter);
            chapterListView.getSelectionModel().select(chapter);
            updateChapterCount();

            ObservableList<Subtitle> data = FXCollections.observableArrayList();
            data.add(new Subtitle(1, "", ""));
        } else {
            // Ensure the selected (or first) chapter has at least one empty subtitle
            Chapter current = chapterListView.getSelectionModel().getSelectedItem();
            if (current == null) current = items.get(0);
            if (current.getSubtitles().isEmpty()) {
                current.getSubtitles().add(newEmptySubtitle());
            }
        }
    }

    private Chapter createDefaultChapter(int index) {
        Chapter c = new Chapter();
        c.setTitle("Chapter " + index);
        c.getSubtitles().add(newEmptySubtitle());
        return c;
    }

    private Subtitle newEmptySubtitle() {
        Subtitle s = new Subtitle();
        // Adjust to your model's field names if different
        s.setPrimaryText("");
        s.setSecondaryText("");
        return s;
    }

    private void updateChapterCount() {
        if (chapterCountLabel != null) {
            chapterCountLabel.setText(String.valueOf(chapterListView.getItems().size()));
        }
    }
}