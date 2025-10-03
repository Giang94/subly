package com.app.subly.controller.manager;

import com.app.subly.model.Chapter;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.function.Supplier;

public class ChapterManager {

    private static final String UNIFIED_SELECTION_STYLE =
            "-fx-selection-bar: #b4dcff; -fx-selection-bar-non-focused: #b4dcff;";


    private final ListView<Chapter> chapterListView;
    private final Label chapterCountLabel;
    private final MenuItem addChapterMenuItem;
    private final MenuItem renameChapterMenuItem;
    private final MenuItem deleteChapterMenuItem;
    private final MenuItem moveUpMenuItem;
    private final MenuItem moveDownMenuItem;

    private final Supplier<SublyProjectSession> sessionSupplier;
    private final SubtitleTableManager subtitleManager;
    private final Runnable markDirty;

    private boolean lockFiltersInstalled = false;
    private boolean chapterStructureLocked = false;

    private final javafx.event.EventHandler<ContextMenuEvent> lockContextFilter =
            e -> {
                if (chapterStructureLocked) e.consume();
            };
    private final javafx.event.EventHandler<KeyEvent> lockKeyFilter =
            e -> {
                if (!chapterStructureLocked) return;
                KeyCode c = e.getCode();
                if (c == KeyCode.DELETE || c == KeyCode.F2
                        || (c == KeyCode.N && e.isControlDown())
                        || (c == KeyCode.R && e.isControlDown())
                        || (c == KeyCode.UP && e.isAltDown())
                        || (c == KeyCode.DOWN && e.isAltDown())) {
                    e.consume();
                }
            };
    private final javafx.event.EventHandler<MouseEvent> lockMouseFilter =
            e -> {
                if (!chapterStructureLocked) return;
                if (e.getClickCount() > 1) e.consume();
            };

    public ChapterManager(ListView<Chapter> chapterListView,
                          Label chapterCountLabel,
                          MenuItem addChapterMenuItem,
                          MenuItem renameChapterMenuItem,
                          MenuItem deleteChapterMenuItem,
                          MenuItem moveUpMenuItem,
                          MenuItem moveDownMenuItem,
                          Supplier<SublyProjectSession> sessionSupplier,
                          SubtitleTableManager subtitleManager,
                          Runnable markDirty) {
        this.chapterListView = chapterListView;
        this.chapterCountLabel = chapterCountLabel;
        this.addChapterMenuItem = addChapterMenuItem;
        this.renameChapterMenuItem = renameChapterMenuItem;
        this.deleteChapterMenuItem = deleteChapterMenuItem;
        this.moveUpMenuItem = moveUpMenuItem;
        this.moveDownMenuItem = moveDownMenuItem;
        this.sessionSupplier = sessionSupplier;
        this.subtitleManager = subtitleManager;
        this.markDirty = markDirty;
    }

    public void initialize() {
        setupContextActions();
        applyUnifiedSelectionColors(chapterListView);
    }

    private void applyUnifiedSelectionColors(javafx.scene.control.Control control) {
        if (control == null) return;
        if (control.getProperties().get("unifiedSelectionColorsInstalled") == null) {
            String existing = control.getStyle();
            control.setStyle((existing == null || existing.isBlank() ? "" : existing + ";") + UNIFIED_SELECTION_STYLE);
            control.getProperties().put("unifiedSelectionColorsInstalled", Boolean.TRUE);
        }
    }

    public void onSessionSet() {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        chapterListView.setItems(session.getChapters());
        installInlineRename();
        session.ensureAtLeastOneChapter();
        if (chapterListView.getSelectionModel().getSelectedIndex() < 0 && !chapterListView.getItems().isEmpty()) {
            chapterListView.getSelectionModel().select(0);
            session.setSelectedChapterIndex(0);
            Chapter ch = session.getSelectedChapter();
            if (ch != null && !isPlaceholder(ch)) {
                subtitleManager.reloadSubtitles(ch.getSubtitles());
            }
        }
        chapterListView.getSelectionModel().selectedIndexProperty()
                .addListener((o, ov, nv) -> handleChapterChange(ov, nv));
        session.getChapters().addListener((ListChangeListener<Chapter>) c -> updateContextMenuState());
        updateContextMenuState();
        applyLockFilters();
    }

    public void setChapterStructureLocked(boolean locked) {
        if (this.chapterStructureLocked == locked) return;
        this.chapterStructureLocked = locked;
        applyLockState();
    }

    public boolean isChapterStructureLocked() {
        return chapterStructureLocked;
    }

    private void applyLockState() {
        applyLockFilters();
        updateContextMenuState();
        if (chapterStructureLocked) chapterListView.edit(-1);
    }

    private void applyLockFilters() {
        if (chapterListView == null || lockFiltersInstalled) return;
        chapterListView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, lockContextFilter);
        chapterListView.addEventFilter(KeyEvent.KEY_PRESSED, lockKeyFilter);
        chapterListView.addEventFilter(MouseEvent.MOUSE_CLICKED, lockMouseFilter);
        lockFiltersInstalled = true;
    }

    private void handleChapterChange(Number prev, Number next) {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        if (prev != null && prev.intValue() >= 0) {
            Chapter old = session.getSelectedChapter();
            if (old != null && !isPlaceholder(old)) {
                subtitleManager.syncCurrentChapterToModel();
            }
        }
        int idx = next == null ? -1 : next.intValue();
        session.setSelectedChapterIndex(idx);
        Chapter ch = session.getSelectedChapter();
        if (ch != null && !isPlaceholder(ch)) {
            if (ch.getSubtitles().isEmpty()) ch.getSubtitles().add(new Subtitle(1, "", ""));
            subtitleManager.reloadSubtitles(ch.getSubtitles());
        } else {
            subtitleManager.reloadSubtitles(java.util.List.of(new Subtitle(1, "", "")));
        }
        updateContextMenuState();
        updateCountLabel();
    }

    private void updateCountLabel() {
        if (chapterCountLabel == null) return;
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        long real = session.getChapters().stream().filter(c -> !isPlaceholder(c)).count();
        chapterCountLabel.setText("Chapters: " + real);
    }

    private void setupContextActions() {
        if (addChapterMenuItem != null) addChapterMenuItem.setOnAction(e -> addChapter());
        if (renameChapterMenuItem != null) renameChapterMenuItem.setOnAction(e -> renameChapter());
        if (deleteChapterMenuItem != null) deleteChapterMenuItem.setOnAction(e -> deleteChapter());
        if (moveUpMenuItem != null) moveUpMenuItem.setOnAction(e -> moveChapterUp());
        if (moveDownMenuItem != null) moveDownMenuItem.setOnAction(e -> moveChapterDown());
    }

    private void addChapter() {
        if (chapterStructureLocked) return;
        SublyProjectSession s = sessionSupplier.get();
        if (s == null) return;
        Chapter c = s.addChapter();
        chapterListView.getSelectionModel().select(c);
        chapterListView.scrollTo(c);
        chapterListView.refresh();
        updateContextMenuState();
        markDirty.run();
    }

    private void renameChapter() {
        if (chapterStructureLocked) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) chapterListView.edit(idx);
    }

    private void deleteChapter() {
        // Prevent deletion if chapter structure is locked
        if (chapterStructureLocked) return;

        // Get the current project session
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;

        // Get the selected chapter index
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;

        // Get the selected chapter object
        Chapter ch = session.getSelectedChapter();
        // Abort if no chapter is selected or if it's a placeholder
        if (ch == null || isPlaceholder(ch)) return;

        // Sync any subtitle edits to the model before deletion
        subtitleManager.syncCurrentChapterToModel();

        // Remove the selected chapter
        session.getChapters().remove(idx);

        // Count remaining real chapters
        long realCount = session.getChapters().stream().filter(c -> !isPlaceholder(c)).count();

        // If no real chapters remain, create a default chapter
        if (realCount == 0) {
            Chapter fresh = new Chapter();
            fresh.setTitle("Chapter 1");
            fresh.getSubtitles().add(new Subtitle(1, "", ""));
            session.getChapters().add(0, fresh);
            idx = 0;
        } else {
            // Move selection to the previous chapter (above)
            idx = Math.max(0, idx - 1);
        }

        // Ensure a placeholder chapter exists
        session.ensurePlaceholderChapter();

        // Select the new chapter
        chapterListView.getSelectionModel().select(idx);
        session.setSelectedChapterIndex(idx);

        // Reload subtitles for the newly selected chapter if it's not a placeholder
        Chapter newCh = session.getSelectedChapter();
        if (newCh != null && !isPlaceholder(newCh)) {
            if (newCh.getSubtitles().isEmpty()) newCh.getSubtitles().add(new Subtitle(1, "", ""));
            subtitleManager.reloadSubtitles(newCh.getSubtitles());
        }

        // Refresh UI and update state
        chapterListView.refresh();
        updateContextMenuState();
        session.touch();
        markDirty.run();
    }

    private void moveChapterUp() {
        if (chapterStructureLocked) return;
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx <= 0) return;
        Chapter ch = session.getSelectedChapter();
        if (ch == null || isPlaceholder(ch)) return;
        var list = session.getChapters();
        list.remove(idx);
        list.add(idx - 1, ch);
        chapterListView.getSelectionModel().select(idx - 1);
        session.setSelectedChapterIndex(idx - 1);
        session.touch();
        chapterListView.refresh();
        updateContextMenuState();
        markDirty.run();
    }

    private void moveChapterDown() {
        if (chapterStructureLocked) return;
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Chapter ch = session.getSelectedChapter();
        if (ch == null || isPlaceholder(ch)) return;
        var list = session.getChapters();
        if (idx >= list.size() - 1) return;
        Chapter next = list.get(idx + 1);
        if (isPlaceholder(next)) return;
        list.remove(idx);
        list.add(idx + 1, ch);
        chapterListView.getSelectionModel().select(idx + 1);
        session.setSelectedChapterIndex(idx + 1);
        session.touch();
        chapterListView.refresh();
        updateContextMenuState();
        markDirty.run();
    }

    private void updateContextMenuState() {
        if (chapterStructureLocked) {
            if (addChapterMenuItem != null) addChapterMenuItem.setDisable(true);
            if (renameChapterMenuItem != null) renameChapterMenuItem.setDisable(true);
            if (deleteChapterMenuItem != null) deleteChapterMenuItem.setDisable(true);
            if (moveUpMenuItem != null) moveUpMenuItem.setDisable(true);
            if (moveDownMenuItem != null) moveDownMenuItem.setDisable(true);
            return;
        }

        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        Chapter ch = session.getSelectedChapter();
        boolean hasSelection = idx >= 0 && ch != null;
        boolean placeholder = hasSelection && isPlaceholder(ch);

        if (addChapterMenuItem != null) addChapterMenuItem.setDisable(false);
        if (renameChapterMenuItem != null) renameChapterMenuItem.setDisable(!hasSelection || placeholder);
        if (deleteChapterMenuItem != null) deleteChapterMenuItem.setDisable(!hasSelection || placeholder);
        if (moveUpMenuItem != null) moveUpMenuItem.setDisable(!hasSelection || placeholder || idx <= 0);
        if (moveDownMenuItem != null) {
            boolean disable = true;
            if (hasSelection && !placeholder) {
                if (idx < session.getChapters().size() - 1) {
                    Chapter next = session.getChapters().get(idx + 1);
                    disable = isPlaceholder(next);
                }
            }
            moveDownMenuItem.setDisable(disable);
        }
    }

    private boolean isPlaceholder(Chapter ch) {
        if (ch == null) return false;
        int idx = chapterListView.getItems().indexOf(ch);
        return (ch.getTitle() == null || ch.getTitle().isBlank()) &&
                idx == chapterListView.getItems().size() - 1;
    }

    private void installInlineRename() {
        chapterListView.setEditable(true);
        PseudoClass placeholderPc = PseudoClass.getPseudoClass("placeholder-chapter");

        chapterListView.setCellFactory(lv -> new ListCell<>() {
            private TextField editor;
            private String oldTitle;

            {
                addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (chapterStructureLocked) return;
                    if (isEmpty() || getItem() == null) return;

                    // Always ensure selection on any click
                    ListView<Chapter> list = getListView();
                    if (list != null) {
                        list.getSelectionModel().clearAndSelect(getIndex());
                        list.getFocusModel().focus(getIndex());
                    }

                    if (e.getClickCount() == 2 && !isEditing()) {
                        startEdit();
                    }
                    e.consume();
                });

                this.focusedProperty().addListener((o, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        Chapter ch = sessionSupplier.get().getSelectedChapter();
                        if (ch != null && (ch.getTitle() == null || ch.getTitle().isBlank())) {
                            ch.setTitle(sessionSupplier.get().computeNextChapterTitle());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Chapter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    pseudoClassStateChanged(placeholderPc, false);
                    return;
                }
                boolean placeholder = isPlaceholder(item);
                pseudoClassStateChanged(placeholderPc, placeholder);
                if (isEditing()) {
                    setText(null);
                    setGraphic(editor);
                } else if (placeholder) {
                    setText(null);
                    setGraphic(buildPlaceholderGraphic());
                } else {
                    setText(item.getTitle());
                    setGraphic(null);
                }
            }

            private javafx.scene.Node buildPlaceholderGraphic() {
                Label plus = new Label("+");
                plus.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent; -fx-font-size: 14;");
                Label hint = new Label(" Add chapter");
                hint.setStyle("-fx-text-fill: -fx-text-inner-color; -fx-opacity: 0.65; -fx-font-style: italic;");
                HBox box = new HBox(plus, hint);
                box.setSpacing(2);
                box.setPadding(new Insets(2, 4, 2, 4));
                Tooltip.install(box, new Tooltip("Double click to create a new chapter"));
                return box;
            }

            @Override
            public void startEdit() {
                if (chapterStructureLocked) return;
                if (getItem() == null) return;
                super.startEdit();
                if (editor == null) createEditor();
                oldTitle = getItem().getTitle();
                editor.setText(oldTitle == null ? "" : oldTitle);
                setText(null);
                setGraphic(editor);
                editor.requestFocus();
                editor.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                Chapter it = getItem();
                if (it == null) {
                    setText(null);
                    setGraphic(null);
                } else if (isPlaceholder(it)) {
                    setText(null);
                    setGraphic(buildPlaceholderGraphic());
                } else {
                    setText(it.getTitle());
                    setGraphic(null);
                }
            }

            @Override
            public void commitEdit(Chapter newValue) {
                super.commitEdit(newValue);
                if (isPlaceholder(newValue)) {
                    setText(null);
                    setGraphic(buildPlaceholderGraphic());
                } else {
                    setText(newValue.getTitle());
                    setGraphic(null);
                }
            }

            private void createEditor() {
                editor = new TextField();
                editor.setOnAction(e -> commitOrReclassify());
                editor.focusedProperty().addListener((o, was, now) -> {
                    if (!now) commitOrReclassify();
                });
                editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        e.consume();
                    }
                });
            }

            private void commitOrReclassify() {
                Chapter ch = getItem();
                if (ch == null) return;
                SublyProjectSession session = sessionSupplier.get();
                String txt = editor.getText() == null ? "" : editor.getText().trim();
                boolean wasPlaceholder = isPlaceholder(ch);

                if (txt.isBlank()) {
                    if (!wasPlaceholder) ch.setTitle(oldTitle);
                    cancelEdit();
                    return;
                }

                if (!txt.equals(ch.getTitle())) {
                    ch.setTitle(txt);
                    if (session != null) session.touch();
                }
                if (wasPlaceholder) {
                    if (ch.getSubtitles().isEmpty()) ch.getSubtitles().add(new Subtitle(1, "", ""));
                    if (session != null) session.ensurePlaceholderChapter();
                }
                commitEdit(ch);
                chapterListView.refresh();
                updateContextMenuState();
                markDirty.run();
            }
        });
    }
}