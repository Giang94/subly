package com.app.subly.component;

import com.app.subly.model.SublySettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
        // Clear any image and color
        layout.setBackground(null);
        stage.getScene().getRoot().setStyle("-fx-background-color: transparent;");
        stage.getScene().setFill(Color.rgb(0, 0, 0, 0.01));
    }

    public void setBackgroundColor(Color color) {
        // Clear any image first
        layout.setBackground(null);
        String rgb = String.format("rgb(%d, %d, %d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        stage.getScene().getRoot().setStyle("-fx-background-color: " + rgb + ";");
        stage.getScene().setFill(Color.rgb(0, 0, 0, 1.0));
    }

    public void setBackgroundImage(String uriOrPath) {
        if (uriOrPath == null || uriOrPath.isBlank()) {
            layout.getChildren().removeIf(node -> node instanceof ImageView);
            return;
        }

        String uri = uriOrPath;
        if (!uri.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            java.io.File f = new java.io.File(uri);
            if (f.exists()) uri = f.toURI().toString();
        }

        Image image = new Image(uri, false);
        if (image.isError()) return;

        layout.getChildren().removeIf(ImageView.class::isInstance);

        ImageView bgView = new ImageView(image);
        bgView.setPreserveRatio(true);
        bgView.setSmooth(true);
        bgView.setCache(true);

        // auto cover behavior
        stage.getScene().widthProperty().addListener((obs, oldV, newV) -> resizeCover(bgView, image));
        stage.getScene().heightProperty().addListener((obs, oldV, newV) -> resizeCover(bgView, image));

        // init resize
        resizeCover(bgView, image);

        layout.getChildren().add(0, bgView);
        layout.setBackground(null);
        layout.setStyle("-fx-background-color: transparent;");
    }

    private void resizeCover(ImageView bgView, Image image) {
        double sceneW = stage.getScene().getWidth();
        double sceneH = stage.getScene().getHeight();

        double imgW = image.getWidth();
        double imgH = image.getHeight();

        double scale = Math.max(sceneW / imgW, sceneH / imgH);

        bgView.setFitWidth(imgW * scale);
        bgView.setFitHeight(imgH * scale);

        // Center align
        StackPane.setAlignment(bgView, Pos.CENTER);
    }

    public void applySettings(SublySettings settings) {
        switch (settings.getBackgroundType()) {
            case TRANSPARENT -> setTransparentBackground();
            case SOLID_COLOR -> setBackgroundColor(Color.web(settings.getProjectorColor()));
            case IMAGE -> setBackgroundImage(settings.getProjectorImageUri());
        }

        label.setStyle(String.format("-fx-font-size: %spx; -fx-text-fill: %s;",
                settings.getSubtitleFontSize(),
                settings.getSubtitleColor()));
    }

    public void setFontSize(int px) {
        if (label == null) return;
        String style = label.getStyle();
        if (style == null) style = "";
        style = style.replaceAll("(?i)\\s*-fx-font-size\\s*:\\s*[^;]+;?", "");
        if (!style.isBlank() && !style.trim().endsWith(";")) style += ";";
        label.setStyle(style + "-fx-font-size: " + px + "px;");
    }

    public void setContentPadding(double top, double right, double bottom, double left) {
        if (layout != null) layout.setPadding(new Insets(top, right, bottom, left));
    }
}