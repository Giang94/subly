package com.app.subly.controller;

import com.app.subly.component.Projector;
import com.app.subly.storage.SublySettings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;

public class SettingsController {

    @FXML
    private ToggleGroup bgGroup;
    @FXML
    private RadioButton transparentOption;
    @FXML
    private RadioButton colorOption;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private Button applyButton;

    private Projector projector;

    public void setProjector(Projector projector) {
        this.projector = projector;
    }

    @FXML
    private void initialize() {
        // Default selection
        transparentOption.setSelected(true);

        // Disable color picker unless "Color" option is selected
        colorPicker.disableProperty().bind(colorOption.selectedProperty().not());

        bgGroup = new ToggleGroup();
        transparentOption.setToggleGroup(bgGroup);
        colorOption.setToggleGroup(bgGroup);

        applyButton.setOnAction(e -> {
            if (projector == null) return;

            if (transparentOption.isSelected()) {
                projector.setTransparentBackground();
            } else if (colorOption.isSelected()) {
                Color chosen = colorPicker.getValue();
                projector.setBackgroundColor(chosen);
            }

            SublySettings sublySettings = new SublySettings();
            sublySettings.setProjectorTransparent(transparentOption.isSelected());
        });
    }
}
