package com.app.subly.component;

import com.app.subly.model.SublySettings;
import com.app.subly.model.enums.BorderWeight;
import com.app.subly.model.enums.FontWeight;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.collections.FXCollections;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

public class StyleToolbarBinder {

    private final Spinner<Integer> fontSizeSpinner;
    private final ColorPicker textColorPicker;
    private final ComboBox<String> fontFamilyCombo;
    private final ComboBox<String> fontWeightCombo;
    private final ComboBox<String> borderWeightCombo;
    private final ColorPicker borderColorPicker;

    private final int minFont;
    private final int maxFont;
    private final IntSupplier currentFontSizeSupplier;
    private final BiConsumer<Integer, Color> applySettingsToProjector;
    private final Runnable markDirty;

    private SublyProjectSession session;
    private Projector projector;

    public StyleToolbarBinder(int minFont,
                              int maxFont,
                              Spinner<Integer> fontSizeSpinner,
                              ColorPicker textColorPicker,
                              ComboBox<String> fontFamilyCombo,
                              ComboBox<String> fontWeightCombo,
                              ComboBox<String> borderWeightCombo,
                              ColorPicker borderColorPicker,
                              IntSupplier currentFontSizeSupplier,
                              BiConsumer<Integer, Color> applySettingsToProjector,
                              Runnable markDirty) {
        this.minFont = minFont;
        this.maxFont = maxFont;
        this.fontSizeSpinner = fontSizeSpinner;
        this.textColorPicker = textColorPicker;
        this.fontFamilyCombo = fontFamilyCombo;
        this.fontWeightCombo = fontWeightCombo;
        this.borderWeightCombo = borderWeightCombo;
        this.borderColorPicker = borderColorPicker;
        this.currentFontSizeSupplier = currentFontSizeSupplier;
        this.applySettingsToProjector = applySettingsToProjector;
        this.markDirty = markDirty;
        initFontFamilies();
        initFontSizeSpinner();
        installListeners();
    }

    private void initFontFamilies() {
        if (fontFamilyCombo != null) {
            List<String> families = javafx.scene.text.Font.getFamilies();
            fontFamilyCombo.setItems(FXCollections.observableArrayList(families));
        }
    }

    private void initFontSizeSpinner() {
        if (fontSizeSpinner != null) {
            fontSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    minFont,
                    maxFont,
                    Math.max(minFont, Math.min(maxFont, currentFontSizeSupplier.getAsInt())),
                    1
            ));
            fontSizeSpinner.getEditor().textProperty().addListener((o, ov, nv) -> {
                // Defer parse until focus lost or Enter; lightweight guard
            });
            fontSizeSpinner.getEditor().setOnAction(e -> commitFontSizeEditor());
            fontSizeSpinner.focusedProperty().addListener((o, was, isNow) -> {
                if (!isNow) commitFontSizeEditor();
            });
        }
    }

    private void commitFontSizeEditor() {
        if (fontSizeSpinner == null) return;
        String txt = fontSizeSpinner.getEditor().getText();
        try {
            int v = Integer.parseInt(txt.trim());
            v = Math.max(minFont, Math.min(maxFont, v));
            fontSizeSpinner.getValueFactory().setValue(v);
            applyFontSize(v);
        } catch (NumberFormatException ignored) {
            // revert
            fontSizeSpinner.getEditor().setText(String.valueOf(fontSizeSpinner.getValue()));
        }
    }

    public void rebind(SublyProjectSession session, Projector projector) {
        this.session = session;
        this.projector = projector;
        loadFromSession();
    }

    private void loadFromSession() {
        if (session == null || session.getSettings() == null) return;
        SublySettings s = session.getSettings();
        if (fontSizeSpinner != null) fontSizeSpinner.getValueFactory().setValue(s.getSubtitleFontSize());
        if (textColorPicker != null) {
            try {
                textColorPicker.setValue(Color.web(s.getSubtitleColor()));
            } catch (Exception ignored) {
            }
        }
        if (fontFamilyCombo != null) fontFamilyCombo.getSelectionModel().select(s.getSubtitleFontFamily());
        if (fontWeightCombo != null) fontWeightCombo.getSelectionModel().select(s.getFontWeight().name());
        if (borderWeightCombo != null) borderWeightCombo.getSelectionModel().select(s.getSubtitleBorderWeight().name());
        pushToProjector();
    }

    private void installListeners() {
        if (fontSizeSpinner != null) {
            fontSizeSpinner.valueProperty().addListener((o, ov, nv) -> {
                if (nv != null) applyFontSize(nv);
            });
        }
        if (textColorPicker != null) textColorPicker.valueProperty().addListener((o, ov, nv) -> {
            if (nv == null || session == null) return;
            session.update(st -> st.setSubtitleColor(ColorConvertUtils.toHexString(nv)));
            markDirty.run();
            pushToProjector();
        });
        if (fontFamilyCombo != null)
            fontFamilyCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (nv == null || session == null) return;
                session.update(s -> s.setSubtitleFontFamily(nv));
                markDirty.run();
                pushToProjector();
            });
        if (fontWeightCombo != null)
            fontWeightCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (nv == null || session == null) return;
                session.update(s -> s.setFontWeight(FontWeight.valueOf(nv)));
                markDirty.run();
                pushToProjector();
            });
        if (borderWeightCombo != null)
            borderWeightCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (nv == null || session == null) return;
                session.update(s -> s.setSubtitleBorderWeight(BorderWeight.valueOf(nv)));
                markDirty.run();
                pushToProjector();
            });
        if (borderColorPicker != null) {
            borderColorPicker.valueProperty().addListener((o, ov, nv) -> {
                if (nv == null || session == null) return;
                session.update(s -> s.setSubtitleBorderColor(ColorConvertUtils.toHexString(nv)));
                markDirty.run();
                pushToProjector();
            });
        }
    }

    private void applyFontSize(int size) {
        if (session != null) session.update(s -> s.setSubtitleFontSize(size));
        markDirty.run();
        pushToProjector();
    }

    private void pushToProjector() {
        if (session == null || projector == null || session.getSettings() == null) return;
        projector.applyLabelSettings(session.getSettings());
    }
}