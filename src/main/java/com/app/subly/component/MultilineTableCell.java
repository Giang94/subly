package com.app.subly.component;

import com.app.subly.model.Subtitle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class MultilineTableCell extends TableCell<Subtitle, String> {

    private static final String STYLE_COLUMN = "-fx-background-color: rgba(123,167,204,0.18);";
    private static final String STYLE_CELL = "-fx-background-color: rgba(123,167,204,0.35);"
            + "-fx-border-color: -fx-accent; -fx-border-width:1; -fx-border-radius:1; -fx-padding:1;";
    private static final String TABLE_SELECTION_STYLE =
            "-fx-selection-bar: #b4dcff; -fx-selection-bar-non-focused: #b4dcff;";

    private final TextArea textArea = new TextArea();
    private boolean committingViaTab = false;
    private boolean focusListenerInstalled = false;

    public MultilineTableCell() {
        textArea.setWrapText(true);
        textArea.setPrefRowCount(1);

        textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                committingViaTab = true;
                doCommit(textArea.getText());
                handleTabNavigation(e.isShiftDown());
                committingViaTab = false;
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    int pos = textArea.getCaretPosition();
                    textArea.insertText(pos, "\\n");
                } else {
                    doCommit(textArea.getText());
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                e.consume();
            }
        });

        textArea.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if ("\r".equals(e.getCharacter()) || "\n".equals(e.getCharacter())) {
                e.consume();
            }
        });

        textArea.focusedProperty().addListener((o, was, isNow) -> {
            if (!isNow && isEditing()) {
                Platform.runLater(() -> {
                    if (isEditing()) doCommit(textArea.getText());
                });
            }
        });

        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !isEditing()) {
                startEdit();
                e.consume();
            }
        });

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (isEmpty()) return;
            TableView<Subtitle> tv = getTableView();
            if (tv != null) {
                tv.getSelectionModel().clearAndSelect(getIndex(), getTableColumn());
                tv.getFocusModel().focus(getIndex(), getTableColumn());
            }
            if (e.getClickCount() == 2 && !isEditing()) {
                startEdit();
            }
            e.consume();
        });
    }

    @Override
    public void startEdit() {
        if (!isEditable() || getTableView() == null || !getTableView().isEditable()) return;
        super.startEdit();
        setText(null);
        setGraphic(textArea);
        textArea.setText(getItem() == null ? "" : getItem());
        textArea.requestFocus();
        textArea.positionCaret(textArea.getText().length());
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        setText(getItem());
        refocusTable();
        applySelectionStyling();
    }

    @Override
    public void commitEdit(String newValue) {
        doCommit(newValue);
    }

    private void doCommit(String raw) {
        String v = raw == null ? "" : raw;
        super.commitEdit(v.replace("\n", "\\n"));
        setGraphic(null);
        setText(getItem());
        if (!committingViaTab) {
            refocusTable();
        }
        applySelectionStyling();
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        ensureTableSelectionColors();
        if (!focusListenerInstalled) {
            installFocusSync();
            focusListenerInstalled = true;
        }

        if (empty) {
            setText(null);
            setGraphic(null);
            setStyle("");
        } else if (isEditing()) {
            textArea.setText(item == null ? "" : item);
            setGraphic(textArea);
            setText(null);
        } else {
            setText(item);
            setGraphic(null);
        }

        applySelectionStyling();
    }

    private void installFocusSync() {
        TableView<Subtitle> tv = getTableView();
        if (tv == null) return;
        ChangeListener<TablePosition> focusListener = (obs, oldPos, newPos) -> applySelectionStyling();
        tv.getFocusModel().focusedCellProperty().addListener(focusListener);
        // Fixed listener (use raw TablePosition due to JavaFX API)
        tv.getSelectionModel().getSelectedCells().addListener(
                (ListChangeListener<TablePosition>) c -> applySelectionStyling()
        );
    }

    private void ensureTableSelectionColors() {
        TableView<Subtitle> tv = getTableView();
        if (tv == null) return;
        if (tv.getProperties().get("customSelectionColorsInstalled") == null) {
            String existing = tv.getStyle();
            tv.setStyle((existing == null ? "" : existing + ";") + TABLE_SELECTION_STYLE);
            tv.getProperties().put("customSelectionColorsInstalled", Boolean.TRUE);
        }
    }

    private void applySelectionStyling() {
        TableView<Subtitle> tv = getTableView();
        if (tv == null) {
            setStyle("");
            return;
        }
        TablePosition<Subtitle, ?> focusPos = tv.getFocusModel().getFocusedCell();
        if (focusPos == null || focusPos.getTableColumn() == null) {
            setStyle("");
            return;
        }

        boolean sameColumn = getTableColumn() == focusPos.getTableColumn();
        boolean sameCell = sameColumn && focusPos.getRow() == getIndex();

        if (sameCell) {
            setStyle(STYLE_CELL);
        } else {
            setStyle("");
        }
    }

    private void handleTabNavigation(boolean backwards) {
        TableView<Subtitle> tv = getTableView();
        if (tv == null) return;
        boolean isLastCol = getTableColumn() == tv.getVisibleLeafColumns()
                .get(tv.getVisibleLeafColumns().size() - 1);

        if (!backwards && isLastCol) {
            int currentRow = getIndex();
            int targetRow = currentRow + 1;

            if (targetRow >= tv.getItems().size()) {
                if (rowBlank(tv, currentRow, textArea.getText())) {
                    refocusTable();
                    return;
                }
                Subtitle s = new Subtitle(tv.getItems().size() + 1, "", "");
                tv.getItems().add(s);
                try {
                    RowIndexer.renumber(tv.getItems());
                } catch (Throwable ignored) {
                }
            }

            if (tv.getVisibleLeafColumns().size() >= 2) {
                TableColumn<Subtitle, ?> primaryCol = tv.getVisibleLeafColumn(1);
                tv.getSelectionModel().clearAndSelect(targetRow, primaryCol);
                tv.getFocusModel().focus(targetRow, primaryCol);
                tv.edit(targetRow, primaryCol);
            }
        } else {
            navigateHorizontal(!backwards);
        }
    }

    private void navigateHorizontal(boolean forward) {
        TableView<Subtitle> tv = getTableView();
        if (tv == null) return;
        TablePosition<Subtitle, ?> pos = tv.getFocusModel().getFocusedCell();
        if (pos == null) return;
        int col = pos.getColumn();
        int next = forward ? col + 1 : col - 1;
        if (next < 0 || next >= tv.getVisibleLeafColumns().size()) return;
        TableColumn<Subtitle, ?> nextCol = tv.getVisibleLeafColumn(next);
        tv.getFocusModel().focus(getIndex(), nextCol);
        tv.getSelectionModel().clearAndSelect(getIndex(), nextCol);
        tv.edit(getIndex(), nextCol);
    }

    private boolean rowBlank(TableView<Subtitle> tv, int row, String currentSecondaryText) {
        String primary = "";
        if (tv.getVisibleLeafColumns().size() > 1) {
            Object pv = tv.getVisibleLeafColumn(1).getCellData(row);
            primary = pv == null ? "" : pv.toString();
        }
        String secondary = currentSecondaryText == null ? "" : currentSecondaryText;
        return isBlank(primary) && isBlank(secondary);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void refocusTable() {
        TableView<Subtitle> tv = getTableView();
        if (tv != null) {
            tv.requestFocus();
            int r = getIndex();
            if (r >= 0 && r < tv.getItems().size()) {
                tv.getFocusModel().focus(r, getTableColumn());
                tv.getSelectionModel().select(r, getTableColumn());
            }
        }
    }
}