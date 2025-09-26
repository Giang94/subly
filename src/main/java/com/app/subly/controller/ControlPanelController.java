package com.app.subly.controller;

import com.app.subly.SublyApplication;
import com.app.subly.component.Projector;
import com.app.subly.component.ProjectorRef;
import com.app.subly.component.StyleToolbarBinder;
import com.app.subly.controller.manager.*;
import com.app.subly.model.Chapter;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import com.app.subly.model.enums.BackgroundType;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import static com.app.subly.persistence.AppSettingsIO.DEFAULT_SUBTITLE_COLOR;

public class ControlPanelController {

    private static final int MIN_FONT_SIZE = 24;
    private static final int MAX_FONT_SIZE = 240;

    // App/session
    private SublyApplication app;
    private SublyProjectSession session;

    // Keep a shared projector ref across parts
    private final ProjectorRef projectorRef = new ProjectorRef();

    // Managers
    private SubtitleTableManager subtitleManager;
    private ChapterManager chapterManager;
    private BackgroundSettingsManager backgroundManager;
    private ProjectFileManager projectFileManager;
    private SubtitlePreviewManager previewManager;
    private PresentingModeManager presentingModeManager;
    private ShowScreenToggleManager showScreenToggleManager;
    private EditingControlLockManager editingControlLockManager;

    // Style toolbar
    private StyleToolbarBinder styleBinder;

    // FXML references
    @FXML
    private ToggleButton toggleShowScreenButton;
    @FXML
    private StackPane currentSubtitlePane;
    @FXML
    private TextFlow currentSubtitleTextFlow;
    @FXML
    private Text currentSubtitleText;
    @FXML
    private ImageView currentSubtitleImage;
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
    private ToggleButton presentingModeToggle;

    // Text settings
    @FXML
    private ComboBox<String> fontFamilyCombo;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private ColorPicker textColorPicker;
    @FXML
    private ComboBox<String> fontWeightCombo;
    @FXML
    private ComboBox<String> borderWeightCombo;
    @FXML
    private ColorPicker borderColorPicker;

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

    // Chapters
    @FXML
    private ListView<Chapter> chapterListView;
    @FXML
    private Label chapterCountLabel;

    // Chapter context menu
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

    // Dirty
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        styleBinder = new StyleToolbarBinder(
                MIN_FONT_SIZE,
                MAX_FONT_SIZE,
                fontSizeSpinner,
                textColorPicker,
                fontFamilyCombo,
                fontWeightCombo,
                borderWeightCombo,
                borderColorPicker,
                this::currentFontSize,
                this::applySettingsToProjector,
                this::markDirty
        );

        // Core managers
        subtitleManager = new SubtitleTableManager(
                subtitleTable, indexColumn, primaryTextColumn, secondaryTextColumn,
                currentSubtitleText, prevButton, nextButton, chapterListView,
                this::markDirty,
                projectorRef::get,
                () -> session
        );

        chapterManager = new ChapterManager(
                chapterListView, chapterCountLabel,
                addChapterMenuItem, renameChapterMenuItem,
                deleteChapterMenuItem, moveUpMenuItem, moveDownMenuItem,
                () -> session,
                subtitleManager,
                this::markDirty
        );

        backgroundManager = new BackgroundSettingsManager(
                bgTransparentRadio, bgColorRadio, bgImageRadio, bgToggleGroup,
                bgColorPicker, chooseImageButton, imagePathField,
                () -> session, projectorRef::get, this::markDirty,
                this::onBackgroundChanged, this::onTextChanged
        );

        projectFileManager = new ProjectFileManager(
                newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, exitMenuItem,
                undoMenuItem, redoMenuItem,
                menuBar,
                () -> session,
                () -> app,
                subtitleManager,
                this::setDirty,
                () -> dirty.get(),
                this
        );

        previewManager = new SubtitlePreviewManager(
                currentSubtitlePane,
                currentSubtitleImage,
                currentSubtitleTextFlow,
                currentSubtitleText
        );

        // Extracted controllers
        editingControlLockManager = new EditingControlLockManager(
                fontFamilyCombo, fontSizeSpinner,
                textColorPicker, fontWeightCombo,
                borderWeightCombo, borderColorPicker,
                bgTransparentRadio,
                bgColorRadio, bgColorPicker,
                bgImageRadio, imagePathField, chooseImageButton,
                subtitleTable, chapterManager,
                addChapterMenuItem, renameChapterMenuItem, deleteChapterMenuItem, moveUpMenuItem, moveDownMenuItem,
                undoMenuItem, redoMenuItem
        );

        presentingModeManager = new PresentingModeManager(
                presentingModeToggle, prevButton, nextButton,
                subtitleTable, chapterListView, () -> session, projectorRef,
                enabled -> editingControlLockManager.setEditingEnabled(enabled)

        );

        showScreenToggleManager = new ShowScreenToggleManager(
                toggleShowScreenButton,
                subtitleTable,
                projectorRef,
                backgroundManager,
                this::applyToggleText,
                this::currentFontSize,
                this::sessionTextColor,
                (Integer size, Color color) -> applySettingsToProjector(size, color),
                this::updatePreviewAppearance
        );

        // Initialize modules
        subtitleManager.initialize();
        chapterManager.initialize();
        backgroundManager.initialize();
        projectFileManager.initialize();
        previewManager.initialize();
        presentingModeManager.initialize();
        showScreenToggleManager.initialize();

        if (imagePathField != null) {
            imagePathField.setVisible(false);
            imagePathField.setManaged(false);
        }

        if (textColorPicker != null) {
            textColorPicker.setValue(sessionTextColor());
            backgroundManager.setTextColorPicker(textColorPicker);
        }
        updatePreviewAppearance();
    }

    public void setShowScreen(SublyApplication app, Projector projector) {
        this.app = app;
        this.projectorRef.set(projector);
        styleBinder.rebind(session, projectorRef.get());
        backgroundManager.applyBackground();
        presentingModeManager.updatePresentingToggleState();
    }

    public void setSession(SublyProjectSession session) {
        this.session = session;

        chapterManager.onSessionSet();
        subtitleManager.onSessionSet();
        styleBinder.rebind(session, projectorRef.get());
        backgroundManager.onSessionSet(session);
        projectFileManager.refreshActions();
        presentingModeManager.onSessionSet();

        if (presentingModeToggle == null || !presentingModeToggle.isSelected()) {
            editingControlLockManager.setEditingEnabled(true);
        }
        if (textColorPicker != null) {
            textColorPicker.setValue(sessionTextColor());
        }
        updatePreviewAppearance();
    }

    private void applySettingsToProjector(int size, Color textColor) {
        Projector projector = projectorRef.get();
        if (projector != null && session != null) {
            projector.applyLabelSettings(session.getSettings());
            if (textColor != null) {
                try {
                    projector.getLabel().setTextFill(textColor);
                } catch (Exception ignored) {
                }
            }
        }
        updatePreviewAppearance();
    }

    private void updatePreviewAppearance() {
        if (previewManager == null) return;
        Color textColor = textColorPicker != null ? textColorPicker.getValue() : Color.web(DEFAULT_SUBTITLE_COLOR);
        previewManager.updateAppearance(
                textColor,
                bgTransparentRadio,
                bgColorRadio,
                bgImageRadio,
                bgColorPicker,
                imagePathField,
                borderWeightCombo.getSelectionModel().getSelectedItem(),
                borderColorPicker.getValue(),
                false
        );
    }

    private void applyToggleText() {
        toggleShowScreenButton.setText(toggleShowScreenButton.isSelected() ? "Close Show Screen" : "Open Show Screen");
    }

    private void setDirty(boolean v) {
        dirty.set(v);
        if (app != null) app.setDirty(v);
    }

    private void markDirty() {
        setDirty(true);
    }

    private int currentFontSize() {
        if (fontSizeSpinner == null) return MIN_FONT_SIZE;
        try {
            int v = fontSizeSpinner.getValue() == null ? MIN_FONT_SIZE : fontSizeSpinner.getValue();
            return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, v));
        } catch (NumberFormatException ex) {
            return MIN_FONT_SIZE;
        }
    }

    private Color sessionTextColor() {
        try {
            if (session != null && session.getSettings() != null) {
                String hex = session.getSettings().getSubtitleColor();
                if (hex != null && !hex.isBlank()) {
                    return Color.web(hex);
                }
            }
        } catch (Exception ignored) {
        }
        return Color.web(DEFAULT_SUBTITLE_COLOR);
    }

    public void onBackgroundChanged() {
        updatePreviewAppearance();
    }

    public void onTextChanged() {
        updatePreviewAppearance();
    }

    public void applySettingsToFormattingTools(SublySettings settings) {
        System.out.println("Calling applySettingsToFormattingTools()");
        if (settings == null) return;
        Platform.runLater(() -> {
            // Font family
            if (settings.getSubtitleFontFamily() != null) {
                ensureItemSelected(fontFamilyCombo, settings.getSubtitleFontFamily());
            }
            // Font size
            if (fontSizeSpinner != null && fontSizeSpinner.getValueFactory() != null) {
                fontSizeSpinner.getValueFactory().setValue((int) settings.getSubtitleFontSize());
            }
            // Font weight
            if (settings.getFontWeight() != null) {
                ensureItemSelected(fontWeightCombo, settings.getFontWeight().name());
            }
            // Text color
            Color textColor = ColorConvertUtils.safeColor(settings.getSubtitleColor());
            if (textColor != null) {
                textColorPicker.setValue(textColor);
            }
            // Border weight
            if (settings.getSubtitleBorderWeight() != null) {
                ensureItemSelected(borderWeightCombo, settings.getSubtitleBorderWeight().name());
            }
            // Border color
            Color borderColor = ColorConvertUtils.safeColor(settings.getSubtitleBorderColor());
            if (borderColor != null) {
                borderColorPicker.setValue(borderColor);
            }

            // Background
            BackgroundType bgType = settings.getBackgroundType();
            if (bgType == null) bgType = BackgroundType.TRANSPARENT;

            switch (bgType) {
                case TRANSPARENT -> bgTransparentRadio.setSelected(true);
                case SOLID_COLOR -> {
                    bgColorRadio.setSelected(true);
                    if (settings.getProjectorColor() != null) {
                        bgColorPicker.setValue(ColorConvertUtils.toJavaFxColor(settings.getProjectorColor()));
                    }
                }
                case IMAGE -> {
                    bgImageRadio.setSelected(true);
                    if (settings.getProjectorImageUri() != null) {
                        imagePathField.setText(settings.getProjectorImageUri());
                    }
                }
            }
        });
    }

    private void ensureItemSelected(ComboBox<String> combo, String value) {
        if (value == null) return;
        if (combo.getItems().stream().noneMatch(v -> v.equalsIgnoreCase(value))) {
            combo.getItems().add(value);
        }
        combo.getSelectionModel().select(value);
    }
}