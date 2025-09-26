package com.app.subly.controller.manager;

import com.app.subly.SublyApplication;
import com.app.subly.component.EditHistory;
import com.app.subly.component.SublySettingsDefaults;
import com.app.subly.controller.ControlPanelController;
import com.app.subly.model.Subtitle;
import com.app.subly.persistence.ProjectBuilders;
import com.app.subly.persistence.SublyProjectIO;
import com.app.subly.project.SublyProjectSession;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Now .subly files are zipped archives handled transparently by SublyProjectIO.
 */
public class ProjectFileManager {

    private static final String PROJECT_EXT = ".subly";

    private final MenuItem newMenuItem;
    private final MenuItem openMenuItem;
    private final MenuItem saveMenuItem;
    private final MenuItem saveAsMenuItem;
    private final MenuItem exitMenuItem;
    private final MenuItem undoMenuItem;
    private final MenuItem redoMenuItem;
    private final MenuBar menuBar;

    private final Supplier<SublyProjectSession> sessionSupplier;
    private final Supplier<SublyApplication> appSupplier;
    private final SubtitleTableManager subtitleManager;
    private final java.util.function.Consumer<Boolean> dirtySetter;
    private final Supplier<Boolean> dirtyFlagSupplier;

    private final ControlPanelController controller;

    public ProjectFileManager(MenuItem newMenuItem,
                              MenuItem openMenuItem,
                              MenuItem saveMenuItem,
                              MenuItem saveAsMenuItem,
                              MenuItem exitMenuItem,
                              MenuItem undoMenuItem,
                              MenuItem redoMenuItem,
                              MenuBar menuBar,
                              Supplier<SublyProjectSession> sessionSupplier,
                              Supplier<SublyApplication> appSupplier,
                              SubtitleTableManager subtitleManager,
                              java.util.function.Consumer<Boolean> dirtySetter,
                              Supplier<Boolean> dirtyFlagSupplier,
                              ControlPanelController controller) {
        this.newMenuItem = newMenuItem;
        this.openMenuItem = openMenuItem;
        this.saveMenuItem = saveMenuItem;
        this.saveAsMenuItem = saveAsMenuItem;
        this.exitMenuItem = exitMenuItem;
        this.undoMenuItem = undoMenuItem;
        this.redoMenuItem = redoMenuItem;
        this.menuBar = menuBar;
        this.sessionSupplier = sessionSupplier;
        this.appSupplier = appSupplier;
        this.subtitleManager = subtitleManager;
        this.dirtySetter = dirtySetter;
        this.dirtyFlagSupplier = dirtyFlagSupplier;
        this.controller = controller;
    }

    public void initialize() {
        wireMenuActions();
        setupEditMenu();
        setupAccelerators();
    }

    private void setupAccelerators() {
        newMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveAsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
    }

    private void setupEditMenu() {
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        undoMenuItem.setOnAction(e -> {
            EditHistory h = subtitleManager.getHistory();
            if (h != null) h.undo();
        });
        redoMenuItem.setOnAction(e -> {
            EditHistory h = subtitleManager.getHistory();
            if (h != null) h.redo();
        });
        EditHistory h = subtitleManager.getHistory();
        if (h != null) {
            undoMenuItem.disableProperty().bind(h.canUndoProperty().not());
            redoMenuItem.disableProperty().bind(h.canRedoProperty().not());
        }
    }

    private void wireMenuActions() {
        newMenuItem.setOnAction(e -> newProject());
        openMenuItem.setOnAction(e -> openProject());
        saveMenuItem.setOnAction(e -> saveProject());
        saveAsMenuItem.setOnAction(e -> saveProjectAs());
        exitMenuItem.setOnAction(e -> requestExit());
    }

    public void refreshActions() {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) {
            saveMenuItem.setDisable(true);
            return;
        }
        File f = session.getProjectFile();
        saveMenuItem.setDisable(f == null || !f.exists());
    }

    private void newProject() {
        if (!confirmWithUnsaved("You have unsaved changes.", "Do you want to save your changes before creating a new project?"))
            return;
        var table = subtitleManager.getTable();
        table.setItems(javafx.collections.FXCollections.observableArrayList(new Subtitle(1, "", "")));
        table.getSelectionModel().selectFirst();
        table.scrollTo(0);
        SublyProjectSession session = sessionSupplier.get();
        if (session != null) session.setProjectFile(null);
        SublyApplication app = appSupplier.get();
        if (app != null) app.updateTitle("Untitled");
        dirtySetter.accept(false);
        refreshActions();
    }

    private void openProject() {
        if (!confirmWithUnsaved("You have unsaved changes.", "Do you want to save your changes before opening a project?"))
            return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Subly Project (*.subly)", "*.subly"));
        File selected = chooser.showOpenDialog(new Stage());
        if (selected == null) return;
        try {
            var project = SublyProjectIO.load(selected.toPath(), com.app.subly.model.SublyProjectFile.class);
            SublyProjectSession session = sessionSupplier.get();
            if (session != null && project != null) {
                session.setProjectFile(selected);
                if (project.getSettings() != null) {
                    SublySettingsDefaults.apply(project.getSettings());
                    appSupplier.get().updateSetting(project.getSettings());
                    session.setSettings(project.getSettings());
                }
                session.replaceAllChapters(project.getChapters());
                if (!session.getChapters().isEmpty()) {
                    session.ensureAllChapterIds();
                    session.setSelectedChapterIndex(0);
                    subtitleManager.reloadSubtitles(session.getChapters().getFirst().getSubtitles());
                } else {
                    subtitleManager.reloadSubtitles(java.util.List.of(new Subtitle(1, "", "")));
                }
                if (controller != null) {
                    controller.applySettingsToFormattingTools(project.getSettings());
                    System.out.println("ProjectFileManager: setSession called on controller with session " + session.hashCode());
                    System.out.println("Photo URI in settings: " + project.getSettings().getProjectorImageUri());
                    controller.setSession(session);
                }
                appSupplier.get().updateTitle(project.getFileName());
                session.clearDirty();
                dirtySetter.accept(false);
                refreshActions();
            }
        } catch (IOException ex) {
            showError("Open Project Failed", ex);
        }
    }

    private void saveProject() {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        session.ensureAllChapterIds();
        subtitleManager.syncCurrentChapterToModel();
        File target = session.getProjectFile();
        if (target == null) {
            saveProjectAs();
            return;
        }
        var project = ProjectBuilders.fromUi(target.getName(), session);
        try {
            SublyProjectIO.save(project, target.toPath());
            appSupplier.get().updateTitle(project.getFileName());
            session.clearDirty();
            dirtySetter.accept(false);
            refreshActions();
        } catch (IOException ex) {
            showError("Save Project Failed", ex);
        }
    }

    private void saveProjectAs() {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        subtitleManager.syncCurrentChapterToModel();
        session.ensureAllChapterIds();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Subly Project (*" + PROJECT_EXT + ")", "*" + PROJECT_EXT));
        File initial = session.getProjectFile();
        if (initial != null && initial.getParentFile() != null) {
            chooser.setInitialDirectory(initial.getParentFile());
            chooser.setInitialFileName(stripExt(initial.getName()) + PROJECT_EXT);
        } else {
            chooser.setInitialFileName("Untitled" + PROJECT_EXT);
        }
        File chosen = chooser.showSaveDialog(new Stage());
        if (chosen == null) return;
        if (!chosen.getName().toLowerCase().endsWith(PROJECT_EXT)) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + PROJECT_EXT);
        }
        session.setProjectFile(chosen);
        subtitleManager.syncCurrentChapterToModel();
        var project = ProjectBuilders.fromUi(chosen.getName(), session);
        try {
            SublyProjectIO.save(project, chosen.toPath());
            appSupplier.get().updateTitle(project.getFileName());
            session.clearDirty();
            dirtySetter.accept(false);
            refreshActions();
        } catch (IOException ex) {
            showError("Save Project Failed", ex);
        }
    }

    private void requestExit() {
        if (confirmWithUnsaved("You have unsaved changes.", "Save changes before exiting?")) {
            Platform.exit();
        }
    }

    private boolean confirmWithUnsaved(String header, String content) {
        if (!dirtyFlagSupplier.get()) return true;
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType dontSaveBtn = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);
        ButtonType chosen = alert.showAndWait().orElse(cancelBtn);
        if (chosen == saveBtn) {
            File target = sessionSupplier.get().getProjectFile();
            if (target == null) {
                saveProjectAs();
            } else {
                saveProject();
            }
            File f = sessionSupplier.get().getProjectFile();
            boolean success = (f != null && f.exists());
            if (success) dirtySetter.accept(false);
            return success;
        } else if (chosen == dontSaveBtn) {
            return true;
        }
        return false;
    }

    private void showError(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }

    private String stripExt(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : name;
    }
}