package com.app.subly.controller;

import com.app.subly.SublyApplication;
import com.app.subly.common.MultilineTableCell;
import com.app.subly.common.SublyApplicationStage;
import com.app.subly.component.Projector;
import com.app.subly.model.SublyProjectFile;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import com.app.subly.persistence.SublyProjectFileManager;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.UnaryOperator;

public class ControlPanelController {

    private static final int MIN_FONT_SIZE = 24;
    private static final int MAX_FONT_SIZE = 240;

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
    private MenuItem saveAsMenuItem;
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
        menuBar();
        configureCurrentSubtitleLabel();
    }

    private void configureCurrentSubtitleLabel() {
        if (currentSubtitleLabel == null) return;

        currentSubtitleLabel.setWrapText(false);
        currentSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        currentSubtitleLabel.setAlignment(Pos.TOP_CENTER);

        Rectangle clip = new Rectangle();
        currentSubtitleLabel.setClip(clip);

        final Text measurer = new Text("Wg\nWg\nWg\nWg"); // includes ascenders/descenders

        Runnable applyHeights = () -> {
            // Ensure CSS/layout are up-to-date
            currentSubtitleLabel.applyCss();
            currentSubtitleLabel.layout();

            measurer.setFont(currentSubtitleLabel.getFont());

            // Content area width = label width - padding
            double left = currentSubtitleLabel.getPadding().getLeft();
            double right = currentSubtitleLabel.getPadding().getRight();
            double top = currentSubtitleLabel.getPadding().getTop();
            double bottom = currentSubtitleLabel.getPadding().getBottom();

            double contentWidth = Math.max(0, currentSubtitleLabel.getWidth() - left - right);
            measurer.setWrappingWidth(contentWidth);

            // Height for exactly 3 lines of text
            double textHeight = Math.ceil(measurer.getLayoutBounds().getHeight());

            // Add padding and a small fudge to avoid clipping descenders
            double threeLines = textHeight + top + bottom + 1; // +1 px guard

            // Reserve and cap to 3 lines
            currentSubtitleLabel.setMinHeight(threeLines);
            currentSubtitleLabel.setPrefHeight(threeLines);
            currentSubtitleLabel.setMaxHeight(threeLines);

            // Clip to the same visual height
            clip.setWidth(currentSubtitleLabel.getWidth());
            clip.setHeight(threeLines);
        };

        // Recompute on width or font changes
        currentSubtitleLabel.widthProperty().addListener((o, ov, nv) -> applyHeights.run());
        currentSubtitleLabel.fontProperty().addListener((o, ov, nv) -> applyHeights.run());

        // Run once after first layout pass
        currentSubtitleLabel.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                // Defer to ensure width is valid
                Platform.runLater(applyHeights);
            }
        });
    }

    private void menuBar() {
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveAsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));

        saveMenuItem.setDisable(true);
        bindSaveAvailability();

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

                    if (session != null) {
                        session.setProjectFile(selectedFile);
                    }

                    // Update app title/settings and table
                    app.updateSetting(projectFile.getSettings());
                    app.updateTitle(projectFile.getFileName());
                    reloadSubtitleTable(projectFile.getSubtitles());

                    // Read settings from file
                    SublySettings loaded = projectFile.getSettings();
                    final int sizeToApply = clamp(loaded.getSubtitleFontSize());
                    final Color textFx = ColorConvertUtils.toJavaFxColor(loaded.getSubtitleColor());
                    final boolean transparent = loaded.isProjectorTransparent();
                    final Color bgFx = ColorConvertUtils.toJavaFxColor(loaded.getProjectorColor());

                    // Reflect in UI
                    if (fontSizeField != null) fontSizeField.setText(String.valueOf(sizeToApply));
                    if (textColorPicker != null) textColorPicker.setValue(textFx);
                    if (bgTransparentCheckBox != null) bgTransparentCheckBox.setSelected(transparent);
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
        });

        saveMenuItem.setOnAction(event -> {
            SublyProjectFileManager manager = new SublyProjectFileManager();
            manager.save(subtitleTable, session);
            setDirty(false);
        });

        saveAsMenuItem.setOnAction(event -> {
            SublyProjectFileManager manager = new SublyProjectFileManager();
            manager.saveAs(subtitleTable, session);
            File f = (session != null) ? session.getProjectFile() : null;
            if (f != null && f.exists()) setDirty(false);
        });

        exitMenuItem.setOnAction(event -> {
            // Exit logic here
            if (confirmExitWithUnsavedCheck()) {
                Platform.exit();
            }
        });

        installWindowCloseHandler();
    }

    private void bindSaveAvailability() {
        // Clear any previous binding/update
        saveMenuItem.disableProperty().unbind();

        if (session == null) {
            saveMenuItem.setDisable(true);
            return;
        }

        // Try to bind to an observable projectFileProperty() if available
        try {
            Method m = session.getClass().getMethod("projectFileProperty");
            Object prop = m.invoke(session);
            if (prop instanceof ObservableValue) {
                @SuppressWarnings("unchecked")
                ObservableValue<File> fileObs = (ObservableValue<File>) prop;

                BooleanBinding noFileOrMissing = Bindings.createBooleanBinding(
                        () -> {
                            File f = fileObs.getValue();
                            return f == null || !f.exists();
                        },
                        fileObs
                );
                saveMenuItem.disableProperty().bind(noFileOrMissing);
                return;
            }
        } catch (Exception ignored) {
        }
        refreshActions();
    }

    public void refreshActions() {
        if (session == null) {
            saveMenuItem.setDisable(true);
            return;
        }
        File f = session.getProjectFile();
        saveMenuItem.setDisable(f == null || !f.exists());
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
                    markDirty();
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
            markDirty();
        });

        secondaryTextColumn.setOnEditCommit(event -> {
            Subtitle subtitle = event.getRowValue();
            subtitle.setSecondaryText(event.getNewValue());
            markDirty();
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

    // Project SESSION
    private SublyProjectSession session;

    public void setSession(SublyProjectSession session) {
        this.session = session;
        bindSaveAvailability();
        initFromSession();
        initStyleToolbar();
        refreshActions();
    }

    private void initFromSession() {
        if (session == null) return;
        int size = session.getSettings().getSubtitleFontSize();
        applyFontSize(size);
    }

    private void applyFontSize(int size) {
        if (projector != null) {
            projector.setFontSize(size);
        }
    }

    // Handle dirty state
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    private void setDirty(boolean value) {
        dirty.set(value);
        app.setDirty(value);
    }

    private void markDirty() {
        dirty.set(true);
        app.setDirty(true);
    }

    private void installWindowCloseHandler() {
        // Ensure we attach once scene/window are available
        menuBar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            if (newScene.getWindow() != null) {
                newScene.getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                    if (!confirmExitWithUnsavedCheck()) e.consume();
                });
            } else {
                newScene.windowProperty().addListener((o2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                            if (!confirmExitWithUnsavedCheck()) e.consume();
                        });
                    }
                });
            }
        });
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

    // Toolbar controls
    @FXML
    private TextField fontSizeField;
    @FXML
    private Button fontSizeDownButton;
    @FXML
    private Button fontSizeUpButton;
    @FXML
    private ColorPicker textColorPicker;
    @FXML
    private ColorPicker bgColorPicker;
    @FXML
    private CheckBox bgTransparentCheckBox;

    // Call this from your initialize() after session/projector are ready
    private void initStyleToolbar() {
        if (session == null) return;

        SublySettings s = session.getSettings();

        // Colors initial values
        if (textColorPicker != null) {
            textColorPicker.setValue(ColorConvertUtils.toJavaFxColor(s.getSubtitleColor()));
        }
        if (bgColorPicker != null) {
            Color bg = ColorConvertUtils.toJavaFxColor(s.getProjectorColor());
            if (s.isProjectorTransparent()) {
                bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0.0);
            }
            bgColorPicker.setValue(bg);
        }

        // Transparent checkbox: disable picker when checked and apply immediately
        if (bgTransparentCheckBox != null) {
            bgTransparentCheckBox.setSelected(s.isProjectorTransparent());
            if (bgColorPicker != null) {
                bgColorPicker.disableProperty().bind(bgTransparentCheckBox.selectedProperty());
            }
            bgTransparentCheckBox.selectedProperty().addListener((obs, was, nowSel) -> {
                updateSettingsAndApply(
                        currentFontSize(),
                        textColorPicker != null ? textColorPicker.getValue() : null,
                        bgColorPicker != null ? bgColorPicker.getValue() : null
                );
            });
        }

        // Font size control: restrict to integers and commit on Enter/focus-out
        int fs = clamp(s.getSubtitleFontSize());
        if (fontSizeField != null) {
            fontSizeField.setText(String.valueOf(fs));
            UnaryOperator<TextFormatter.Change> intFilter = change -> {
                String newText = change.getControlNewText();
                return newText.matches("\\d{0,3}") ? change : null;
            };
            TextFormatter<Integer> formatter =
                    new TextFormatter<>(new IntegerStringConverter(), fs, intFilter);
            fontSizeField.setTextFormatter(formatter);

            fontSizeField.setOnAction(e -> commitFontSizeFromField());
            fontSizeField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) commitFontSizeFromField();
            });
        }

        if (fontSizeDownButton != null) fontSizeDownButton.setOnAction(e -> adjustFontSizeBy(-1));
        if (fontSizeUpButton != null) fontSizeUpButton.setOnAction(e -> adjustFontSizeBy(1));

        // Color pickers listeners
        if (textColorPicker != null) {
            textColorPicker.valueProperty().addListener((obs, ov, nv) -> {
                updateSettingsAndApply(
                        currentFontSize(),
                        nv,
                        bgColorPicker != null ? bgColorPicker.getValue() : null
                );
            });
        }
        if (bgColorPicker != null) {
            bgColorPicker.valueProperty().addListener((obs, ov, nv) -> {
                updateSettingsAndApply(
                        currentFontSize(),
                        textColorPicker != null ? textColorPicker.getValue() : null,
                        nv
                );
            });
        }
    }

    private void commitFontSizeFromField() {
        int parsed = currentFontSize();
        updateSettingsAndApply(parsed,
                textColorPicker != null ? textColorPicker.getValue() : null,
                bgColorPicker != null ? bgColorPicker.getValue() : null);
        // Normalize field text to clamped value
        if (fontSizeField != null) {
            fontSizeField.setText(String.valueOf(parsed));
        }
    }

    private void adjustFontSizeBy(int delta) {
        int cur = currentFontSize();
        int next = clamp(cur + delta);
        if (fontSizeField != null) {
            fontSizeField.setText(String.valueOf(next));
        }
        updateSettingsAndApply(next,
                textColorPicker != null ? textColorPicker.getValue() : null,
                bgColorPicker != null ? bgColorPicker.getValue() : null);
    }

    private int currentFontSize() {
        int fallback = MIN_FONT_SIZE;
        if (fontSizeField == null) return fallback;
        String txt = fontSizeField.getText();
        try {
            int v = Integer.parseInt(txt == null || txt.isBlank() ? String.valueOf(fallback) : txt.trim());
            return clamp(v);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private int clamp(int v) {
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, v));
    }

    private void updateSettingsAndApply(int fontSize, Color textColor, Color bgColor) {
        if (session == null) return;

        session.update(s -> {
            s.setSubtitleFontSize(fontSize);
            if (textColor != null) {
                s.setSubtitleColor(ColorConvertUtils.toHexString(textColor));
            }
            // Use the checkbox to decide transparency, not color opacity
            boolean transparent = bgTransparentCheckBox != null && bgTransparentCheckBox.isSelected();
            s.setProjectorTransparent(transparent);
            if (!transparent && bgColor != null) {
                s.setProjectorColor(ColorConvertUtils.toHexString(bgColor));
            }
        });

        if (projector != null) {
            projector.applySettings(session.getSettings());
        }
        markDirty();
    }
}
