package com.app.subly.component;

import com.app.subly.model.SublySettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Projector {
    private Stage stage;
    private Label label;
    private StackPane layout;

    public Projector() {
        stage = new SublyApplicationStage();
        stage.initStyle(StageStyle.TRANSPARENT);

        label = new Label();
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);

        layout = new StackPane(label);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(0, 40, 0, 40)); // default side padding

        Scene scene = new Scene(layout, 600, 400);
        stage.setScene(scene);

        initProjectorBehavior(stage, layout);
    }

    public void setText(String text) {
        label.setText(text.replace("\\n", "\n"));
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
        stage.getScene().setFill(Color.rgb(0, 0, 0, 0.02));
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

    public void setFontSize(int px) {
        if (label == null) return;

        String style = label.getStyle();
        if (style == null) style = "";

        // Remove any existing font-size declaration (case-insensitive)
        style = style.replaceAll("(?i)\\s*-fx-font-size\\s*:\\s*[^;]+;?", "");

        // Ensure styles are separated by a semicolon
        if (!style.isBlank() && !style.trim().endsWith(";")) {
            style += ";";
        }

        // Append the new font size, preserving other label styles (e.g., text fill)
        label.setStyle(style + "-fx-font-size: " + px + "px;");
    }

    public void setContentPadding(double top, double right, double bottom, double left) {
        if (layout != null) {
            layout.setPadding(new Insets(top, right, bottom, left));
        }
    }

    public void setHorizontalPadding(double padding) {
        setContentPadding(0, padding, 0, padding);
    }
}