package com.app.subly.project;

import com.app.subly.component.TrailingBlankRowPolicy;
import com.app.subly.model.Chapter;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@Setter
public class SublyProjectSession {

    private java.io.File projectFile;
    private SublySettings settings = new SublySettings();

    private final ObservableList<Chapter> chapters = FXCollections.observableArrayList();
    private final IntegerProperty selectedChapterIndex = new SimpleIntegerProperty(-1);

    private Consumer<Boolean> dirtyListener;
    private boolean dirty;

    public SublyProjectSession() {
        chapters.addListener((ListChangeListener<Chapter>) c -> markDirty());
        selectedChapterIndex.addListener((o, ov, nv) -> markDirty());
    }

    public List<Chapter> getEffectiveChapters() {
        return chapters.stream()
                .filter(c -> !isPlaceholder(c))
                .collect(Collectors.toList());
    }

    private boolean isPlaceholder(Chapter c) {
        if (c == null) return false;
        int last = chapters.size() - 1;
        return (c.getTitle() == null || c.getTitle().isBlank()) && chapters.indexOf(c) == last;
    }

    public void ensurePlaceholderChapter() {
        if (chapters.isEmpty()) {
            addChapter();
        }
        if (chapters.isEmpty()) return;
        Chapter last = chapters.get(chapters.size() - 1);
        if (!isPlaceholder(last)) {
            Chapter placeholder = new Chapter();
            placeholder.setTitle("");
            // leave subtitles empty for placeholder
            chapters.add(placeholder);
        }
    }

    public int getSelectedChapterIndex() {
        return selectedChapterIndex.get();
    }

    public void setSelectedChapterIndex(int idx) {
        if (idx >= 0 && idx < chapters.size()) {
            selectedChapterIndex.set(idx);
        } else {
            selectedChapterIndex.set(-1);
        }
    }

    public Chapter getSelectedChapter() {
        int i = getSelectedChapterIndex();
        return (i >= 0 && i < chapters.size()) ? chapters.get(i) : null;
    }

    public void ensureAtLeastOneChapter() {
        if (chapters.isEmpty()) {
            addChapter();
        }
        if (getSelectedChapterIndex() < 0) setSelectedChapterIndex(0);
        Chapter c = getSelectedChapter();
        if (c != null && !isPlaceholder(c) && c.getSubtitles().isEmpty()) {
            c.getSubtitles().add(newEmptySubtitle());
        }
        ensurePlaceholderChapter();
    }

    public Chapter addChapter() {
        Chapter c = new Chapter();
        int realCount = (int) chapters.stream().filter(ch -> !isPlaceholder(ch)).count();
        c.setTitle("Chapter " + (realCount + 1));
        c.getSubtitles().add(newEmptySubtitle());

        // Insert before placeholder if it exists
        if (!chapters.isEmpty() && isPlaceholder(chapters.get(chapters.size() - 1))) {
            chapters.add(chapters.size() - 1, c);
        } else {
            chapters.add(c);
        }
        ensurePlaceholderChapter();
        if (getSelectedChapterIndex() < 0) setSelectedChapterIndex(0);
        return c;
    }

    public void ensureAllChapterIds() {
        for (Chapter c : chapters) {
            c.ensureId();
        }
    }

    public void replaceAllChapters(List<Chapter> newChapters) {
        chapters.setAll(newChapters);
        ensureAllChapterIds();
        ensureAtLeastOneChapter();
        setSelectedChapterIndex(0);
        markDirty();
    }

    public void syncCurrentChapterFromTable(TableView<Subtitle> table,
                                            TrailingBlankRowPolicy trailingPolicy) {
        if (table == null) return;
        Chapter selected = getSelectedChapter();
        if (selected == null || isPlaceholder(selected)) return;

        List<Subtitle> items = table.getItems();
        if (items == null) return;

        List<Subtitle> cleaned = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Subtitle s = items.get(i);
            if (s == null) continue;
            boolean isLast = (i == items.size() - 1);
            if (trailingPolicy != null && trailingPolicy.isBlankRow(s) && isLast) continue;
            cleaned.add(s);
        }
        for (int i = 0; i < cleaned.size(); i++) {
            cleaned.get(i).setId(i + 1);
        }

        selected.getSubtitles().clear();
        selected.getSubtitles().addAll(cleaned);

        markDirty();
    }

    private Subtitle newEmptySubtitle() {
        Subtitle s = new Subtitle();
        s.setId(1);
        s.setPrimaryText("This text will be displayed on the screen.");
        s.setSecondaryText("");
        return s;
    }

    public void setSettings(SublySettings settings) {
        this.settings = settings;
        markDirty();
    }

    public void update(java.util.function.Consumer<SublySettings> mutator) {
        if (settings == null) settings = new SublySettings();
        mutator.accept(settings);
        markDirty();
    }

    public void touch() {
        markDirty();
    }

    public void clearDirty() {
        dirty = false;
        if (dirtyListener != null) dirtyListener.accept(false);
    }

    private void markDirty() {
        dirty = true;
        if (dirtyListener != null) dirtyListener.accept(true);
    }

    public String computeNextChapterTitle() {
        int max = 0;
        for (Chapter c : chapters) {
            if (isPlaceholder(c)) continue;
            String t = c.getTitle();
            if (t == null) continue;
            t = t.trim();
            if (t.startsWith("Chapter ")) {
                try {
                    int n = Integer.parseInt(t.substring(8).trim());
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "Chapter " + (max + 1);
    }

    public boolean promotePlaceholderForSubtitleEdit() {
        Chapter ch = getSelectedChapter();
        if (ch == null || !isPlaceholder(ch)) return false;

        // Assign default title
        if (ch.getTitle() == null || ch.getTitle().isBlank()) {
            ch.setTitle(computeNextChapterTitle());
        }

        // Ensure at least one subtitle
        if (ch.getSubtitles().isEmpty()) {
            Subtitle s = new Subtitle();
            s.setId(1);
            s.setPrimaryText("");
            s.setSecondaryText("");
            ch.getSubtitles().add(s);
        }

        // Append new placeholder if needed
        ensurePlaceholderChapter();
        touch();
        return true;
    }
}