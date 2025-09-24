package com.app.subly.controller.manager;

import com.app.subly.component.EditHistory;
import com.app.subly.component.MultilineTableCell;
import com.app.subly.component.PasteManager;
import com.app.subly.component.RowIndexer;
import com.app.subly.component.TrailingBlankRowPolicy;
import com.app.subly.model.Chapter;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.util.*;

public class SubtitleTableManager {

    private final TableView<Subtitle> table;
    private final TableColumn<Subtitle, Integer> indexColumn;
    private final TableColumn<Subtitle, String> primaryColumn;
    private final TableColumn<Subtitle, String> secondaryColumn;
    private final Label currentSubtitleLabel;
    private final Button prevButton;
    private final Button nextButton;
    private final Runnable markDirty;
    private final java.util.function.Supplier<com.app.subly.component.Projector> projectorSupplier;
    private final java.util.function.Supplier<SublyProjectSession> sessionSupplier;

    private final TrailingBlankRowPolicy trailingBlank = new TrailingBlankRowPolicy();
    private EditHistory history;
    private PasteManager pasteManager;

    public SubtitleTableManager(TableView<Subtitle> table,
                         TableColumn<Subtitle, Integer> indexColumn,
                         TableColumn<Subtitle, String> primaryColumn,
                         TableColumn<Subtitle, String> secondaryColumn,
                         Label currentSubtitleLabel,
                         Button prevButton,
                         Button nextButton,
                         Runnable markDirty,
                         java.util.function.Supplier<com.app.subly.component.Projector> projectorSupplier,
                         java.util.function.Supplier<SublyProjectSession> sessionSupplier) {
        this.table = table;
        this.indexColumn = indexColumn;
        this.primaryColumn = primaryColumn;
        this.secondaryColumn = secondaryColumn;
        this.currentSubtitleLabel = currentSubtitleLabel;
        this.prevButton = prevButton;
        this.nextButton = nextButton;
        this.markDirty = markDirty;
        this.projectorSupplier = projectorSupplier;
        this.sessionSupplier = sessionSupplier;
    }

    public void initialize() {
        setupTableStructure();
        setupNavigation();
        setupHistoryAndPaste();
        installEditingHandlers();
        seedDemo();
    }

    private void seedDemo() {
        ObservableList<Subtitle> data = FXCollections.observableArrayList(
                new Subtitle(1, "This line will be showed on screen", "This line is for translation")
        );
        table.setItems(data);
        trailingBlank.ensureTrailingBlankRow(table);
        if (!data.isEmpty()) {
            table.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(data.getFirst().getPrimaryText());
        }
    }

    private void setupTableStructure() {
        table.setEditable(true);
        table.getColumns().setAll(indexColumn, primaryColumn, secondaryColumn);
        table.setTableMenuButtonVisible(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        indexColumn.setText("#");
        indexColumn.setMinWidth(30);
        indexColumn.setPrefWidth(30);
        indexColumn.setMaxWidth(30);
        indexColumn.setResizable(false);

        primaryColumn.setResizable(true);
        secondaryColumn.setResizable(true);

        table.sceneProperty().addListener((o, ov, nv) -> {
            if (nv != null) Platform.runLater(this::applyInitialWidths);
        });
        table.widthProperty().addListener((o, ov, nv) -> applyInitialWidths());

        indexColumn.setCellValueFactory(cd -> {
            Integer id = cd.getValue().getId();
            int value;
            if (id == null) {
                int pos = table.getItems().indexOf(cd.getValue());
                value = pos >= 0 ? pos + 1 : 0;
            } else value = id;
            return new javafx.beans.property.SimpleIntegerProperty(value).asObject();
        });
        primaryColumn.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrimaryText()));
        secondaryColumn.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getSecondaryText()));

        primaryColumn.setCellFactory(col -> new MultilineTableCell());
        secondaryColumn.setCellFactory(col -> new MultilineTableCell());
    }

    private void applyInitialWidths() {
        double total = table.getWidth();
        if (total <= 0) return;
        double indexW = indexColumn.getWidth() > 0 ? indexColumn.getWidth() : indexColumn.getPrefWidth();
        double remaining = Math.max(0, total - indexW);
        double half = remaining / 2.0;
        primaryColumn.setPrefWidth(half);
        secondaryColumn.setPrefWidth(half);
        primaryColumn.setMinWidth(80);
        secondaryColumn.setMinWidth(80);
    }

    private void setupNavigation() {
        prevButton.setOnAction(e -> {
            int i = table.getSelectionModel().getSelectedIndex();
            if (i > 0) {
                table.getSelectionModel().selectPrevious();
                table.scrollTo(table.getSelectionModel().getSelectedIndex());
            }
        });
        nextButton.setOnAction(e -> {
            int i = table.getSelectionModel().getSelectedIndex();
            if (i < table.getItems().size() - 1) {
                table.getSelectionModel().selectNext();
                table.scrollTo(table.getSelectionModel().getSelectedIndex());
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            String text = n.getPrimaryText() == null ? "" : n.getPrimaryText().replace("\\n", "\n");
            currentSubtitleLabel.setText(text);
            var proj = projectorSupplier.get();
            if (proj != null) proj.setText(text);
        });
    }

    private void setupHistoryAndPaste() {
        history = new EditHistory(() -> {
            trailingBlank.ensureTrailingBlankRow(table);
            RowIndexer.renumber(table.getItems());
            table.refresh();
        }, markDirty);

        pasteManager = new PasteManager(
                table,
                primaryColumn,
                secondaryColumn,
                indexColumn,
                () -> new Subtitle(table.getItems().size() + 1, "", ""),
                () -> trailingBlank.ensureTrailingBlankRow(table),
                markDirty,
                history
        );
        pasteManager.install();
    }

    private void installEditingHandlers() {
        table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                handleTabAddRow(e);
            } else if (e.getCode() == KeyCode.DELETE && !e.isControlDown() && !e.isAltDown()) {
                handleDeleteRows(e);
            }
        });

        primaryColumn.setOnEditCommit(ev -> {
            int rowIndex = ev.getTablePosition().getRow();
            Subtitle row = ev.getRowValue();
            String oldV = ev.getOldValue();
            String newV = ev.getNewValue();
            row.setPrimaryText(newV);
            Subtitle sel = table.getSelectionModel().getSelectedItem();
            if (sel == row) {
                String txt = newV == null ? "" : newV.replace("\\n", "\n");
                currentSubtitleLabel.setText(txt);
                var proj = projectorSupplier.get();
                if (proj != null) proj.setText(txt);
            }
            if (history != null) {
                history.push(EditHistory.of(
                        () -> safeRow(rowIndex).setPrimaryText(oldV),
                        () -> safeRow(rowIndex).setPrimaryText(newV)
                ));
            }
            trailingBlank.ensureTrailingBlankRow(table);
            markDirty.run();
        });

        secondaryColumn.setOnEditCommit(ev -> {
            int rowIndex = ev.getTablePosition().getRow();
            String oldV = ev.getOldValue();
            String newV = ev.getNewValue();
            ev.getRowValue().setSecondaryText(newV);
            if (history != null) {
                history.push(EditHistory.of(
                        () -> safeRow(rowIndex).setSecondaryText(oldV),
                        () -> safeRow(rowIndex).setSecondaryText(newV)
                ));
            }
            trailingBlank.ensureTrailingBlankRow(table);
            markDirty.run();
        });

        primaryColumn.setEditable(true);
        secondaryColumn.setEditable(true);
    }

    private Subtitle safeRow(int idx) {
        if (idx >= 0 && idx < table.getItems().size()) return table.getItems().get(idx);
        return new Subtitle(idx + 1, "", "");
    }

    private void handleTabAddRow(KeyEvent e) {
        int row = table.getSelectionModel().getSelectedIndex();
        int last = table.getItems().size() - 1;
        if (row == last && !trailingBlank.isBlankRow(table.getItems().get(last))) {
            Subtitle s = new Subtitle(last + 2, "", "");
            table.getItems().add(s);
            if (history != null) {
                history.push(EditHistory.of(
                        () -> {
                            if (!table.getItems().isEmpty())
                                table.getItems().remove(table.getItems().size() - 1);
                        },
                        () -> table.getItems().add(new Subtitle(table.getItems().size() + 1, "", ""))
                ));
            }
            table.getSelectionModel().select(s);
            table.getFocusModel().focus(last + 1, primaryColumn);
            table.edit(last + 1, primaryColumn);
            e.consume();
            markDirty.run();
        }
    }

    private void handleDeleteRows(KeyEvent e) {
        var items = table.getItems();
        var indices = new ArrayList<>(table.getSelectionModel().getSelectedIndices());
        if (indices.isEmpty()) return;
        indices.sort(Comparator.naturalOrder());

        List<Integer> keptIdx = new ArrayList<>();
        List<Subtitle> keptRows = new ArrayList<>();
        for (int idx : indices) {
            if (idx >= 0 && idx < items.size()) {
                Subtitle s = items.get(idx);
                if (trailingBlank.isBlankRow(s) && idx == items.size() - 1) continue;
                keptIdx.add(idx);
                keptRows.add(s);
            }
        }
        if (keptIdx.isEmpty()) return;

        for (int i = keptIdx.size() - 1; i >= 0; i--) {
            int idx = keptIdx.get(i);
            if (idx >= 0 && idx < items.size()) items.remove(idx);
        }
        trailingBlank.ensureTrailingBlankRow(table);
        RowIndexer.renumber(items);
        table.refresh();
        markDirty.run();

        if (history != null) {
            EditHistory.CompoundEdit ce = new EditHistory.CompoundEdit();
            ce.add(EditHistory.of(
                    () -> {
                        for (int i = 0; i < keptIdx.size(); i++) {
                            int at = Math.min(keptIdx.get(i), table.getItems().size());
                            table.getItems().add(at, keptRows.get(i));
                        }
                    },
                    () -> {
                        for (int i = keptIdx.size() - 1; i >= 0; i--) {
                            int at = keptIdx.get(i);
                            if (at >= 0 && at < table.getItems().size()) {
                                table.getItems().remove(at);
                            }
                        }
                    }
            ));
            history.push(ce);
        }
        e.consume();
    }

    public void onSessionSet() {
        var sess = sessionSupplier.get();
        if (sess == null) return;
        installPlaceholderPromotion();
    }

    private void installPlaceholderPromotion() {
        table.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> promotePlaceholder());
        table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != KeyCode.ESCAPE && e.getCode() != KeyCode.TAB) promotePlaceholder();
        });
    }

    private void promotePlaceholder() {
        var sess = sessionSupplier.get();
        if (sess == null) return;
        Chapter ch = sess.getSelectedChapter();
        if (ch != null && sess.promotePlaceholderForSubtitleEdit()) {
            // chapter list will refresh via ChapterManager
        }
    }

    void reloadSubtitles(java.util.List<Subtitle> subtitles) {
        var data = FXCollections.observableArrayList(subtitles);
        table.setItems(data);
        if (!data.isEmpty()) {
            table.getSelectionModel().selectFirst();
            currentSubtitleLabel.setText(data.getFirst().getPrimaryText().replace("\\n", "\n"));
            var proj = projectorSupplier.get();
            if (proj != null) proj.setText(data.getFirst().getPrimaryText().replace("\\n", "\n"));
        }
        trailingBlank.ensureTrailingBlankRow(table);
    }

    void syncCurrentChapterToModel() {
        var sess = sessionSupplier.get();
        if (sess == null) return;
        sess.syncCurrentChapterFromTable(table, trailingBlank);
    }

    EditHistory getHistory() {
        return history;
    }

    TableView<Subtitle> getTable() {
        return table;
    }

    TrailingBlankRowPolicy getTrailingBlankPolicy() {
        return trailingBlank;
    }
}