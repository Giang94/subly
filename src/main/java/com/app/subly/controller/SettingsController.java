// java
package com.app.subly.controller;

import com.app.subly.component.Projector;
import com.app.subly.model.SublySettings;
import com.app.subly.project.SublyProjectSession;
import com.app.subly.utils.ColorConvertUtils;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class SettingsController {

    @FXML
    private ToggleGroup bgGroup;
    @FXML
    private RadioButton transparentOption;
    @FXML
    private RadioButton colorOption;
    @FXML
    private ColorPicker colorPicker;       // Background color
    @FXML
    private ColorPicker textColorPicker;   // Text color
    @FXML
    private Slider textSizeSlider;         // Text size
    @FXML
    private Label textSizeValueLabel;
    @FXML
    private Button applyButton;

    private Projector projector;
    private SublyProjectSession session;
    private Runnable onDirty;

    public void setProjector(Projector projector) {
        this.projector = projector;
    }

    public void setSession(SublyProjectSession session) {
        this.session = session;
        loadFromSession();
    }

    public void setOnDirty(Runnable onDirty) {
        this.onDirty = onDirty;
    }

    @FXML
    private void initialize() {
        // Toggle group
        bgGroup = new ToggleGroup();
        transparentOption.setToggleGroup(bgGroup);
        colorOption.setToggleGroup(bgGroup);

        // Enable/disable background color picker based on selection
        colorPicker.disableProperty().bind(colorOption.selectedProperty().not());

        // Text slider config
        if (textSizeSlider != null) {
            textSizeSlider.setMin(24);
            textSizeSlider.setMax(240);
            textSizeSlider.setMajorTickUnit(10);
            textSizeSlider.setMinorTickCount(9);
            textSizeSlider.setBlockIncrement(1);
            textSizeSlider.setShowTickMarks(true);
            textSizeSlider.setShowTickLabels(true);
            textSizeSlider.setSnapToTicks(true);

            if (textSizeValueLabel != null) {
                textSizeValueLabel.textProperty().bind(
                        Bindings.format("%.0f px", textSizeSlider.valueProperty())
                );
            }
        }

        applyButton.setOnAction(e -> applyChanges());
    }

    private void loadFromSession() {
        if (session == null) return;
        SublySettings s = session.getSettings();

        // Background
        if (s.isProjectorTransparent()) {
            transparentOption.setSelected(true);
        } else {
            colorOption.setSelected(true);
        }
        colorPicker.setValue(ColorConvertUtils.toJavaFxColor(s.getProjectorColor()));

        // Text
        if (textColorPicker != null) {
            textColorPicker.setValue(ColorConvertUtils.toJavaFxColor(s.getSubtitleColor()));
        }
        if (textSizeSlider != null) {
            int currentSize = s.getSubtitleFontSize();
            if (currentSize <= 0) currentSize = 24;
            textSizeSlider.setValue(currentSize);
        }
    }

    private void applyChanges() {
        if (session == null) return;

        boolean makeTransparent = transparentOption.isSelected();
        Color bg = colorPicker.getValue();
        Color textColor = (textColorPicker != null) ? textColorPicker.getValue() : null;
        int newSize = (textSizeSlider != null) ? (int) Math.round(textSizeSlider.getValue()) : 24;

        // Update session model (persisted with the project)
        session.update(s -> {
            s.setProjectorTransparent(makeTransparent);
            if (!makeTransparent && bg != null) {
                s.setProjectorColor(ColorConvertUtils.toHexString(bg));
            }
            s.setSubtitleFontSize(newSize);
            if (textColor != null) {
                s.setSubtitleColor(ColorConvertUtils.toHexString(textColor));
            }
        });

        // Apply immediately to Projector
        if (projector != null) {
            projector.applySettings(session.getSettings());
        }

        if (onDirty != null) onDirty.run();
    }
}