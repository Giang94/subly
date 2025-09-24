package com.app.subly.controller.manager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class SubtitlePreviewManager {

    private static final double ASPECT_W = 16.0;
    private static final double ASPECT_H = 9.0;

    private final StackPane container;
    private final ImageView bgImageView;
    private final Label textLabel;

    private boolean sizingConfigured = false;

    public SubtitlePreviewManager(StackPane container, ImageView bgImageView, Label textLabel) {
        this.container = container;
        this.bgImageView = bgImageView;
        this.textLabel = textLabel;
    }

    public void initialize() {
        if (sizingConfigured) return;
        sizingConfigured = true;

        textLabel.setWrapText(false);
        textLabel.setTextAlignment(TextAlignment.CENTER);
        textLabel.setAlignment(Pos.CENTER);

        Text measurer = new Text("Wg\nWg\nWg\nWg");

        Runnable applyHeights = () -> {
            if (container.getScene() == null) return;
            textLabel.applyCss();
            textLabel.layout();

            measurer.setFont(textLabel.getFont());
            measurer.setWrappingWidth(10_000);
            double lineHeight = Math.ceil(measurer.getLayoutBounds().getHeight() / 3.0);
            if (lineHeight <= 0) lineHeight = 20;

            double padT = safe(textLabel.getPadding().getTop());
            double padB = safe(textLabel.getPadding().getBottom());

            double totalHeight = lineHeight * 3 + padT + padB + 1;
            double width = (totalHeight * ASPECT_W) / ASPECT_H;

            fixSize(container, width, totalHeight);
            refitImage();
        };

        textLabel.fontProperty().addListener((o, ov, nv) -> applyHeights.run());
        container.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) Platform.runLater(applyHeights);
        });

        ChangeListener<Number> sizeWatcher = (o, ov, nv) -> refitImage();
        container.widthProperty().addListener(sizeWatcher);
        container.heightProperty().addListener(sizeWatcher);

        container.setBorder(
                new Border(new BorderStroke(
                        Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
                ))
        );

        Platform.runLater(applyHeights);
    }

    public void updateAppearance(
            Color textColor,
            RadioButton transparentRadio,
            RadioButton colorRadio,
            RadioButton imageRadio,
            ColorPicker bgColorPicker,
            TextField imagePathField
    ) {
        if (textColor != null) {
            textLabel.setTextFill(textColor);
        }

        boolean imageMode = imageRadio != null && imageRadio.isSelected();
        if (imageMode) {
            setBackgroundImage(imagePathField != null ? imagePathField.getText() : null);
            return;
        } else {
            bgImageView.setImage(null);
        }

        Background bg = null;
        if (transparentRadio != null && transparentRadio.isSelected()) {
            bg = Background.EMPTY;
        } else if (colorRadio != null && colorRadio.isSelected()) {
            if (bgColorPicker != null && bgColorPicker.getValue() != null) {
                bg = new Background(new BackgroundFill(bgColorPicker.getValue(), CornerRadii.EMPTY, Insets.EMPTY));
            }
        }
        container.setBackground(bg);
    }

    private void setBackgroundImage(String uriOrPath) {
        if (uriOrPath == null || uriOrPath.isBlank()) {
            bgImageView.setImage(null);
            return;
        }

        Image img = safeLoad(resolveToUri(uriOrPath));
        if (img == null) return;

        bgImageView.setImage(img);
        refitImage();
    }

    private void refitImage() {
        if (bgImageView == null || bgImageView.getImage() == null) return;
        double w = container.getWidth();
        double h = container.getHeight();
        if (w <= 0 || h <= 0) return;

        Image img = bgImageView.getImage();
        double iw = img.getWidth();
        double ih = img.getHeight();
        if (iw <= 0 || ih <= 0) return;

        // Scale to cover
        double scale = Math.max(w / iw, h / ih);
        double sw = iw * scale;
        double sh = ih * scale;

        // Compute centered viewport
        double vx = (sw - w) / (2 * scale);
        double vy = (sh - h) / (2 * scale);
        double vw = w / scale;
        double vh = h / scale;

        bgImageView.setViewport(new javafx.geometry.Rectangle2D(vx, vy, vw, vh));
        bgImageView.setFitWidth(w);
        bgImageView.setFitHeight(h);
    }


    private void fixSize(Region r, double w, double h) {
        if (w <= 0 || h <= 0) return;
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
    }

    private String resolveToUri(String path) {
        if (path == null) return null;
        if (path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) return path;
        java.io.File f = new java.io.File(path);
        return f.exists() ? f.toURI().toString() : path;
    }

    private Image safeLoad(String uri) {
        if (uri == null) return null;
        try {
            Image img = new Image(uri, false);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }

    private double safe(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v)) ? 0 : v;
    }
}
