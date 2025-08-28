package com.app.subly;

import com.app.subly.component.ControlPanel;
import com.app.subly.component.Projector;
import com.app.subly.storage.AppSettings;
import com.app.subly.storage.AppSettingsManager;
import com.app.subly.utils.AppIconUtils;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class SublyApplication extends Application {

    private Projector projector;
    private ControlPanel controlPanel;
    private AppSettings settings;

    @Override
    public void start(Stage primaryStage) throws IOException {
        settings = AppSettingsManager.load();

        projector = new Projector();
        controlPanel = new ControlPanel(projector);

        projector.applySettings(settings);

        // Set the control panel as the primary stage
        primaryStage.setTitle("Control Panel");
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
}
