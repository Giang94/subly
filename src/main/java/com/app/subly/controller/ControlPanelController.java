package com.app.subly.controller;

import com.app.subly.SublyApplication;
import com.app.subly.component.*;
import com.app.subly.model.*;
import com.app.subly.persistence.ProjectBuilders;
import com.app.subly.persistence.SublyProjectIO;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
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

    private static final String PROJECT_EXT = ".subly";
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

    // Handle chapters
    @FXML
    private ListView<Chapter> chapterListView;
    @FXML
    private Label chapterCountLabel;

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
        ObservableList<Subtitle> data = FXCollections.observableArrayList(new Subtitle(1, "This line will be showed on screen", "This line is for translation"));
        subtitleTable.setItems(data);
        trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);

        // History: refresh table + enforce trailing blank after undo/redo; mark dirty on any change
        history = new EditHistory(() -> {
            trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
            RowIndexer.renumber(subtitleTable.getItems());
            subtitleTable.refresh();
        }, this::markDirty);

        // Paste manager with undo
        // Paste manager records into shared history
        pasteManager = new PasteManager(subtitleTable, primaryTextColumn, secondaryTextColumn, indexColumn, () -> new Subtitle(subtitleTable.getItems().size() + 1, "", ""), () -> trailingRowPolicy.ensureTrailingBlankRow(subtitleTable), this::markDirty, history);
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

        setupSubtitleColumns();
    }

    private void setupSubtitleColumns() {
        // Ensure ONLY these three columns remain
        subtitleTable.getColumns().setAll(indexColumn, primaryTextColumn, secondaryTextColumn);
        subtitleTable.setTableMenuButtonVisible(false);
        subtitleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Fixed (or near fixed) width for index column
        indexColumn.setText("#");
        indexColumn.setMinWidth(30);
        indexColumn.setPrefWidth(30);
        indexColumn.setMaxWidth(30);
        indexColumn.setResizable(false);

        // Allow user resizing on text columns
        primaryTextColumn.setResizable(true);
        secondaryTextColumn.setResizable(true);

        // One‑time equal sizing after layout
        subtitleTable.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                Platform.runLater(this::applyInitialEqualSubtitleWidths);
            }
        });
        subtitleTable.widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() > 0) {
                applyInitialEqualSubtitleWidths();
            }
        });
    }

    private void applyInitialEqualSubtitleWidths() {
        double total = subtitleTable.getWidth();
        if (total <= 0) return;

        double indexW = indexColumn.getWidth();
        if (indexW <= 0) indexW = indexColumn.getPrefWidth();

        double remaining = Math.max(0, total - indexW);
        double half = remaining / 2.0;

        // Only set preferred; leave min/max flexible so user can resize
        primaryTextColumn.setPrefWidth(half);
        secondaryTextColumn.setPrefWidth(half);

        // Sensible minimums
        primaryTextColumn.setMinWidth(80);
        secondaryTextColumn.setMinWidth(80);
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
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"), new FileChooser.ExtensionFilter("All Files", "*.*"));

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
                Color c = (bgColorPicker != null && bgColorPicker.getValue() != null) ? bgColorPicker.getValue() : Color.BLACK;
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

    private boolean isPlaceholder(Chapter ch) {
        if (ch == null || chapterListView == null) return false;
        int idx = chapterListView.getItems().indexOf(ch);
        return (ch.getTitle() == null || ch.getTitle().isBlank()) && idx == chapterListView.getItems().size() - 1;
    }

    private void installSubtitleTablePlaceholderPromotion() {
        if (subtitleTable == null) return;

        Runnable promoteIfNeeded = () -> {
            if (session == null) return;
            Chapter ch = session.getSelectedChapter();
            if (ch != null && isPlaceholder(ch)) {
                boolean promoted = session.promotePlaceholderForSubtitleEdit();
                if (promoted) {
                    chapterListView.refresh();
                }
            }
        };

        // First mouse interaction
        subtitleTable.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> promoteIfNeeded.run());

        // Keyboard interaction (typing)
        subtitleTable.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.TAB) return;
            promoteIfNeeded.run();
        });
    }

    public void setSession(SublyProjectSession session) {
        this.session = session;

        if (chapterListView != null) {
            chapterListView.setItems(session.getChapters());
            installChapterInlineRename();
            session.ensureAtLeastOneChapter(); // now also ensures placeholder

            if (chapterListView.getSelectionModel().getSelectedIndex() < 0 && !chapterListView.getItems().isEmpty()) {
                chapterListView.getSelectionModel().select(0);
                session.setSelectedChapterIndex(0);
                Chapter ch = session.getSelectedChapter();
                if (ch != null && !isPlaceholder(ch)) reloadSubtitleTable(ch.getSubtitles());
            }

            chapterListView.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> {
                if (ov != null && ov.intValue() >= 0) {
                    Chapter prev = session.getSelectedChapter();
                    if (prev != null && !isPlaceholder(prev)) {
                        session.syncCurrentChapterFromTable(subtitleTable, trailingRowPolicy);
                    }
                }
                int idx = (nv == null) ? -1 : nv.intValue();
                session.setSelectedChapterIndex(idx);
                Chapter ch = session.getSelectedChapter();
                if (ch != null && !isPlaceholder(ch)) {
                    reloadSubtitleTable(ch.getSubtitles().isEmpty() ? java.util.List.of(new Subtitle(1, "", "")) : ch.getSubtitles());
                } else {
                    // Placeholder selected: show empty table (but preserve current unsynced data)
                    subtitleTable.setItems(FXCollections.observableArrayList());
                    trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
                }
            });
        }

        if (styleBinder == null) {
            styleBinder = new StyleToolbarBinder(MIN_FONT_SIZE, MAX_FONT_SIZE, fontSizeField, fontSizeDownButton, fontSizeUpButton, textColorPicker, this::currentFontSize, this::applySettingsToProjector, this::markDirty);
        }

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

        if (bgToggleGroup != null) {
            bgToggleGroup.selectedToggleProperty().addListener((obs, o, n) -> applyBackgroundChange());
        }
        if (bgColorPicker != null) {
            bgColorPicker.valueProperty().addListener((obs, o, n) -> {
                if (bgColorRadio != null && bgColorRadio.isSelected()) applyBackgroundChange();
            });
        }

        styleBinder.rebind(session, projector);
        applyBackgroundChange();
        installSubtitleTablePlaceholderPromotion();
        // Handle context menu on Chapter list view
        setupChapterContextMenuActions();
        updateChapterContextMenuState();
        chapterListView.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> updateChapterContextMenuState());
        session.getChapters().addListener((ListChangeListener<Chapter>) c -> updateChapterContextMenuState());
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
                        history.push(EditHistory.of(() -> {
                            if (!subtitleTable.getItems().isEmpty())
                                subtitleTable.getItems().remove(subtitleTable.getItems().size() - 1);
                        }, () -> subtitleTable.getItems().add(new Subtitle(subtitleTable.getItems().size() + 1, "", ""))));
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
                            }));
                    history.push(ce);
                }
                e.consume();
            }
        });

        // Cell factories
        primaryTextColumn.setCellFactory(col -> new MultilineTableCell());
        secondaryTextColumn.setCellFactory(col -> new MultilineTableCell());
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
                history.push(EditHistory.of(() -> {
                    if (rowIndex < subtitleTable.getItems().size())
                        subtitleTable.getItems().get(rowIndex).setPrimaryText(oldV);
                }, () -> {
                    if (rowIndex < subtitleTable.getItems().size())
                        subtitleTable.getItems().get(rowIndex).setPrimaryText(newV);
                }));
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
                history.push(EditHistory.of(() -> {
                    if (rowIndex < subtitleTable.getItems().size())
                        subtitleTable.getItems().get(rowIndex).setSecondaryText(oldV);
                }, () -> {
                    if (rowIndex < subtitleTable.getItems().size())
                        subtitleTable.getItems().get(rowIndex).setSecondaryText(newV);
                }));
            }
            trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
            markDirty();
        });

        // Value factories
        indexColumn.setCellValueFactory(cd -> {
            Integer id = cd.getValue().getId();
            if (id == null) {
                // Fallback: compute position (1-based). Avoid -1 if not found.
                int pos = subtitleTable.getItems().indexOf(cd.getValue());
                int safe = (pos >= 0 ? pos + 1 : 0);
                return new javafx.beans.property.SimpleIntegerProperty(safe).asObject();
            }
            return new javafx.beans.property.SimpleIntegerProperty(id).asObject();
        });
        primaryTextColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrimaryText()));
        secondaryTextColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getSecondaryText()));
    }

    private void installChapterInlineRename() {
        chapterListView.setEditable(true);
        PseudoClass placeholderPc = PseudoClass.getPseudoClass("placeholder-chapter");

        chapterListView.setCellFactory(lv -> new ListCell<>() {
            private TextField editor;
            private String oldTitle;

            {
                // Single click on placeholder starts edit; double-click continues to work for normal rows
                addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                    Chapter it = getItem();
                    if (e.getClickCount() == 1 && it != null && isPlaceholder(it) && !isEditing()) {
                        startEdit();
                        e.consume();
                    } else if (e.getClickCount() == 2 && it != null && !isEditing()) {
                        startEdit();
                        e.consume();
                    }
                });
            }

            @Override
            protected void updateItem(Chapter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    pseudoClassStateChanged(placeholderPc, false);
                    return;
                }
                boolean placeholder = isPlaceholder(item);
                pseudoClassStateChanged(placeholderPc, placeholder);

                if (isEditing()) {
                    setText(null);
                    setGraphic(editor);
                    return;
                }

                if (placeholder) {
                    setText(null);
                    setGraphic(buildPlaceholderGraphic());
                } else {
                    setText(item.getTitle());
                    setGraphic(null);
                }
            }

            private javafx.scene.Node buildPlaceholderGraphic() {
                Label plus = new Label("+");
                plus.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent; -fx-font-size: 14;");
                Label hint = new Label(" Add chapter");
                hint.setStyle("-fx-text-fill: -fx-text-inner-color; -fx-opacity: 0.65; -fx-font-style: italic;");
                HBox box = new HBox(plus, hint);
                box.setSpacing(2);
                box.setPadding(new Insets(2, 4, 2, 4));
                Tooltip.install(box, new Tooltip("Click to create a new chapter"));
                return box;
            }

            @Override
            public void startEdit() {
                if (getItem() == null) return;
                super.startEdit();
                if (editor == null) createEditor();
                oldTitle = getItem().getTitle();
                editor.setText(oldTitle == null ? "" : oldTitle);
                setText(null);
                setGraphic(editor);
                editor.requestFocus();
                editor.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                Chapter it = getItem();
                if (it == null) {
                    setText(null);
                    setGraphic(null);
                } else if (isPlaceholder(it)) {
                    setText(null);
                    setGraphic(buildPlaceholderGraphic());
                } else {
                    setText(it.getTitle());
                    setGraphic(null);
                }
            }

            @Override
            public void commitEdit(Chapter newValue) {
                super.commitEdit(newValue);
                if (isPlaceholder(newValue)) {
                    setText(null);
                    setGraphic(buildPlaceholderGraphic());
                } else {
                    setText(newValue.getTitle());
                    setGraphic(null);
                }
            }

            private void createEditor() {
                editor = new TextField();
                editor.setOnAction(e -> commitOrReclassify());
                editor.focusedProperty().addListener((o, was, now) -> {
                    if (!now) commitOrReclassify();
                });
                editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        e.consume();
                    }
                });
            }

            private void commitOrReclassify() {
                Chapter ch = getItem();
                if (ch == null) return;
                String txt = editor.getText() == null ? "" : editor.getText().trim();
                boolean wasPlaceholder = isPlaceholder(ch);

                if (txt.isBlank()) {
                    // keep placeholder (or revert if normal)
                    if (!wasPlaceholder) {
                        ch.setTitle(oldTitle);
                    }
                    cancelEdit();
                    return;
                }

                if (!txt.equals(ch.getTitle())) {
                    ch.setTitle(txt);
                    if (session != null) session.touch();
                }

                if (wasPlaceholder) {
                    if (ch.getSubtitles().isEmpty()) {
                        ch.getSubtitles().add(new Subtitle(1, "", ""));
                    }
                    session.ensurePlaceholderChapter();
                }

                commitEdit(ch);
                chapterListView.refresh();
            }
        });
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
        if (!confirmNewWithUnsavedCheck()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Subly Project (*" + PROJECT_EXT + ")", "*" + PROJECT_EXT));

        Stage stage = new SublyApplicationStage();
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;

        try {
            var project = SublyProjectIO.load(selected.toPath());
            if (session != null) {
                session.setProjectFile(selected);
                if (project.getSettings() != null) {
                    app.updateSetting(project.getSettings());
                    session.setSettings(project.getSettings());
                }
                session.replaceAllChapters(project.getChapters());
            }

            if (!session.getChapters().isEmpty()) {
                session.ensureAllChapterIds();
                chapterListView.getSelectionModel().select(0);
                reloadSubtitleTable(session.getChapters().get(0).getSubtitles());
            } else {
                reloadSubtitleTable(java.util.List.of(new Subtitle(1, "", "")));
            }

            app.updateTitle(project.getFileName());
            session.clearDirty();
            setDirty(false);
            refreshActions();
        } catch (IOException ex) {
            showIoError("Open Project Failed", ex);
        }
    }

    private void saveProject() {
        if (session == null) return;
        session.ensureAllChapterIds();
        session.syncCurrentChapterFromTable(subtitleTable, trailingRowPolicy);
        File target = session.getProjectFile();
        if (target == null) {
            saveProjectAs();
            return;
        }
        SublyProjectFile project = ProjectBuilders.fromUi(target.getName(), chapterListView, subtitleTable, session);
        try {
            SublyProjectIO.save(project, target.toPath());
            app.updateTitle(project.getFileName());
            session.clearDirty();
            setDirty(false);
            refreshActions();
        } catch (IOException ex) {
            showIoError("Save Project Failed", ex);
        }
    }

    private void saveProjectAs() {
        if (session == null) return;
        session.syncCurrentChapterFromTable(subtitleTable, trailingRowPolicy);
        session.ensureAllChapterIds();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Subly Project (*" + PROJECT_EXT + ")", "*" + PROJECT_EXT));

        File initial = session.getProjectFile();
        if (initial != null && initial.getParentFile() != null) {
            chooser.setInitialDirectory(initial.getParentFile());
            chooser.setInitialFileName(stripExt(initial.getName()) + PROJECT_EXT);
        } else {
            chooser.setInitialFileName("Untitled" + PROJECT_EXT);
        }

        Stage stage = new SublyApplicationStage();
        File chosen = chooser.showSaveDialog(stage);
        if (chosen == null) return;

        if (!chosen.getName().toLowerCase().endsWith(PROJECT_EXT)) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + PROJECT_EXT);
        }

        session.setProjectFile(chosen);
        session.syncCurrentChapterFromTable(subtitleTable, trailingRowPolicy);

        SublyProjectFile project = ProjectBuilders.fromUi(chosen.getName(), chapterListView, subtitleTable, session);
        try {
            SublyProjectIO.save(project, chosen.toPath());
            app.updateTitle(project.getFileName());
            session.clearDirty();
            setDirty(false);
            refreshActions();
        } catch (IOException ex) {
            showIoError("Save Project Failed", ex);
        }
    }

    private void requestExit() {
        if (confirmExitWithUnsavedCheck()) {
            Platform.exit();
        }
    }

    private String stripExt(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : name;
    }

    // Simple error dialog (optional)
    private void showIoError(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(ex.getMessage());
        a.showAndWait();
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
        File target = (session != null) ? session.getProjectFile() : null;
        if (target == null) {
            saveProjectAs();
        } else {
            saveProject();
        }
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
                    style = String.format("-fx-background-image: url(\"%s\"); " + "-fx-background-size: cover; " + "-fx-background-position: center center; " + "-fx-background-repeat: no-repeat;", cssUrl);
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

    // Chapter context menu and actions
    @FXML
    private MenuItem addChapterMenuItem;
    @FXML
    private MenuItem renameChapterMenuItem;
    @FXML
    private MenuItem deleteChapterMenuItem;
    @FXML
    private MenuItem moveUpMenuItem;
    @FXML
    private MenuItem moveDownMenuItem;

    private void setupChapterContextMenuActions() {
        if (addChapterMenuItem != null) {
            addChapterMenuItem.setOnAction(e -> onAddChapter());
        }
        if (renameChapterMenuItem != null) {
            renameChapterMenuItem.setOnAction(e -> onRenameChapter());
        }
        if (deleteChapterMenuItem != null) {
            deleteChapterMenuItem.setOnAction(e -> onDeleteChapter());
        }
        if (moveUpMenuItem != null) {
            moveUpMenuItem.setOnAction(e -> onMoveChapterUp());
        }
        if (moveDownMenuItem != null) {
            moveDownMenuItem.setOnAction(e -> onMoveChapterDown());
        }
    }

    private void updateChapterContextMenuState() {
        if (chapterListView == null || session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        Chapter ch = session.getSelectedChapter();
        boolean hasSelection = idx >= 0 && ch != null;
        boolean placeholder = hasSelection && isPlaceholder(ch);

        if (addChapterMenuItem != null) addChapterMenuItem.setDisable(false);
        if (renameChapterMenuItem != null) renameChapterMenuItem.setDisable(!hasSelection);
        if (deleteChapterMenuItem != null) {
            // Allow delete unless placeholder
            deleteChapterMenuItem.setDisable(!hasSelection || placeholder);
        }
        if (moveUpMenuItem != null) {
            moveUpMenuItem.setDisable(!hasSelection || placeholder || idx <= 0);
        }
        if (moveDownMenuItem != null) {
            boolean disable = true;
            if (hasSelection && !placeholder) {
                if (idx < session.getChapters().size() - 1) {
                    Chapter next = session.getChapters().get(idx + 1);
                    disable = isPlaceholder(next);
                }
            }
            moveDownMenuItem.setDisable(disable);
        }
    }

    private void onAddChapter() {
        if (session == null) return;
        Chapter c = session.addChapter();
        chapterListView.getSelectionModel().select(c);
        chapterListView.scrollTo(c);
        chapterListView.refresh();
        updateChapterContextMenuState();
    }

    private void onRenameChapter() {
        if (chapterListView == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        chapterListView.edit(idx);
    }

    private void onDeleteChapter() {
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Chapter ch = session.getSelectedChapter();
        if (ch == null || isPlaceholder(ch)) return;

        // Sync edits of current chapter before deletion
        session.syncCurrentChapterFromTable(subtitleTable, trailingRowPolicy);

        // Remove selected chapter
        session.getChapters().remove(idx);

        // If now no real chapters remain, create a fresh one
        long realCount = session.getChapters().stream().filter(c -> !isPlaceholder(c)).count();
        if (realCount == 0) {
            Chapter fresh = new Chapter();
            fresh.setTitle("Chapter 1");
            fresh.getSubtitles().add(new Subtitle(1, "", ""));
            session.getChapters().add(0, fresh);
            idx = 0;
        }

        // Ensure placeholder chapter exists at end
        session.ensurePlaceholderChapter();

        // Adjust selection
        int newIndex = Math.min(idx, session.getChapters().size() - 1);
        if (newIndex >= 0) {
            chapterListView.getSelectionModel().select(newIndex);
            session.setSelectedChapterIndex(newIndex);
        } else {
            session.setSelectedChapterIndex(-1);
        }

        // Reload table for newly selected chapter (if real)
        Chapter newCh = session.getSelectedChapter();
        if (newCh != null && !isPlaceholder(newCh)) {
            if (newCh.getSubtitles().isEmpty()) {
                newCh.getSubtitles().add(new Subtitle(1, "", ""));
            }
            reloadSubtitleTable(newCh.getSubtitles());
        } else {
            subtitleTable.setItems(FXCollections.observableArrayList(new Subtitle(1, "", "")));
            trailingRowPolicy.ensureTrailingBlankRow(subtitleTable);
        }

        chapterListView.refresh();
        updateChapterContextMenuState();
        session.touch();
        markDirty();
    }

    private void onMoveChapterUp() {
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx <= 0) return;
        Chapter ch = session.getSelectedChapter();
        if (ch == null || isPlaceholder(ch)) return;

        ObservableList<Chapter> list = session.getChapters();
        list.remove(idx);
        list.add(idx - 1, ch);
        chapterListView.getSelectionModel().select(idx - 1);
        session.setSelectedChapterIndex(idx - 1);
        session.touch();
        chapterListView.refresh();
        updateChapterContextMenuState();
    }

    private void onMoveChapterDown() {
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Chapter ch = session.getSelectedChapter();
        if (ch == null || isPlaceholder(ch)) return;

        ObservableList<Chapter> list = session.getChapters();
        if (idx >= list.size() - 1) return;
        Chapter next = list.get(idx + 1);
        if (isPlaceholder(next)) return; // cannot move below placeholder

        list.remove(idx);
        list.add(idx + 1, ch);
        chapterListView.getSelectionModel().select(idx + 1);
        session.setSelectedChapterIndex(idx + 1);
        session.touch();
        chapterListView.refresh();
        updateChapterContextMenuState();
    }
}