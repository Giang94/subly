// java
package com.app.subly.component;

import com.app.subly.model.Subtitle;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles TSV paste into a TableView<Subtitle> and records a single compound edit into EditHistory.
 */
public class PasteManager {

    private final TableView<Subtitle> table;
    private final TableColumn<Subtitle, String> primaryCol;
    private final TableColumn<Subtitle, String> secondaryCol;
    private final TableColumn<Subtitle, ?> indexCol;
    private final Supplier<Subtitle> newRowSupplier;
    private final Runnable ensureTrailingBlankRow;
    private final Runnable markDirty;
    private final EditHistory history;

    private final Map<TableColumn<Subtitle, ?>, BiConsumer<Subtitle, String>> writers = new LinkedHashMap<>();
    private final Map<TableColumn<Subtitle, ?>, Function<Subtitle, String>> readers = new LinkedHashMap<>();

    public PasteManager(TableView<Subtitle> table,
                        TableColumn<Subtitle, String> primaryCol,
                        TableColumn<Subtitle, String> secondaryCol,
                        TableColumn<Subtitle, ?> indexCol,
                        Supplier<Subtitle> newRowSupplier,
                        Runnable ensureTrailingBlankRow,
                        Runnable markDirty,
                        EditHistory history) {
        this.table = table;
        this.primaryCol = primaryCol;
        this.secondaryCol = secondaryCol;
        this.indexCol = indexCol;
        this.newRowSupplier = newRowSupplier;
        this.ensureTrailingBlankRow = ensureTrailingBlankRow != null ? ensureTrailingBlankRow : () -> {
        };
        this.markDirty = markDirty != null ? markDirty : () -> {
        };
        this.history = history;

        writers.put(primaryCol, Subtitle::setPrimaryText);
        writers.put(secondaryCol, Subtitle::setSecondaryText);
        if (indexCol != null) {
            writers.put(indexCol, (row, v) -> {
                try {
                    row.setId(Integer.parseInt(v.trim()));
                } catch (Exception ignored) {
                }
            });
        }
        readers.put(primaryCol, Subtitle::getPrimaryText);
        readers.put(secondaryCol, Subtitle::getSecondaryText);
        if (indexCol != null) readers.put(indexCol, s -> String.valueOf(s.getId()));
    }

    public void install() {
        table.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if ((e.isControlDown() && e.getCode() == KeyCode.V) || (e.isShiftDown() && e.getCode() == KeyCode.INSERT)) {
                e.consume();
                pasteFromClipboard();
            }
        });
    }

    private void pasteFromClipboard() {
        if (writers.isEmpty()) return;

        Clipboard cb = Clipboard.getSystemClipboard();
        if (!cb.hasString()) return;

        String raw = cb.getString();
        if (raw == null || raw.isEmpty()) return;

        String[] lines = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.strip().isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            boolean allBlank = true;
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    allBlank = false;
                    break;
                }
            }
            if (allBlank) continue;
            rows.add(parts);
        }
        if (rows.isEmpty()) return;

        // Top-left selected cell
        TablePosition<Subtitle, ?> anchor = null;
        int bestRow = Integer.MAX_VALUE, bestCol = Integer.MAX_VALUE;
        for (TablePosition<?, ?> p : table.getSelectionModel().getSelectedCells()) {
            int row = p.getRow();
            int col = table.getVisibleLeafIndex((TableColumn<Subtitle, ?>) p.getTableColumn());
            if (row < bestRow || (row == bestRow && col < bestCol)) {
                bestRow = row;
                bestCol = col;
                // noinspection unchecked
                anchor = (TablePosition<Subtitle, ?>) p;
            }
        }
        if (anchor == null) {
            if (table.getColumns().isEmpty()) return;
            anchor = new TablePosition<>(table, 0, (TableColumn<Subtitle, ?>) table.getColumns().get(0));
            bestRow = 0;
            bestCol = table.getVisibleLeafIndex(anchor.getTableColumn());
        }

        int startRow = Math.max(0, bestRow);
        int startCol = Math.max(0, bestCol);

        List<TableColumn<?, ?>> leafs = new ArrayList<>();
        collectLeafColumns(table.getColumns(), leafs);
        if (leafs.isEmpty()) return;

        ObservableList<Subtitle> items = table.getItems();
        int initialSize = items.size();

        EditHistory.CompoundEdit compound = new EditHistory.CompoundEdit();

        int clipboardCols = rows.stream().mapToInt(arr -> arr.length).max().orElse(1);
        boolean singleColMode = (clipboardCols == 1);

        for (int r = 0; r < rows.size(); r++) {
            int targetRow = startRow + r;
            while (targetRow >= items.size()) {
                items.add(newRowSupplier.get());
            }
            Subtitle rowItem = items.get(targetRow);
            String[] values = rows.get(r);
            int colsToPaste = singleColMode ? 1 : Math.min(clipboardCols, leafs.size() - startCol);

            for (int c = 0; c < colsToPaste; c++) {
                int targetColIndex = singleColMode ? startCol : (startCol + c);
                if (targetColIndex < 0 || targetColIndex >= leafs.size()) break;

                @SuppressWarnings("unchecked")
                TableColumn<Subtitle, ?> col = (TableColumn<Subtitle, ?>) leafs.get(targetColIndex);
                BiConsumer<Subtitle, String> writer = writers.get(col);
                Function<Subtitle, String> reader = readers.get(col);
                if (writer == null || reader == null) continue;

                String newValue = singleColMode ? values[0] : (c < values.length ? values[c] : "");
                String oldValue = reader.apply(rowItem);
                if (Objects.equals(oldValue, newValue)) continue;

                final int rowIndex = targetRow;
                writer.accept(rowItem, newValue); // apply now
                compound.add(EditHistory.of(
                        () -> {
                            if (rowIndex < table.getItems().size())
                                writer.accept(table.getItems().get(rowIndex), oldValue);
                        },
                        () -> {
                            if (rowIndex < table.getItems().size())
                                writer.accept(table.getItems().get(rowIndex), newValue);
                        }
                ));
            }
        }

        int rowsAdded = Math.max(0, items.size() - initialSize);
        if (rowsAdded > 0) {
            compound.getClass(); // keep reference
            compound = prependRowsAdded(compound, items, rowsAdded);
        }

        ensureTrailingBlankRow.run();
        RowIndexer.renumber(items);
        table.refresh();
        markDirty.run();

        if (!compound.isEmpty() && history != null) {
            history.push(compound);
        }
    }

    private EditHistory.CompoundEdit prependRowsAdded(EditHistory.CompoundEdit compound, ObservableList<Subtitle> items, int count) {
        EditHistory.Edit rowAddEdit = EditHistory.of(
                () -> { // undo: remove last 'count' rows if present
                    for (int i = 0; i < count; i++) {
                        if (!items.isEmpty()) items.remove(items.size() - 1);
                    }
                },
                () -> { // redo: append 'count' rows
                    for (int i = 0; i < count; i++) items.add(newRowSupplier.get());
                }
        );
        EditHistory.CompoundEdit withRows = new EditHistory.CompoundEdit();
        withRows.add(rowAddEdit);
        // append all existing children from 'compound'
        // reflection-free: rebuild by redoing 'compound' once into withRows is complex; keep simple by returning a new Compound that starts with rowAdd then all others
        // We cannot access children directly; instead, we wrap: undo/redo order still correct by nesting:
        EditHistory.Edit rest = compound;
        withRows.add(rest);
        return withRows;
    }

    private void collectLeafColumns(List<? extends TableColumn<?, ?>> src, List<TableColumn<?, ?>> out) {
        for (TableColumn<?, ?> c : src) {
            if (c.getColumns().isEmpty()) out.add(c);
            else collectLeafColumns(c.getColumns(), out);
        }
    }
}