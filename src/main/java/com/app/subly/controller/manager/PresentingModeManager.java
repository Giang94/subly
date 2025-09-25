package com.app.subly.controller.manager;

import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PresentingModeManager {

    private final ToggleButton presentingModeToggle;
    private final Button prevButton;
    private final Button nextButton;
    private final TableView<Subtitle> subtitleTable;

    private final Supplier<SublyProjectSession> sessionSupplier;
    private final Consumer<Boolean> editingEnabledConsumer;

    private PresentingModeNavigator presentingNavigator;

    public PresentingModeManager(
            ToggleButton presentingModeToggle,
            Button prevButton,
            Button nextButton,
            TableView<Subtitle> subtitleTable,
            Supplier<SublyProjectSession> sessionSupplier,
            Consumer<Boolean> editingEnabledConsumer
    ) {
        this.presentingModeToggle = presentingModeToggle;
        this.prevButton = prevButton;
        this.nextButton = nextButton;
        this.subtitleTable = Objects.requireNonNull(subtitleTable);
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier);
        this.editingEnabledConsumer = Objects.requireNonNull(editingEnabledConsumer);
    }

    public void initialize() {
        if (presentingModeToggle == null) return;
        presentingModeToggle.setSelected(false);
        updatePresentingToggleState();
        updatePresentingButtonText();
        lockToggleSizeWhenReady();
        installPrevNextHandlers();
        disablePrevNext();

        presentingModeToggle.setOnAction(e -> {
            if (presentingModeToggle.isSelected()) {
                startPresentingMode();
            } else {
                stopPresentingMode();
            }
            updatePresentingButtonText();
        });
    }

    public void onSessionSet() {
        updatePresentingToggleState();
        if (isPresenting()) {
            presentingNavigator = null;
        }
    }

    public void setPresenting(boolean presenting) {
        if (presentingModeToggle == null) return;
        if (presentingModeToggle.isSelected() == presenting) return;
        presentingModeToggle.setSelected(presenting);
        if (presenting) startPresentingMode(); else stopPresentingMode();
        updatePresentingButtonText();
    }

    public boolean isPresenting() {
        return presentingModeToggle != null && presentingModeToggle.isSelected();
    }

    public void updatePresentingToggleState() {
        if (presentingModeToggle == null) return;
        SublyProjectSession session = sessionSupplier.get();
        boolean available = session != null && session.getChapters() != null && !session.getChapters().isEmpty();
        presentingModeToggle.setDisable(!available);
        if (!available && presentingModeToggle.isSelected()) {
            presentingModeToggle.setSelected(false);
            stopPresentingMode();
        }
        updatePresentingButtonText();
    }

    private void startPresentingMode() {
        editingEnabledConsumer.accept(false);
        enablePrevNext();
    }

    private void stopPresentingMode() {
        if (presentingNavigator != null) presentingNavigator.setPresentingMode(false);
        editingEnabledConsumer.accept(true);
        disablePrevNext();
    }

    private void updatePresentingButtonText() {
        if (presentingModeToggle == null) return;
        presentingModeToggle.setText(isPresenting() ? "Presenting" : "Editing");
    }

    private void installPrevNextHandlers() {
        if (prevButton != null) {
            prevButton.setOnAction(e -> {
                if (isPresenting() && presentingNavigator != null && presentingNavigator.isPresentingMode()) {
                    presentingNavigator.previous();
                } else {
                    int i = subtitleTable.getSelectionModel().getSelectedIndex();
                    if (i > 0) subtitleTable.getSelectionModel().select(i - 1);
                }
            });
        }
        if (nextButton != null) {
            nextButton.setOnAction(e -> {
                if (isPresenting() && presentingNavigator != null && presentingNavigator.isPresentingMode()) {
                    presentingNavigator.next();
                } else {
                    int i = subtitleTable.getSelectionModel().getSelectedIndex();
                    if (i < subtitleTable.getItems().size() - 1) {
                        subtitleTable.getSelectionModel().select(i + 1);
                    }
                }
            });
        }
    }

    private void enablePrevNext() {
        if (prevButton != null) prevButton.setDisable(false);
        if (nextButton != null) nextButton.setDisable(false);
    }

    private void disablePrevNext() {
        if (prevButton != null) prevButton.setDisable(true);
        if (nextButton != null) nextButton.setDisable(true);
    }

    // --- Toggle sizing helpers ---
    private void lockToggleSizeWhenReady() {
        if (presentingModeToggle.getScene() == null) {
            presentingModeToggle.sceneProperty().addListener((o, ov, nv) -> {
                if (nv != null) Platform.runLater(this::lockToggleSize);
            });
        } else {
            Platform.runLater(this::lockToggleSize);
        }
    }

    private void lockToggleSize() {
        if (presentingModeToggle == null) return;
        String original = presentingModeToggle.getText();
        double wEditing = measurePrefWidth("Editing");
        double wPresenting = measurePrefWidth("Presenting");
        double width = Math.ceil(Math.max(wEditing, wPresenting));
        double hEditing = measurePrefHeight("Editing");
        double hPresenting = measurePrefHeight("Presenting");
        double height = Math.ceil(Math.max(hEditing, hPresenting));
        presentingModeToggle.setMinWidth(width);
        presentingModeToggle.setPrefWidth(width);
        presentingModeToggle.setMaxWidth(width);
        presentingModeToggle.setMinHeight(height);
        presentingModeToggle.setPrefHeight(height);
        presentingModeToggle.setMaxHeight(height);
        presentingModeToggle.setText(original);
    }

    private double measurePrefWidth(String text) {
        String old = presentingModeToggle.getText();
        presentingModeToggle.setText(text);
        presentingModeToggle.applyCss();
        double w = presentingModeToggle.prefWidth(-1);
        presentingModeToggle.setText(old);
        return w;
    }

    private double measurePrefHeight(String text) {
        String old = presentingModeToggle.getText();
        presentingModeToggle.setText(text);
        presentingModeToggle.applyCss();
        double h = presentingModeToggle.prefHeight(-1);
        presentingModeToggle.setText(old);
        return h;
    }
}