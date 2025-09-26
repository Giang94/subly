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

    public PresentingModeNavigator(List<Chapter> chapters,
                                   int chapterIndex, int rowIndex,
                                   Listener listener) {
        this.chapters = Objects.requireNonNull(chapters);
        this.listener = listener;
        this.chapterIndex = chapterIndex;
        this.rowIndex = rowIndex;
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
        var subtitles = ch.getSubtitles();
        int rowsCount = subtitles != null ? subtitles.size() : 0;
        int chaptersCount = this.chapters.size() - 1;

        if (rowIndex + 1 < rowsCount) {
            rowIndex++;
            var subtitle = subtitles.get(rowIndex);
            System.out.println("Next line in same chapter: " + (subtitle != null ? subtitle.getPrimaryText() : "null"));
            notifyChange();
            return;
        }
        if (chapterIndex + 1 < chaptersCount) {
            chapterIndex++;
            rowIndex = 0;
            var nextCh = chapters.get(chapterIndex);
            var nextSubtitles = nextCh.getSubtitles();
            var subtitle = (nextSubtitles != null && !nextSubtitles.isEmpty()) ? nextSubtitles.get(rowIndex) : null;
            System.out.println("Next line in next chapter: " + (subtitle != null ? subtitle.getPrimaryText() : "null"));
            notifyChange();
        } else {
            listener.onEndReached();
            System.out.println("End reached");
        }
    }

    public void previous() {
        if (!presentingMode || chapters.isEmpty()) return;
        var subtitles = chapters.get(chapterIndex).getSubtitles();

        if (rowIndex > 0) {
            rowIndex--;
            var subtitle = (subtitles != null && rowIndex < subtitles.size()) ? subtitles.get(rowIndex) : null;
            notifyChange();
            System.out.println("Previous line in same chapter: " + (subtitle != null ? subtitle.getPrimaryText() : "null"));
            return;
        }
        if (chapterIndex > 0) {
            chapterIndex--;
            var prev = chapters.get(chapterIndex);
            var prevSubtitles = prev.getSubtitles();
            rowIndex = (prevSubtitles != null && !prevSubtitles.isEmpty()) ? Math.max(0, prevSubtitles.size() - 1) : 0;
            var subtitle = (prevSubtitles != null && rowIndex < prevSubtitles.size()) ? prevSubtitles.get(rowIndex) : null;
            System.out.println("Previous line in previous chapter: " + (subtitle != null ? subtitle.getPrimaryText() : "null"));
            notifyChange();
        } else {
            listener.onStartReached();
            System.out.println("Start reached");
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
        notifyChange();
    }

    private void notifyChange() {
        listener.onPositionChanged(chapterIndex, rowIndex);
    }
}