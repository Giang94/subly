package com.app.subly.controller.manager;

import com.app.subly.component.ProjectorRef;
import com.app.subly.model.Subtitle;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ShowScreenToggleManager {
    private static final String OPEN_TEXT = "Open Show Screen";
    private static final String CLOSE_TEXT = "Close Show Screen";

    private final ToggleButton toggleShowScreenButton;
    private final TableView<Subtitle> subtitleTable;
    private final ProjectorRef projectorRef;
    private final BackgroundSettingsManager backgroundManager;

    private final Runnable applyToggleText;
    private final Supplier<Integer> fontSizeSupplier;
    private final Supplier<Color> textColorSupplier;
    private final BiConsumer<Integer, Color> applySettingsToProjector;
    private final Runnable updatePreviewAppearance;

    public ShowScreenToggleManager(
            ToggleButton toggleShowScreenButton,
            TableView<Subtitle> subtitleTable,
            ProjectorRef projectorRef,
            BackgroundSettingsManager backgroundManager,
            Runnable applyToggleText,
            Supplier<Integer> fontSizeSupplier,
            Supplier<Color> textColorSupplier,
            BiConsumer<Integer, Color> applySettingsToProjector,
            Runnable updatePreviewAppearance
    ) {
        this.toggleShowScreenButton = Objects.requireNonNull(toggleShowScreenButton);
        this.subtitleTable = Objects.requireNonNull(subtitleTable);
        this.projectorRef = Objects.requireNonNull(projectorRef);
        this.backgroundManager = Objects.requireNonNull(backgroundManager);
        this.applyToggleText = Objects.requireNonNull(applyToggleText);
        this.fontSizeSupplier = Objects.requireNonNull(fontSizeSupplier);
        this.textColorSupplier = Objects.requireNonNull(textColorSupplier);
        this.applySettingsToProjector = Objects.requireNonNull(applySettingsToProjector);
        this.updatePreviewAppearance = Objects.requireNonNull(updatePreviewAppearance);
    }

    public void initialize() {
        applyToggleText.run();

        // Lock the toggle’s size so text changes don’t resize it
        lockToggleSizeWhenReady();

        toggleShowScreenButton.setOnAction(e -> {
            if (toggleShowScreenButton.isSelected()) {
                projectorRef.showEnsure();
                Subtitle sel = subtitleTable.getSelectionModel().getSelectedItem();
                if (sel != null && sel.getPrimaryText() != null) {
                    projectorRef.get().setText(sel.getPrimaryText());
                }
                backgroundManager.applyBackground();
                applySettingsToProjector.accept(fontSizeSupplier.get(), textColorSupplier.get());
                updatePreviewAppearance.run();
            } else {
                projectorRef.hideIfVisible();
            }
            applyToggleText.run();
        });
    }

    // --- Fixed-size toggle button helpers ---

    private void lockToggleSizeWhenReady() {
        if (toggleShowScreenButton.getScene() == null) {
            toggleShowScreenButton.sceneProperty().addListener((o, ov, nv) -> {
                if (nv != null) Platform.runLater(this::lockToggleSize);
            });
        } else {
            Platform.runLater(this::lockToggleSize);
        }
    }

    private void lockToggleSize() {
        String original = toggleShowScreenButton.getText();

        double wOpen = measurePrefWidth(OPEN_TEXT);
        double wClose = measurePrefWidth(CLOSE_TEXT);
        double width = Math.ceil(Math.max(wOpen, wClose));

        double hOpen = measurePrefHeight(OPEN_TEXT);
        double hClose = measurePrefHeight(CLOSE_TEXT);
        double height = Math.ceil(Math.max(hOpen, hClose));

        toggleShowScreenButton.setMinWidth(width);
        toggleShowScreenButton.setPrefWidth(width);
        toggleShowScreenButton.setMaxWidth(width);

        toggleShowScreenButton.setMinHeight(height);
        toggleShowScreenButton.setPrefHeight(height);
        toggleShowScreenButton.setMaxHeight(height);

        toggleShowScreenButton.setText(original);
    }

    private double measurePrefWidth(String text) {
        String old = toggleShowScreenButton.getText();
        toggleShowScreenButton.setText(text);
        toggleShowScreenButton.applyCss();
        double w = toggleShowScreenButton.prefWidth(-1);
        toggleShowScreenButton.setText(old);
        return w;
    }

    private double measurePrefHeight(String text) {
        String old = toggleShowScreenButton.getText();
        toggleShowScreenButton.setText(text);
        toggleShowScreenButton.applyCss();
        double h = toggleShowScreenButton.prefHeight(-1);
        toggleShowScreenButton.setText(old);
        return h;
    }
}