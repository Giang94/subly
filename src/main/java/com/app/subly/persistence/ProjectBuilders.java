package com.app.subly.persistence;

import com.app.subly.model.Chapter;
import com.app.subly.model.SublyProjectFile;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

public final class ProjectBuilders {

    private ProjectBuilders() {
    }

    public static SublyProjectFile fromUi(String fileName,
                                          SublyProjectSession session) {

        SublyProjectFile project = new SublyProjectFile();
        project.setFileName(stripExt(fileName));

        if (session != null) {
            SublySettings settings = session.getSettings();
            if (settings != null) project.setSettings(settings);
        }

        List<Chapter> chapterList = session != null ? session.getEffectiveChapters() : null;
        List<Chapter> chapters = new ArrayList<>();
        if (chapterList != null && !chapterList.isEmpty()) {
            for (Chapter source : chapterList) {
                Chapter copy = new Chapter();
                copy.setId(source.getId());
                copy.setIndex(source.getIndex());
                copy.setTitle(source.getTitle());
                List<Subtitle> subs = new ArrayList<>();
                if (source.getSubtitles() != null) subs.addAll(source.getSubtitles());
                copy.setSubtitles(subs);
                chapters.add(copy);
            }
        }
//        else if (subtitleTable != null && subtitleTable.getItems() != null) {
//            // Fallback: single chapter from table
//            Chapter ch = new Chapter();
//            ch.setIndex(1);
//            ch.setTitle("Chapter 1");
//            ch.getSubtitles().addAll(subtitleTable.getItems());
//            chapters.add(ch);
//        }

        project.setChapters(chapters);
        project.normalize();
        return project;
    }

    private static String stripExt(String n) {
        if (n == null) return null;
        int i = n.lastIndexOf('.');
        return (i > 0) ? n.substring(0, i) : n;
    }
}