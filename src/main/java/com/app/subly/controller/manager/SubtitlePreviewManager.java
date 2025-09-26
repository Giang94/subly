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

    public enum PreviewSizingMode {
        FIXED_ASPECT,   // Original behavior (fixed 16:9 box)
        FLEX_WIDTH      // Fixed height (3 lines), width grows to fill parent
    }

    private static final double ASPECT_W = 16.0;
    private static final double ASPECT_H = 9.0;

    private final StackPane container;
    private final ImageView bgImageView;
    private final TextFlow textFlow;
    private final Text textNode;
    private final Rectangle textClip = new Rectangle();

    private boolean initialized = false;
    private PreviewSizingMode sizingMode;

    public SubtitlePreviewManager(StackPane container,
                                  ImageView bgImageView,
                                  TextFlow textFlow,
                                  Text textNode) {
        this(container, bgImageView, textFlow, textNode, PreviewSizingMode.FLEX_WIDTH);
    }

    public SubtitlePreviewManager(StackPane container,
                                  ImageView bgImageView,
                                  TextFlow textFlow,
                                  Text textNode,
                                  PreviewSizingMode sizingMode) {
        this.container = container;
        this.bgImageView = bgImageView;
        this.textFlow = textFlow;
        this.textNode = textNode;
        this.sizingMode = (sizingMode == null) ? PreviewSizingMode.FIXED_ASPECT : sizingMode;
    }

    public void setSizingMode(PreviewSizingMode mode) {
        if (mode == null || mode == this.sizingMode) return;
        this.sizingMode = mode;
        if (initialized) Platform.runLater(this::applySizing);
    }

    public PreviewSizingMode getSizingMode() {
        return sizingMode;
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;

        StackPane.setAlignment(textFlow, Pos.CENTER);
        textFlow.setTextAlignment(TextAlignment.CENTER);
        if (!textFlow.getChildren().contains(textNode)) {
            textFlow.getChildren().setAll(textNode);
        }
        container.setClip(textClip);

        textNode.fontProperty().addListener((o, ov, nv) -> applySizing());
        container.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) Platform.runLater(this::applySizing);
        });

        container.widthProperty().addListener((o, ov, nv) -> {
            if (sizingMode == PreviewSizingMode.FLEX_WIDTH) updateWrapping();
            refitImage();
        });
        container.heightProperty().addListener((o, ov, nv) -> refitImage());

        container.setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)))
        );

        Platform.runLater(this::applySizing);
    }

    private void applySizing() {
        if (container.getScene() == null) return;

        // Clear previous bindings
        unfixSize(container);
        textFlow.minWidthProperty().unbind();
        textFlow.prefWidthProperty().unbind();
        textFlow.maxWidthProperty().unbind();
        textClip.widthProperty().unbind();

        textFlow.applyCss();
        textFlow.layout();

        // Measure single line
        Text probe = new Text("Wg");
        probe.setFont(textNode.getFont());
        double lineHeight = Math.ceil(probe.getLayoutBounds().getHeight());
        double lineSpacing = safe(textFlow.getLineSpacing());
        Insets pad = safeInsets(textFlow.getPadding());
        double padT = pad.getTop();
        double padB = pad.getBottom();

        // Total height (3 lines) kept same formula as original
        double totalHeight = Math.ceil(lineHeight * 3 + lineSpacing * 2 + padT + padB + 2) * 1.5;

        if (sizingMode == PreviewSizingMode.FIXED_ASPECT) {
            // Original mode: fixed 16:9 rectangle
            double width = Math.ceil((totalHeight * ASPECT_W) / ASPECT_H);
            fixSize(container, width, totalHeight);

            textFlow.setMinWidth(width);
            textFlow.setPrefWidth(width);
            textFlow.setMaxWidth(width);
            textFlow.setMinHeight(Region.USE_PREF_SIZE);
            textFlow.setPrefHeight(Region.USE_COMPUTED_SIZE);
            textFlow.setMaxHeight(Region.USE_PREF_SIZE);

            double wrap = Math.max(0, width - (pad.getLeft() + pad.getRight()));
            textNode.setWrappingWidth(wrap);

            textClip.setX(0);
            textClip.setY(0);
            textClip.setWidth(width);
            textClip.setHeight(totalHeight);
        } else {
            // FLEX_WIDTH: fixed height, width flexible
            container.setMinHeight(0);
            container.setPrefHeight(totalHeight);
            container.setMaxHeight(Double.MAX_VALUE);

            textFlow.setMinWidth(0);
            textFlow.setPrefWidth(Region.USE_COMPUTED_SIZE);
            textFlow.setMaxWidth(Double.MAX_VALUE);

            textFlow.setMinHeight(Region.USE_PREF_SIZE);
            textFlow.setPrefHeight(Region.USE_COMPUTED_SIZE);
            textFlow.setMaxHeight(totalHeight);

            textClip.setHeight(totalHeight);
            textClip.widthProperty().bind(container.widthProperty());

            updateWrapping();
        }

        refitImage();
    }

    private void updateWrapping() {
        Insets pad = safeInsets(textFlow.getPadding());
        double wrap = Math.max(0, container.getWidth() - (pad.getLeft() + pad.getRight()));
        textNode.setWrappingWidth(wrap);
    }

    public void updateAppearance(
            Color textColor,
            RadioButton transparentRadio,
            RadioButton colorRadio,
            RadioButton imageRadio,
            ColorPicker bgColorPicker,
            TextField imagePathField,
            String borderWeight,
            Color borderColor,
            boolean doUpdate) {
        if (doUpdate) {
            if (textColor != null) {
                textNode.setFill(textColor);
            }

            if (borderWeight != null && !borderWeight.equals("None") && borderColor != null) {
                textNode.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
                textNode.setStroke(borderColor);
                textNode.setStrokeWidth(1);
            } else {
                textNode.setStrokeWidth(0);
            }

            boolean imageMode = imageRadio != null && imageRadio.isSelected();
            if (imageMode) {
                setBackgroundImage(imagePathField != null ? imagePathField.getText() : null);
                return;
            } else if (bgImageView != null) {
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
    }

    public void setText(String text) {
        textNode.setText(text == null ? "" : text);
    }

    private void setBackgroundImage(String uriOrPath) {
        if (bgImageView == null) return;
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

    private void fixHeight(Region r, double h) {
        if (h <= 0) return;
        r.setMinHeight(h);
        r.setPrefHeight(h);
        r.setMaxHeight(h);
    }

    private void unfixSize(Region r) {
        r.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        r.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
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