package com.app.subly.component;

import com.app.subly.model.Subtitle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MultilineTableCell extends TableCell<Subtitle, String> {
    private final TextArea textArea = new TextArea();

    public MultilineTableCell() {
        textArea.setWrapText(true);
        textArea.setPrefRowCount(1); // Keep it from growing too tall

        textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    // Insert literal \n
                    int pos = textArea.getCaretPosition();
                    textArea.insertText(pos, "\\n");
                } else {
                    // Commit without adding a real newline
                    commitEdit(textArea.getText());
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                e.consume();
            }
        });

        textArea.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            String ch = e.getCharacter();
            if ("\r".equals(ch) || "\n".equals(ch)) {
                e.consume();
            }
        });
    }

    @Override
    public void startEdit() {
        super.startEdit();
        setText(null);
        setGraphic(textArea);

        String item = getItem();
        if (item != null) {
            // Always show literal "\n", never real line breaks
            textArea.setText(item.replace("\n", "\\n"));
        } else {
            textArea.setText("");
        }
        textArea.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    @Override
    public void commitEdit(String newValue) {
        if (newValue != null) {
            // Store with literal \n, not real newline
            super.commitEdit(newValue.replace("\n", "\\n"));
        } else {
            super.commitEdit(newValue);
        }
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            textArea.setText(item);
            setGraphic(textArea);
            setText(null);
        } else {
            setText(item);
            setGraphic(null);
        }
    }
}
