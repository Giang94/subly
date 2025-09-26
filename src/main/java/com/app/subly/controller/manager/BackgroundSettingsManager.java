package com.app.subly.controller.manager;

import com.app.subly.model.enums.BackgroundType;
import com.app.subly.model.SublySettings;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Supplier;

public class BackgroundSettingsManager {

    private final RadioButton bgTransparentRadio;
    private final RadioButton bgColorRadio;
    private final RadioButton bgImageRadio;
    private ToggleGroup bgToggleGroup;
    private final ColorPicker bgColorPicker;
    private final Button chooseImageButton;
    private final TextField imagePathField;

    // NEW: optional text color picker
    private ColorPicker textColorPicker;
    private boolean internalTextColorUpdate = false;

    private final Supplier<SublyProjectSession> sessionSupplier;
    private final Supplier<com.app.subly.component.Projector> projectorSupplier;
    private final Runnable markDirty;
    private final Runnable onPreviewBackgroundChanged;
    private final Runnable onPreviewTextChanged;

    public BackgroundSettingsManager(RadioButton bgTransparentRadio,
                                     RadioButton bgColorRadio,
                                     RadioButton bgImageRadio,
                                     ToggleGroup bgToggleGroup,
                                     ColorPicker bgColorPicker,
                                     Button chooseImageButton,
                                     TextField imagePathField,
                                     Supplier<SublyProjectSession> sessionSupplier,
                                     Supplier<com.app.subly.component.Projector> projectorSupplier,
                                     Runnable markDirty,
                                     Runnable onPreviewBackgroundChanged,
                                     Runnable onPreviewTextChanged) {
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
        this.onPreviewTextChanged = onPreviewTextChanged;
    }

    public void initialize() {
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
        bgToggleGroup.selectedToggleProperty().addListener((o, ov, nv) -> applyBackground());
        bgColorPicker.valueProperty().addListener((o, ov, nv) -> {
            if (bgColorRadio.isSelected()) {
                applyBackground();
            }
        });
    }

    public void setTextColorPicker(ColorPicker picker) {
        this.textColorPicker = picker;
        if (picker == null) return;
        picker.valueProperty().addListener((obs, oldC, newC) -> {
            if (internalTextColorUpdate) return;
            if (newC == null) return;
            SublyProjectSession session = sessionSupplier.get();
            if (session == null) return;
            String hex = ColorConvertUtils.toHexString(newC);
            session.update(s -> s.setSubtitleColor(hex));
            var proj = projectorSupplier.get();
            if (proj != null) {
                proj.applySettings(session.getSettings());
            }
            markDirty.run();
            if (onPreviewTextChanged != null) onPreviewTextChanged.run();
        });
    }

    public void onSessionSet(SublyProjectSession s) {
        System.out.println("onSessionSet called, session (BackgroundSettingsManager): " + s.hashCode());
        System.out.println("imagePathField text (onSessionSet init): " + imagePathField.getText());
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
                System.out.println("imagePathField (onSessionSet): " + imagePathField.hashCode() + ", uri=" + settings.getProjectorImageUri());
                if (settings.getProjectorImageUri() != null) {
                    imagePathField.setText(settings.getProjectorImageUri());
                }
            }
        }
        // NEW: apply saved text color into picker without firing listener
        if (textColorPicker != null && settings.getSubtitleColor() != null) {
            try {
                internalTextColorUpdate = true;
                textColorPicker.setValue(ColorConvertUtils.toJavaFxColor(settings.getSubtitleColor()));
            } finally {
                internalTextColorUpdate = false;
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

    public void applyBackground() {
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
        if (proj != null) {
            if (bgImageRadio.isSelected()) {
                System.out.println("imagePathField (on applyBackground): " + imagePathField.hashCode() + ", uri=" + imagePathField.getText());
                String uri = imagePathField.getText();
                boolean ok = proj.setBackgroundImage(uri, this.getClass().getSimpleName());
                if (!ok) {
                    imagePathField.clear();
                    session.update(s -> s.setProjectorImageUri(null));
                }
            } else {
                proj.applySettings(session.getSettings());
            }
        }

        markDirty.run();
        firePreviewUpdate();
    }

    private void firePreviewUpdate() {
        if (onPreviewBackgroundChanged != null) onPreviewBackgroundChanged.run();
        if (onPreviewTextChanged != null) onPreviewTextChanged.run();
    }
}