package com.app.subly.controller.manager;

import com.app.subly.model.Chapter;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.function.Supplier;

public class ChapterManager {

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
        chapterListView.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> handleChapterChange(ov, nv));
        session.getChapters().addListener((ListChangeListener<Chapter>) c -> updateContextMenuState());
        updateContextMenuState();
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
            if (ch.getSubtitles().isEmpty()) {
                ch.getSubtitles().add(new Subtitle(1, "", ""));
            }
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
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) chapterListView.edit(idx);
    }

    private void deleteChapter() {
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Chapter ch = session.getSelectedChapter();
        if (ch == null || isPlaceholder(ch)) return;

        subtitleManager.syncCurrentChapterToModel();
        session.getChapters().remove(idx);
        long realCount = session.getChapters().stream().filter(c -> !isPlaceholder(c)).count();
        if (realCount == 0) {
            Chapter fresh = new Chapter();
            fresh.setTitle("Chapter 1");
            fresh.getSubtitles().add(new Subtitle(1, "", ""));
            session.getChapters().add(0, fresh);
            idx = 0;
        }
        session.ensurePlaceholderChapter();
        int newIndex = Math.min(idx, session.getChapters().size() - 1);
        if (newIndex >= 0) {
            chapterListView.getSelectionModel().select(newIndex);
            session.setSelectedChapterIndex(newIndex);
        } else {
            session.setSelectedChapterIndex(-1);
        }
        Chapter newCh = session.getSelectedChapter();
        if (newCh != null && !isPlaceholder(newCh)) {
            if (newCh.getSubtitles().isEmpty()) newCh.getSubtitles().add(new Subtitle(1, "", ""));
            subtitleManager.reloadSubtitles(newCh.getSubtitles());
        }
        chapterListView.refresh();
        updateContextMenuState();
        session.touch();
        markDirty.run();
    }

    private void moveChapterUp() {
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
        SublyProjectSession session = sessionSupplier.get();
        if (session == null) return;
        int idx = chapterListView.getSelectionModel().getSelectedIndex();
        Chapter ch = session.getSelectedChapter();
        boolean hasSelection = idx >= 0 && ch != null;
        boolean placeholder = hasSelection && isPlaceholder(ch);

        if (addChapterMenuItem != null) addChapterMenuItem.setDisable(false);
        if (renameChapterMenuItem != null) renameChapterMenuItem.setDisable(!hasSelection);
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
                addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                    Chapter it = getItem();
                    if (e.getClickCount() == 1 && it != null && isPlaceholder(it) && !isEditing()) {
                        startEdit();
                        e.consume();
                    } else if (e.getClickCount() == 2 && it != null && !isEditing()) {
                        startEdit();
                        e.consume();
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
                    return;
                }
                if (placeholder) {
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
                Tooltip.install(box, new Tooltip("Click to create a new chapter"));
                return box;
            }

            @Override
            public void startEdit() {
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