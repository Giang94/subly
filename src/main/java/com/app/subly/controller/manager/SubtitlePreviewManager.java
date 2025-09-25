package com.app.subly.controller.manager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.shape.Rectangle;

public class SubtitlePreviewManager {

    private static final double ASPECT_W = 16.0;
    private static final double ASPECT_H = 9.0;

    private final StackPane container;
    private final ImageView bgImageView;

    private final TextFlow textFlow;
    private final Text textNode;

    // Clip the container to enforce the visible bounds
    private final Rectangle textClip = new Rectangle();

    private boolean sizingConfigured = false;

    public SubtitlePreviewManager(StackPane container, ImageView bgImageView, TextFlow textFlow, Text textNode) {
        this.container = container;
        this.bgImageView = bgImageView;
        this.textFlow = textFlow;
        this.textNode = textNode;
    }

    public void initialize() {
        if (sizingConfigured) return;
        sizingConfigured = true;

        // Center the TextFlow node inside the container
        StackPane.setAlignment(textFlow, Pos.CENTER);
        textFlow.setTextAlignment(TextAlignment.CENTER);

        // Ensure the Text is attached
        if (!textFlow.getChildren().contains(textNode)) {
            textFlow.getChildren().setAll(textNode);
        }

        // Clip the container to the fixed preview area
        container.setClip(textClip);

        // Measure a single line accurately and build a safe 3-line height
        Text oneLine = new Text("Wg");

        Runnable applyHeights = () -> {
            if (container.getScene() == null) return;

            textFlow.applyCss();
            textFlow.layout();

            oneLine.setFont(textNode.getFont());
            double lineHeight = Math.ceil(oneLine.getLayoutBounds().getHeight());
            double lineSpacing = safe(textFlow.getLineSpacing());

            Insets pad = safeInsets(textFlow.getPadding());
            double padT = pad.getTop();
            double padB = pad.getBottom();
            double padL = pad.getLeft();
            double padR = pad.getRight();

            // Exactly 3 lines (no extra multiplier)
            double totalHeight = Math.ceil(lineHeight * 3 + lineSpacing * 2 + padT + padB + 2) * 1.5;
            double width = Math.ceil((totalHeight * ASPECT_W) / ASPECT_H);

            // Fix preview container size (the visible bounds)
            fixSize(container, width, totalHeight);

            // TextFlow width is fixed for wrapping; height should be computed from content
            textFlow.setPrefWidth(width);
            textFlow.setMinWidth(width);
            textFlow.setMaxWidth(width);

            // Do NOT force height; let it compute by content so StackPane can center it vertically
            textFlow.setMinHeight(Region.USE_PREF_SIZE);
            textFlow.setPrefHeight(Region.USE_COMPUTED_SIZE);
            textFlow.setMaxHeight(Region.USE_PREF_SIZE);

            // Wrapping width excludes padding
            double wrap = Math.max(0, width - (padL + padR));
            textNode.setWrappingWidth(wrap);

            // Update clip to the container bounds
            textClip.setX(0);
            textClip.setY(0);
            textClip.setWidth(width);
            textClip.setHeight(totalHeight);

            // Refit background to container
            refitImage();
        };

        textNode.fontProperty().addListener((o, ov, nv) -> applyHeights.run());
        container.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) Platform.runLater(applyHeights);
        });

        javafx.beans.value.ChangeListener<Number> sizeWatcher = (o, ov, nv) -> refitImage();
        container.widthProperty().addListener(sizeWatcher);
        container.heightProperty().addListener(sizeWatcher);

        container.setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)))
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
            textNode.setFill(textColor);
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

    public void setText(String text) {
        textNode.setText(text == null ? "" : text);
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

        double scale = Math.max(w / iw, h / ih);
        double sw = iw * scale;
        double sh = ih * scale;

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

    private Insets safeInsets(Insets in) {
        return in == null ? Insets.EMPTY : in;
    }
}