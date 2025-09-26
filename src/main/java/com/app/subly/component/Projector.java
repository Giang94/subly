package com.app.subly.component;

import com.app.subly.model.Chapter;
import com.app.subly.model.SublySettings;
import com.app.subly.utils.DialogHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

import static com.app.subly.utils.Fonts.mapFxWeight;

@Getter
@Setter
public class Projector {

    private Stage stage;
    private Label label;
    private StackPane layout;
    private List<Chapter> chapters = Collections.emptyList();

    private int currentChapterIndex = -1;
    private int currentRowIndex = -1;

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
        layout.getChildren().removeIf(node -> node instanceof ImageView);
        layout.setStyle("");
        stage.getScene().setFill(Color.TRANSPARENT);
        layout.setBackground(
                new javafx.scene.layout.Background(
                        new javafx.scene.layout.BackgroundFill(
                                color, null, null
                        )
                )
        );
    }

    public boolean setBackgroundImage(String uriOrPath, String caller) {
        System.out.println("[" + caller + "] Setting background image: " + uriOrPath);
        if (uriOrPath == null || uriOrPath.isBlank()) {
            layout.getChildren().removeIf(node -> node instanceof ImageView);
            return true;
        }

        java.io.File f = new java.io.File(uriOrPath);
        if (f.exists()) {
            String uri = f.toURI().toString();
            Image image;
            try {
                image = new Image(uri, false);
            } catch (Exception ex) {
                System.out.println("Failed to load image: " + ex.getMessage());
                DialogHelper.showImageLoadFailure(stage, uri);
                return false;
            }

            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                System.out.println("Failed to load error image: " + image.getException());
                DialogHelper.showImageLoadFailure(stage, uri);
                return false;
            }

            layout.getChildren().removeIf(ImageView.class::isInstance);

            ImageView bgView = new ImageView(image);
            bgView.setPreserveRatio(true);
            bgView.setSmooth(true);
            bgView.setCache(true);

            stage.getScene().widthProperty().addListener((obs, o, n) -> resizeCover(bgView, image));
            stage.getScene().heightProperty().addListener((obs, o, n) -> resizeCover(bgView, image));
            resizeCover(bgView, image);

            layout.getChildren().addFirst(bgView);
            layout.setBackground(null);
            layout.setStyle("-fx-background-color: transparent;");
            return true;
        } else {
            System.out.println("File does not exist: " + uriOrPath);
            DialogHelper.showImageLoadFailure(stage, uriOrPath);
            return false;
        }
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
        applyLabelSettings(settings);

        switch (settings.getBackgroundType()) {
            case TRANSPARENT -> setTransparentBackground();
            case SOLID_COLOR -> setBackgroundColor(Color.web(settings.getProjectorColor()));
            case IMAGE -> setBackgroundImage(settings.getProjectorImageUri(), this.getClass().getSimpleName());
        }
    }

    public void applyLabelSettings(SublySettings settings) {
        if (settings == null) return;

        // Force the exact font family + weight + size
        Font fxFont = Font.font(
                settings.getSubtitleFontFamily(),
                mapFxWeight(settings.getFontWeight()),
                settings.getSubtitleFontSize()
        );
        label.setFont(fxFont);

        StringBuilder style = new StringBuilder()
                .append("-fx-text-fill: ").append(settings.getSubtitleColor()).append(";");

        var border = settings.getSubtitleBorderWeight();
        boolean borderOn = border != null && !border.isNone();
        if (borderOn) {
            double radius = border.getRadius();
            double spread = border.getSpread();

            String outlineHex = settings.getSubtitleBorderColor();
            if (outlineHex == null || outlineHex.isBlank()) outlineHex = "#000000";
            style.append("-fx-effect: dropshadow(gaussian, ")
                    .append(outlineHex).append(", ")
                    .append(radius).append(", ")
                    .append(spread).append(", 0, 0);");
        } else {
            style.append("-fx-effect: none;");
        }

        label.setStyle(style.toString());
    }
}