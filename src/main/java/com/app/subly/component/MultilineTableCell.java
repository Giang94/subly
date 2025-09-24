package com.app.subly.component;

import com.app.subly.model.Subtitle;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MultilineTableCell extends TableCell<Subtitle, String> {
    private final TextArea textArea = new TextArea();
    private boolean committingViaTab = false;

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
                // Delay so focus model updates first
                Platform.runLater(() -> {
                    if (isEditing()) doCommit(textArea.getText());
                });
            }
        });

        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !isEditing()) {
                // You decided ENTER no longer moves; just start edit
                startEdit();
                e.consume();
            }
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
    }

    @Override
    public void commitEdit(String newValue) {
        // Route through helper to ensure consistent cleanup
        doCommit(newValue);
    }

    private void doCommit(String raw) {
        String v = raw == null ? "" : raw;
        // Preserve your literal \n storing rule
        super.commitEdit(v.replace("\n", "\\n"));
        setGraphic(null);
        setText(getItem());
        // Restore focus to table unless we are moving via TAB
        if (!committingViaTab) {
            refocusTable();
        }
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            textArea.setText(item == null ? "" : item);
            setGraphic(textArea);
            setText(null);
        } else {
            setText(item);
            setGraphic(null);
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
                try { RowIndexer.renumber(tv.getItems()); } catch (Throwable ignored) {}
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