package com.app.subly.persistence;

import com.app.subly.component.SublyApplicationStage;
import com.app.subly.model.Chapter;
import com.app.subly.model.SublyProjectFile;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SublyProjectFileManagers {

    private static final String EXT = ".subly";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);

    public SublyProjectFile loadProject(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("Project file not found: " + (file == null ? "null" : file.getAbsolutePath()));
        }
        SublyProjectFile p = MAPPER.readValue(file, SublyProjectFile.class);
        // if (p.getSchemaVersion() == null) p.setSchemaVersion(1);
        return p;
    }

    // Save to the current session file, or delegate to Save As if none
    public void save(TableView<Subtitle> table, SublyProjectSession session) {
        File target = (session != null) ? session.getProjectFile() : null;
        if (target == null) {
            saveAs(table, session);
            return;
        }
        writeNow(buildProjectFromUi(table, session, target.getName()), target);
    }

    // Prompt for a file name, update session, then save
    public void saveAs(TableView<Subtitle> table, SublyProjectSession session) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Project");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Subly Project (*.subly)", "*" + EXT),
                );
        File initial = (session != null && session.getProjectFile() != null) ? session.getProjectFile() : null;
        if (initial != null && initial.getParentFile() != null) {
            chooser.setInitialDirectory(initial.getParentFile());
            chooser.setInitialFileName(stripExt(initial.getName()) + EXT);
        } else {
            chooser.setInitialFileName("Untitled" + EXT);
        }

        Stage stage = new SublyApplicationStage();
        File chosen = chooser.showSaveDialog(stage);
        if (chosen == null) return;

        // Ensure extension
        if (!chosen.getName().toLowerCase().endsWith(EXT)) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + EXT);
        }

        if (session != null) session.setProjectFile(chosen);
        writeNow(buildProjectFromUi(table, session, chosen.getName()), chosen);
    }

    private void writeNow(SublyProjectFile project, File target) {
        try {
            target.getParentFile().mkdirs();
            MAPPER.writeValue(target, project);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save project: " + target, e);
        }
    }

    // Build a SublyProjectFile snapshot from UI + session
    private SublyProjectFile buildProjectFromUi(TableView<Subtitle> table, SublyProjectSession session, String displayName) {
        SublyProjectFile p = new SublyProjectFile();
        // p.setSchemaVersion(1);
        p.setFileName(stripExt(displayName));

        // Copy settings from session
        SublySettings s = (session != null) ? session.getSettings() : null;
        if (s != null) {
            p.setSettings(s);
        }

        // Chapters: prefer session chapters; else synthesize one from the table
        List<Chapter> chapters = (session != null && session.getChapters() != null && !session.getChapters().isEmpty())
                ? deepCopyChapters(session.getChapters())
                : List.of(synthesizeChapterFromTable(table));

        // Ensure each chapter has at least one subtitle
        for (Chapter c : chapters) {
            if (c.getSubtitles() == null || c.getSubtitles().isEmpty()) {
                c.setSubtitles(new ArrayList<>(List.of(new Subtitle(1, "", ""))));
            } else {
                normalizeSubtitles(c.getSubtitles());
            }
        }
        p.setChapters(chapters);

        return p;
    }

    // Create a single chapter from the current table content
    private Chapter synthesizeChapterFromTable(TableView<Subtitle> table) {
        Chapter c = new Chapter();
        c.setTitle("Chapter 1");
        List<Subtitle> items = new ArrayList<>();
        if (table != null && table.getItems() != null) {
            items.addAll(table.getItems());
        }
        items = withoutTrailingBlank(items);
        if (items.isEmpty()) {
            items.add(new Subtitle(1, "", ""));
        }
        renumber(items);
        c.setSubtitles(items);
        return c;
    }

    // Deep copy chapters if your model reuses JavaFX collections
    private List<Chapter> deepCopyChapters(List<Chapter> src) {
        List<Chapter> out = new ArrayList<>();
        for (Chapter c : src) {
            Chapter nc = new Chapter();
            nc.setTitle(c.getTitle());
            List<Subtitle> subs = new ArrayList<>();
            if (c.getSubtitles() != null) subs.addAll(c.getSubtitles());
            subs = withoutTrailingBlank(subs);
            if (subs.isEmpty()) subs.add(new Subtitle(1, "", ""));
            renumber(subs);
            nc.setSubtitles(subs);
            out.add(nc);
        }
        return out;
    }

    private List<Subtitle> withoutTrailingBlank(List<Subtitle> src) {
        List<Subtitle> list = new ArrayList<>(src);
        // Drop final row if both fields are blank
        while (!list.isEmpty()) {
            Subtitle last = list.get(list.size() - 1);
            String p = safe(last.getPrimaryText());
            String s = safe(last.getSecondaryText());
            if (p.isEmpty() && s.isEmpty()) {
                list.remove(list.size() - 1);
            } else {
                break;
            }
        }
        return list;
    }

    private void renumber(List<Subtitle> list) {
        for (int i = 0; i < list.size(); i++) {
            Subtitle s = list.get(i);
            s.setId(i + 1);
        }
    }

    private String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : name;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void normalizeSubtitles(List<Subtitle> list) {
        if (list == null) return;

        // Start from a copy with trailing blank rows removed
        List<Subtitle> normalized = withoutTrailingBlank(list);

        // Trim and null-safe fields
        for (Subtitle s : normalized) {
            s.setPrimaryText(safe(s.getPrimaryText()));
            s.setSecondaryText(safe(s.getSecondaryText()));
        }

        // Ensure at least one row
        if (normalized.isEmpty()) {
            normalized.add(new Subtitle(1, "", ""));
        }

        // Renumber sequentially
        renumber(normalized);

        // Replace contents to keep the same list instance (e.g., ObservableList)
        list.clear();
        list.addAll(normalized);
    }
}