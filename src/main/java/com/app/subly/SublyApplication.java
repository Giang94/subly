package com.app.subly;

import com.app.subly.component.ControlPanel;
import com.app.subly.component.Projector;
import com.app.subly.model.SublySettings;
import com.app.subly.persistence.AppSettingsIO;
import com.app.subly.utils.AppIconUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;

public class SublyApplication extends Application {

    private static final String APP_NAME = "Subly";

    private Projector projector;
    private ControlPanel controlPanel;
    private SublySettings settings;

    private String currentTitle = "Untitled";
    private boolean dirty = false;

    @Override
    public void start(Stage primaryStage) throws IOException {
        settings = AppSettingsIO.load();
        projector = new Projector();
        controlPanel = new ControlPanel(this, projector);
        projector.applySettings(settings);

        configureStage(primaryStage, controlPanel.getScene());
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (projector != null && projector.isVisible()) {
                projector.close();
            }
        });

        lockMinimumUsableSize(primaryStage);
    }

    private void configureStage(Stage stage, Scene scene) {
        stage.setScene(scene);
        stage.getIcons().add(AppIconUtils.getAppIcon());
        applyWindowTitle(); // initial
    }

    private void lockMinimumUsableSize(Stage stage) {
        Platform.runLater(() -> {
            if (controlPanel == null || controlPanel.getScene() == null) return;
            var root = controlPanel.getScene().getRoot();

            // Bind side panels so they cannot collapse below pref width
            if (root instanceof BorderPane bp) {
                Region left = safeRegion(bp.getLeft());
                Region right = safeRegion(bp.getRight());
                if (left != null) left.minWidthProperty().bind(left.prefWidthProperty());
                if (right != null) right.minWidthProperty().bind(right.prefWidthProperty());
            }

            double decoW = stage.getWidth() - controlPanel.getScene().getWidth();
            double decoH = stage.getHeight() - controlPanel.getScene().getHeight();
            double minW = root.prefWidth(-1) + decoW;
            double minH = root.prefHeight(-1) + decoH;
            stage.setMinWidth(minW);
            stage.setMinHeight(minH);
        });
    }

    private Region safeRegion(Object node) {
        return (node instanceof Region r) ? r : null;
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Backward compatible method name
    public void updateSetting(SublySettings newSettings) {
        updateSettings(newSettings);
    }

    public void updateSettings(SublySettings newSettings) {
        if (newSettings == null) return;
        this.settings = newSettings;
        projector.applySettings(settings);
    }

    public void updateTitle(String title) {
        this.currentTitle = (title == null || title.isBlank()) ? "Untitled" : title.trim();
        applyWindowTitle();
    }

    public void setDirty(boolean value) {
        this.dirty = value;
        applyWindowTitle();
    }

    private void applyWindowTitle() {
        Runnable task = () -> {
            if (controlPanel == null || controlPanel.getScene() == null) return;
            var window = controlPanel.getScene().getWindow();
            if (window instanceof Stage stage) {
                String finalTitle = APP_NAME + " - " + currentTitle + (dirty ? " *" : "");
                stage.setTitle(finalTitle);
            }
        };
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}