package com.app.subly.component;

import com.app.subly.model.SublySettings;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.converter.IntegerStringConverter;
import javafx.scene.control.TextFormatter;

import java.util.function.Supplier;

public class StyleToolbarBinder {

    private final int minFont;
    private final int maxFont;

    private final TextField fontSizeField;
    private final Button fontDown;
    private final Button fontUp;
    private final ColorPicker textColorPicker;

    private final Supplier<Integer> currentFontSize;
    private final ApplyProjector applyProjector;
    private final Runnable markDirty;

    private SublyProjectSession session;
    private Projector projector;

    public StyleToolbarBinder(int minFont, int maxFont,
                              TextField fontSizeField, Button fontDown, Button fontUp,
                              ColorPicker textColorPicker,
                              Supplier<Integer> currentFontSize,
                              ApplyProjector applyProjector,
                              Runnable markDirty) {
        this.minFont = minFont;
        this.maxFont = maxFont;
        this.fontSizeField = fontSizeField;
        this.fontDown = fontDown;
        this.fontUp = fontUp;
        this.textColorPicker = textColorPicker;
        this.currentFontSize = currentFontSize;
        this.applyProjector = applyProjector;
        this.markDirty = markDirty;

        initControls();
    }

    public void rebind(SublyProjectSession session, Projector projector) {
        this.session = session;
        this.projector = projector;
        if (session == null) return;

        SublySettings s = session.getSettings();

        if (textColorPicker != null) {
            textColorPicker.setValue(ColorConvertUtils.toJavaFxColor(s.getSubtitleColor()));
        }
        if (fontSizeField != null) {
            int fs = clamp(s.getSubtitleFontSize());
            fontSizeField.setText(String.valueOf(fs));
        }

        applyCurrentToProjector();
    }

    private void initControls() {
        if (fontSizeField != null) {
            TextFormatter<Integer> formatter = new TextFormatter<>(
                    new IntegerStringConverter(),
                    clamp((currentFontSize != null ? currentFontSize.get() : minFont)),
                    change -> change.getControlNewText().matches("\\d{0,3}") ? change : null
            );
            fontSizeField.setTextFormatter(formatter);
            fontSizeField.setOnAction(e -> commitFontSize());
            fontSizeField.focusedProperty().addListener((obs, was, now) -> {
                if (!now) commitFontSize();
            });
        }
        if (fontDown != null) fontDown.setOnAction(e -> adjustFont(-1));
        if (fontUp != null) fontUp.setOnAction(e -> adjustFont(1));

        if (textColorPicker != null) {
            textColorPicker.valueProperty().addListener((obs, ov, nv) -> onAnyStyleChanged());
        }
    }

    private void onAnyStyleChanged() {
        if (session == null) return;
        int size = currentFontSize != null ? clamp(currentFontSize.get()) : minFont;
        Color text = textColorPicker != null ? textColorPicker.getValue() : null;

        session.update(s -> {
            s.setSubtitleFontSize(size);
            if (text != null) s.setSubtitleColor(ColorConvertUtils.toHexString(text));
        });

        applyCurrentToProjector();
        if (markDirty != null) markDirty.run();
    }

    private void commitFontSize() {
        onAnyStyleChanged();
        if (fontSizeField != null && currentFontSize != null) {
            fontSizeField.setText(String.valueOf(clamp(currentFontSize.get())));
        }
    }

    private void adjustFont(int delta) {
        if (fontSizeField == null || currentFontSize == null) return;
        int next = clamp(currentFontSize.get() + delta);
        fontSizeField.setText(String.valueOf(next));
        onAnyStyleChanged();
    }

    private void applyCurrentToProjector() {
        if (session == null) return;
        int size = clamp(session.getSettings().getSubtitleFontSize());
        Color text = textColorPicker != null ? textColorPicker.getValue() : null;
        if (applyProjector != null) applyProjector.apply(size, text);
    }

    private int clamp(int v) {
        return Math.clamp(v, minFont, maxFont);
    }

    @FunctionalInterface
    public interface ApplyProjector {
        void apply(int size, Color textColor);
    }
}