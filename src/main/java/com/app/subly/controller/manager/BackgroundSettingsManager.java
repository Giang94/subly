package com.app.subly.controller.manager;

import com.app.subly.model.BackgroundType;
import com.app.subly.model.SublySettings;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Supplier;

class BackgroundSettingsManager {

    private final RadioButton bgTransparentRadio;
    private final RadioButton bgColorRadio;
    private final RadioButton bgImageRadio;
    private ToggleGroup bgToggleGroup;
    private final ColorPicker bgColorPicker;
    private final Button chooseImageButton;
    private final TextField imagePathField;

    private final Supplier<SublyProjectSession> sessionSupplier;
    private final Supplier<com.app.subly.component.Projector> projectorSupplier;
    private final Runnable markDirty;
    private final Runnable onPreviewBackgroundChanged;

    BackgroundSettingsManager(RadioButton bgTransparentRadio,
                              RadioButton bgColorRadio,
                              RadioButton bgImageRadio,
                              ToggleGroup bgToggleGroup,
                              ColorPicker bgColorPicker,
                              Button chooseImageButton,
                              TextField imagePathField,
                              Supplier<SublyProjectSession> sessionSupplier,
                              Supplier<com.app.subly.component.Projector> projectorSupplier,
                              Runnable markDirty,
                              Runnable onPreviewBackgroundChanged) {
        this.bgTransparentRadio = bgTransparentRadio;
        this.bgColorRadio = bgColorRadio;
        this.bgImageRadio = bgImageRadio;
        this.bgToggleGroup = bgToggleGroup;
        this.bgColorPicker = bgColorPicker;
        this.chooseImageButton = chooseImageButton;
        this.imagePathField = imagePathField;
        this.sessionSupplier = sessionSupplier;
        this.projectorSupplier = projectorSupplier;
        this.markDirty = markDirty;
        this.onPreviewBackgroundChanged = onPreviewBackgroundChanged;

    }

    void initialize() {
        if (bgToggleGroup == null) {
            bgToggleGroup = new ToggleGroup();
            bgTransparentRadio.setToggleGroup(bgToggleGroup);
            bgColorRadio.setToggleGroup(bgToggleGroup);
            bgImageRadio.setToggleGroup(bgToggleGroup);
        }
        bgColorPicker.disableProperty().bind(bgColorRadio.selectedProperty().not());
        chooseImageButton.disableProperty().bind(bgImageRadio.selectedProperty().not());
        imagePathField.disableProperty().bind(bgImageRadio.selectedProperty().not());

        chooseImageButton.setOnAction(this::onChooseImage);
        bgToggleGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            applyBackground();
        });
        bgColorPicker.valueProperty().addListener((o, ov, nv) -> {
            if (bgColorRadio.isSelected()) {
                applyBackground();
            }
        });
    }

    void onSessionSet() {
        SublyProjectSession s = sessionSupplier.get();
        if (s == null) return;
        SublySettings settings = s.getSettings();
        if (settings == null) return;
        switch (settings.getBackgroundType()) {
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
        applyBackground();
    }

    private void onChooseImage(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Background Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            imagePathField.setText(file.toURI().toString());
            applyBackground();
        }
    }

    void applyBackground() {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        session.update(s -> {
            if (bgTransparentRadio.isSelected()) {
                s.setBackgroundType(BackgroundType.TRANSPARENT);
                s.setProjectorImageUri(null);
            } else if (bgColorRadio.isSelected()) {
                s.setBackgroundType(BackgroundType.SOLID_COLOR);
                Color c = bgColorPicker.getValue() != null ? bgColorPicker.getValue() : Color.BLACK;
                s.setProjectorColor(ColorConvertUtils.toHexString(c));
                s.setProjectorImageUri(null);
            } else if (bgImageRadio.isSelected()) {
                s.setBackgroundType(BackgroundType.IMAGE);
                s.setProjectorImageUri(imagePathField.getText());
            }
        });
        var proj = projectorSupplier.get();
        if (proj != null) proj.applySettings(session.getSettings());
        markDirty.run();
        firePreviewUpdate();
    }

    private void firePreviewUpdate() {
        if (onPreviewBackgroundChanged != null) {
            onPreviewBackgroundChanged.run();
        }
    }
}