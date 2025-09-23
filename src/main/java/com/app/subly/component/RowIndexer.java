package com.app.subly.component;

import com.app.subly.model.Subtitle;
import javafx.collections.ObservableList;

public final class RowIndexer {

    private RowIndexer() {
    }

    public static void renumber(ObservableList<Subtitle> items) {
        if (items == null) return;
        int next = 1;
        int last = items.size() - 1;
        for (int i = 0; i < items.size(); i++) {
            Subtitle s = items.get(i);
            if (s == null) continue;
            boolean trailingBlank = isBlank(s) && i == last;
            if (trailingBlank) {
                s.setId(next); // trailing blank shows next available index
            } else {
                s.setId(next++);
            }
        }
    }

    private static boolean isBlank(Subtitle s) {
        String a = s.getPrimaryText();
        String b = s.getSecondaryText();
        return (a == null || a.isBlank()) && (b == null || b.isBlank());
    }
}
