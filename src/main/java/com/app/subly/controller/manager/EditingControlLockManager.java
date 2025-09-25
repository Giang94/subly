package com.app.subly.controller.manager;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.Map;

public class EditingControlLockManager {

    private final Spinner<Integer> fontSizeSpinner;
    private final ColorPicker textColorPicker;

    private final RadioButton bgTransparentRadio;
    private final RadioButton bgColorRadio;
    private final ColorPicker bgColorPicker;
    private final RadioButton bgImageRadio;
    private final TextField imagePathField;
    private final Button chooseImageButton;

    private final TableView<?> subtitleTable;
    private final ChapterManager chapterManager;

    private final MenuItem addChapterMenuItem;
    private final MenuItem renameChapterMenuItem;
    private final MenuItem deleteChapterMenuItem;
    private final MenuItem moveUpMenuItem;
    private final MenuItem moveDownMenuItem;

    private final MenuItem undoMenuItem;
    private final MenuItem redoMenuItem;

    private final Map<MenuItem, EventHandler<ActionEvent>> originalMenuHandlers = new HashMap<>();

    public EditingControlLockManager(
            Spinner<Integer> fontSizeSpinner, ColorPicker textColorPicker,
            RadioButton bgTransparentRadio, RadioButton bgColorRadio, ColorPicker bgColorPicker,
            RadioButton bgImageRadio, TextField imagePathField, Button chooseImageButton,
            TableView<?> subtitleTable, ChapterManager chapterManager,
            MenuItem addChapterMenuItem, MenuItem renameChapterMenuItem, MenuItem deleteChapterMenuItem,
            MenuItem moveUpMenuItem, MenuItem moveDownMenuItem,
            MenuItem undoMenuItem, MenuItem redoMenuItem
    ) {
        this.fontSizeSpinner = fontSizeSpinner;
        this.textColorPicker = textColorPicker;
        this.bgTransparentRadio = bgTransparentRadio;
        this.bgColorRadio = bgColorRadio;
        this.bgColorPicker = bgColorPicker;
        this.bgImageRadio = bgImageRadio;
        this.imagePathField = imagePathField;
        this.chooseImageButton = chooseImageButton;
        this.subtitleTable = subtitleTable;
        this.chapterManager = chapterManager;
        this.addChapterMenuItem = addChapterMenuItem;
        this.renameChapterMenuItem = renameChapterMenuItem;
        this.deleteChapterMenuItem = deleteChapterMenuItem;
        this.moveUpMenuItem = moveUpMenuItem;
        this.moveDownMenuItem = moveDownMenuItem;
        this.undoMenuItem = undoMenuItem;
        this.redoMenuItem = redoMenuItem;
    }

    public void setEditingEnabled(boolean enabled) {
        // Font / text styling
        safeDisable(fontSizeSpinner, !enabled);
        safeDisable(textColorPicker, !enabled);

        // Background controls
        safeDisable(bgTransparentRadio, !enabled);
        safeDisable(bgColorRadio, !enabled);
        safeDisable(bgColorPicker, !enabled);
        safeDisable(bgImageRadio, !enabled);
        safeDisable(imagePathField, !enabled);
        if (imagePathField != null) imagePathField.setEditable(enabled);
        safeDisable(chooseImageButton, !enabled);

        // Subtitle table
        if (subtitleTable != null) {
            subtitleTable.setEditable(enabled);
            subtitleTable.getColumns().forEach(tc -> tc.setEditable(enabled));
        }
        if (chapterManager != null) {
            chapterManager.setChapterStructureLocked(!enabled);
        }

        // Chapters menu
        safeDisable(addChapterMenuItem, !enabled);
        safeDisable(renameChapterMenuItem, !enabled);
        safeDisable(deleteChapterMenuItem, !enabled);
        safeDisable(moveUpMenuItem, !enabled);
        safeDisable(moveDownMenuItem, !enabled);

        // Global edit menu
        safeDisable(undoMenuItem, !enabled);
        safeDisable(redoMenuItem, !enabled);
    }

    private void safeDisable(Control c, boolean disable) {
        if (c == null) return;
        if (c.disableProperty().isBound()) {
            c.setMouseTransparent(disable);
            c.setFocusTraversable(!disable);
            return;
        }
        c.setDisable(disable);
    }

    private void safeDisable(MenuItem mi, boolean disable) {
        if (mi == null) return;
        if (!mi.disableProperty().isBound()) {
            mi.setDisable(disable);
        } else {
            if (disable) {
                originalMenuHandlers.computeIfAbsent(mi, k -> mi.getOnAction());
                mi.setOnAction(e -> e.consume());
            } else {
                EventHandler<ActionEvent> h = originalMenuHandlers.remove(mi);
                if (h != null) mi.setOnAction(h);
            }
        }
        if (disable && !originalMenuHandlers.containsKey(mi)) {
            originalMenuHandlers.put(mi, mi.getOnAction());
            mi.setOnAction(e -> e.consume());
        } else if (!disable && originalMenuHandlers.containsKey(mi)) {
            EventHandler<ActionEvent> h = originalMenuHandlers.remove(mi);
            mi.setOnAction(h);
        }
    }
}