package com.app.subly.controller.migrate;

import com.app.subly.SublyApplication;
import com.app.subly.component.Projector;
import com.app.subly.component.StyleToolbarBinder;
import com.app.subly.model.Chapter;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ControlPanelController {

    private static final int MIN_FONT_SIZE = 24;
    private static final int MAX_FONT_SIZE = 240;
    private static final double ASPECT_W = 16.0;
    private static final double ASPECT_H = 9.0;

    // App/session
    private SublyApplication app;
    private SublyProjectSession session;
    private Projector projector;

    // Managers
    private SubtitleTableManager subtitleManager;
    private ChapterManager chapterManager;
    private BackgroundSettingsManager backgroundManager;
    private ProjectFileManager projectFileManager;
    private SubtitlePreviewManager previewManager;

    // Style toolbar
    private StyleToolbarBinder styleBinder;

    // FXML references
    @FXML
    private ToggleButton toggleShowScreenButton;
    @FXML
    private StackPane currentSubtitlePane;
    @FXML
    private Label currentSubtitleLabel;
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
        configureCurrentSubtitleLabel();
        styleBinder = new StyleToolbarBinder(
                MIN_FONT_SIZE, MAX_FONT_SIZE,
                fontSizeField, fontSizeDownButton, fontSizeUpButton,
                textColorPicker, this::currentFontSize, this::applySettingsToProjector, this::markDirty
        );

        // Instantiate managers
        subtitleManager = new SubtitleTableManager(
                subtitleTable, indexColumn, primaryTextColumn, secondaryTextColumn,
                currentSubtitleLabel, prevButton, nextButton,
                this::markDirty,
                () -> projector,
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
                () -> session, () -> projector, this::markDirty,
                this::onBackgroundChanged
        );

        projectFileManager = new ProjectFileManager(
                newMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, exitMenuItem,
                undoMenuItem, redoMenuItem,
                menuBar,
                () -> session,
                () -> app,
                subtitleManager,
                this::setDirty,
                () -> dirty.get()
        );

        previewManager = new SubtitlePreviewManager(currentSubtitlePane, currentSubtitleImage, currentSubtitleLabel);

        subtitleManager.initialize();
        chapterManager.initialize();
        backgroundManager.initialize();
        projectFileManager.initialize();
        previewManager.initialize();

        if (imagePathField != null) {
            imagePathField.setVisible(false);
            imagePathField.setManaged(false);
        }

        setupShowScreenToggle();
        updatePreviewAppearance();
    }

    private void setupShowScreenToggle() {
        applyToggleText();
        toggleShowScreenButton.setOnAction(e -> {
            if (toggleShowScreenButton.isSelected()) {
                if (projector == null) {
                    projector = new Projector();
                }
                projector.show();
                Subtitle sel = subtitleTable.getSelectionModel().getSelectedItem();
                if (sel != null && sel.getPrimaryText() != null) {
                    projector.setText(sel.getPrimaryText());
                }
                backgroundManager.applyBackground();
                applySettingsToProjector(currentFontSize(), textColorPicker.getValue());
                updatePreviewAppearance();
                applyToggleText();
            } else {
                if (projector != null) projector.hide();
                applyToggleText();
            }
        });
    }

    private void applyToggleText() {
        toggleShowScreenButton.setText(toggleShowScreenButton.isSelected() ? "Close Show Screen" : "Open Show Screen");
    }

    public void setShowScreen(SublyApplication app, Projector projector) {
        this.app = app;
        this.projector = projector;
        styleBinder.rebind(session, projector);
        backgroundManager.applyBackground();
    }

    public void setSession(SublyProjectSession session) {
        this.session = session;
        chapterManager.onSessionSet();
        subtitleManager.onSessionSet();
        styleBinder.rebind(session, projector);
        backgroundManager.onSessionSet();
        projectFileManager.refreshActions();
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

            // Use a wide wrapping width to measure single-line height reliably
            measurer.setWrappingWidth(10_000);
            double lineHeight = Math.ceil(measurer.getLayoutBounds().getHeight() / 4.0); // 4 lines in template text
            if (lineHeight <= 0) lineHeight = 20;

            double threeLinesContent = lineHeight * 3;
            double totalHeight = threeLinesContent + top + bottom + 1;

            // Compute width from aspect ratio
            double width = (totalHeight * ASPECT_W) / ASPECT_H;

            currentSubtitleLabel.setMinHeight(totalHeight);
            currentSubtitleLabel.setPrefHeight(totalHeight);
            currentSubtitleLabel.setMaxHeight(totalHeight);

            currentSubtitleLabel.setMinWidth(width);
            currentSubtitleLabel.setPrefWidth(width);
            currentSubtitleLabel.setMaxWidth(width);

            clip.setWidth(width);
            clip.setHeight(totalHeight);
        };

        // Recalculate when font or scene added
        currentSubtitleLabel.fontProperty().addListener((o, ov, nv) -> applyHeights.run());
        currentSubtitleLabel.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) Platform.runLater(applyHeights);
        });
        // Initial
        Platform.runLater(applyHeights);
    }

    private void applySettingsToProjector(int size, Color textColor) {
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
        Color txt = textColorPicker != null ? textColorPicker.getValue() : null;
        previewManager.updateAppearance(
                txt,
                bgTransparentRadio,
                bgColorRadio,
                bgImageRadio,
                bgColorPicker,
                imagePathField
        );
    }

    private void setDirty(boolean v) {
        dirty.set(v);
        if (app != null) app.setDirty(v);
    }

    private void markDirty() {
        setDirty(true);
    }

    private int currentFontSize() {
        if (fontSizeField == null) return MIN_FONT_SIZE;
        try {
            int v = Integer.parseInt(fontSizeField.getText() == null ? String.valueOf(MIN_FONT_SIZE) : fontSizeField.getText().trim());
            return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, v));
        } catch (NumberFormatException ex) {
            return MIN_FONT_SIZE;
        }
    }

    public void onBackgroundChanged() {
        updatePreviewAppearance();
    }
}