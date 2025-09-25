package com.app.subly.controller.manager;

import com.app.subly.model.Chapter;

import java.util.List;
import java.util.Objects;

public class PresentingModeNavigator {

    public interface Listener {
        void onPositionChanged(int chapterIndex, int rowIndex);

        void onEndReached();

        void onStartReached();
    }

    private final List<Chapter> chapters;
    private final Listener listener;

    private boolean presentingMode;
    private int chapterIndex = 0;
    private int rowIndex = 0;

    public PresentingModeNavigator(List<Chapter> chapters, Listener listener) {
        this.chapters = Objects.requireNonNull(chapters);
        this.listener = Objects.requireNonNull(listener);
        normalize();
    }

    public void setPresentingMode(boolean on) {
        this.presentingMode = on;
        if (on) notifyChange();
    }

    public boolean isPresentingMode() {
        return presentingMode;
    }

    public void resetToStart() {
        chapterIndex = 0;
        rowIndex = 0;
        if (presentingMode) notifyChange();
    }

    public void next() {
        if (!presentingMode || chapters.isEmpty()) return;
        var ch = chapters.get(chapterIndex);
        int rows = ch.getSubtitles().size();

        if (rowIndex + 1 < rows) {
            rowIndex++;
            notifyChange();
            return;
        }
        if (chapterIndex + 1 < chapters.size()) {
            chapterIndex++;
            rowIndex = 0;
            notifyChange();
        } else {
            listener.onEndReached();
        }
    }

    public void previous() {
        if (!presentingMode || chapters.isEmpty()) return;

        if (rowIndex > 0) {
            rowIndex--;
            notifyChange();
            return;
        }
        if (chapterIndex > 0) {
            chapterIndex--;
            var prev = chapters.get(chapterIndex);
            rowIndex = Math.max(0, prev.getSubtitles().size() - 1);
            notifyChange();
        } else {
            listener.onStartReached();
        }
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    private void normalize() {
        if (chapters.isEmpty()) {
            chapterIndex = 0;
            rowIndex = 0;
            return;
        }
        if (chapterIndex >= chapters.size()) chapterIndex = chapters.size() - 1;
        int rows = chapters.get(chapterIndex).getSubtitles().size();
        if (rows == 0) rowIndex = 0;
        else if (rowIndex >= rows) rowIndex = rows - 1;
    }

    private void notifyChange() {
        listener.onPositionChanged(chapterIndex, rowIndex);
    }
}