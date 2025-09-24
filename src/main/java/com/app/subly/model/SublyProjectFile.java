package com.app.subly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublyProjectFile {

    private Integer schemaVersion = 1;
    private String fileName;
    private SublySettings settings;
    private List<Chapter> chapters = new ArrayList<>();

    public void normalize() {
        if (chapters == null) chapters = new ArrayList<>();
        int chapterIndex = 1;
        for (Chapter c : chapters) {
            if (c.getIndex() == null) c.setIndex(chapterIndex++);
            if (c.getSubtitles() == null) c.setSubtitles(new ArrayList<>());
            SubtitleNormalizer.normalizeList(c.getSubtitles());
        }
        if (chapters.isEmpty()) {
            Chapter ch = new Chapter();
            ch.setIndex(1);
            ch.setTitle("Chapter 1");
            ch.setSubtitles(new ArrayList<>());
            SubtitleNormalizer.ensureAtLeastOne(ch.getSubtitles());
            chapters.add(ch);
        }
    }
}
