package com.app.subly.component;

import com.app.subly.model.Subtitle;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

public class TrailingBlankRowPolicy {

    public boolean isBlankRow(Subtitle s) {
        if (s == null) return true;
        String p = s.getPrimaryText();
        String t = s.getSecondaryText();
        return (p == null || p.isBlank()) && (t == null || t.isBlank());
    }

    public void ensureTrailingBlankRow(TableView<Subtitle> table) {
        if (table == null || table.getItems() == null) return;
        ObservableList<Subtitle> items = table.getItems();

        // Remove blank rows except the last
        for (int i = 0; i < items.size() - 1; i++) {
            if (isBlankRow(items.get(i))) {
                items.remove(i);
                i--;
            }
        }
        // Ensure exactly one trailing blank row
        if (items.isEmpty() || !isBlankRow(items.get(items.size() - 1))) {
            items.add(new Subtitle(items.size() + 1, "", ""));
        }
    }
}