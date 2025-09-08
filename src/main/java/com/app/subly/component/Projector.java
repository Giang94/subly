package com.app.subly.component;

import com.app.subly.common.SublyApplicationStage;
import com.app.subly.storage.SublySettings;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Projector {
    private Stage stage;
    private Label label;

    public Projector() {
        stage = new SublyApplicationStage();
        stage.setTitle("Show Screen");
        stage.initStyle(StageStyle.TRANSPARENT);

        label = new Label();

        StackPane layout = new StackPane(label);
        Scene scene = new Scene(layout, 600, 400);
        stage.setScene(scene);

        initProjectorBehavior(stage, layout);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void show() {
        stage.show();
    }

    public void hide() {
        stage.hide();
    }

    public boolean isVisible() {
        return stage.isShowing();
    }

    public void close() {
        stage.close();
    }

    private double xOffset = 0;
    private double yOffset = 0;

    public void initProjectorBehavior(Stage stage, Parent root) {
        // Double click anywhere -> toggle fullscreen
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        // Click and drag anywhere -> move window (only if not fullscreen)
        root.setOnMousePressed(event -> {
            if (!stage.isFullScreen()) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });

        root.setOnMouseDragged(event -> {
            if (!stage.isFullScreen()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    public void setTransparentBackground() {
        stage.getScene().getRoot().setStyle("-fx-background-color: transparent;");
        stage.getScene().setFill(Color.rgb(0, 0, 0, 0.05));
    }

    public void setBackgroundColor(Color color) {
        String rgb = String.format("rgb(%d, %d, %d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        (stage.getScene().getRoot()).setStyle("-fx-background-color: " + rgb + ";");
    }

    public void applySettings(SublySettings settings) {
        if (settings.isProjectorTransparent()) {
            setTransparentBackground();
        } else {
            setBackgroundColor(Color.web(settings.getProjectorColor()));
        }

        label.setStyle(String.format("-fx-font-size: %spx; -fx-text-fill: %s;",
                settings.getSubtitleFontSize(),
                settings.getSubtitleColor()));
    }
}