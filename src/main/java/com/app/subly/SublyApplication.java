package com.app.subly;

import com.app.subly.component.ControlPanel;
import com.app.subly.component.Projector;
import com.app.subly.model.SublySettings;
import com.app.subly.persistence.AppSettingsManager;
import com.app.subly.utils.AppIconUtils;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class SublyApplication extends Application {

    private Projector projector;
    private ControlPanel controlPanel;
    private SublySettings settings;

    @Override
    public void start(Stage primaryStage) throws IOException {
        settings = AppSettingsManager.load();

        projector = new Projector();
        controlPanel = new ControlPanel(this, projector);

        projector.applySettings(settings);

        // Set the control panel as the primary stage
        primaryStage.setTitle("Subly - Untitled*");
        primaryStage.setScene(controlPanel.getScene());
        primaryStage.getIcons().add(AppIconUtils.getAppIcon());
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            if (projector.isVisible()) {
                projector.close();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void updateSetting(SublySettings sublySettings) {
        this.settings = sublySettings;
        settings.setProjectorColor(sublySettings.getProjectorColor());
        settings.setSubtitleColor(sublySettings.getSubtitleColor());
        settings.setSubtitleFontSize(sublySettings.getSubtitleFontSize());
        settings.setProjectorTransparent(sublySettings.isProjectorTransparent());

        // Apply immediately to projector
        projector.applySettings(settings);
    }

    public void updateTitle(String title) {
        Stage primaryStage = (Stage) controlPanel.getScene().getWindow();
        primaryStage.setTitle("Subly - " + title);
    }
}
