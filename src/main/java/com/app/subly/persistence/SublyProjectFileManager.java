package com.app.subly.persistence;

import com.app.subly.common.SublyApplicationStage;
import com.app.subly.model.SublyProjectFile;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import com.app.subly.project.SublyProjectSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class SublyProjectFileManager {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SublyProjectFile loadProject(File file) throws IOException {
        SublySettings settings = null;
        List<Subtitle> subtitles = null;

        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                try (InputStream in = zipFile.getInputStream(entry)) {
                    if (entry.getName().endsWith("settings.json")) {
                        settings = objectMapper.readValue(in, SublySettings.class);
                    } else if (entry.getName().endsWith("subtitles.json")) {
                        subtitles = objectMapper.readValue(in,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Subtitle.class)
                        );
                    }
                }
            }
        }

        if (settings == null || subtitles == null) {
            throw new IOException("Invalid .subly project file: missing settings or subtitles.");
        }

        // Keep absolute path to enable subsequent save to the same file
        return new SublyProjectFile(file.getAbsolutePath(), settings, subtitles);
    }

    public void save(TableView<Subtitle> subtitleTable, SublyProjectSession session) {
        File target = null;
        boolean updateSession = false;

        if (session != null && session.getProjectFile() != null && session.getProjectFile().exists()) {
            target = session.getProjectFile();
        } else {
            String initial = (session != null && session.getProjectFile() != null)
                    ? session.getProjectFile().getName()
                    : "my_project.subly";
            target = chooseTarget("Save Subly project", initial);
            if (target == null) return;
            updateSession = true;
        }

        performSave(target, subtitleTable, session, updateSession);
    }

    // Save As: always prompts; always updates session to the new file
    public void saveAs(TableView<Subtitle> subtitleTable, SublyProjectSession session) {
        String initial = (session != null && session.getProjectFile() != null)
                ? session.getProjectFile().getName()
                : "my_project.subly";
        File target = chooseTarget("Save Subly project as", initial);
        if (target == null) return;

        performSave(target, subtitleTable, session, true);
    }

    private void performSave(File target,
                             TableView<Subtitle> subtitleTable,
                             SublyProjectSession session,
                             boolean updateSession) {
        try {
            SublyProjectFile projectFile = new SublyProjectFile(
                    target.getAbsolutePath(),
                    session.getSettings(),
                    subtitleTable.getItems()
            );
            saveProjectAsZip(projectFile);

            if (updateSession) {
                updateSessionFile(session, target);
            }

            System.out.println("Project saved to: " + target.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSessionFile(SublyProjectSession session, File target) {
        if (session == null) return;
        try {
            SublyProjectSession.class.getMethod("setCurrentFile", File.class).invoke(session, target);
        } catch (Exception ignore) {
            try {
                SublyProjectSession.class.getMethod("setProjectFile", File.class).invoke(session, target);
            } catch (Exception ignored) {
                // No suitable setter; ignore
            }
        }
    }

    private void saveProjectAsZip(SublyProjectFile projectFile) throws IOException {
        // Create temp files for settings + subtitles
        File settingsFile = saveSettings(projectFile.getFileName(), projectFile.getSettings());
        File subtitlesFile = saveSubtitles(projectFile.getFileName(), projectFile.getSubtitles());

        // Package them into one archive
        try (FileOutputStream fos = new FileOutputStream(projectFile.getFileName());
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // --- settings.json ---
            ZipEntry settingsEntry = new ZipEntry("settings.json");
            zipOut.putNextEntry(settingsEntry);
            byte[] settingsBytes = objectMapper.writeValueAsBytes(projectFile.getSettings());
            zipOut.write(settingsBytes);
            zipOut.closeEntry();

            // --- subtitles.json ---
            ZipEntry subtitlesEntry = new ZipEntry("subtitles.json");
            zipOut.putNextEntry(subtitlesEntry);
            byte[] subtitlesBytes = objectMapper.writeValueAsBytes(projectFile.getSubtitles());
            zipOut.write(subtitlesBytes);
            zipOut.closeEntry();
        }

        // Clean up temp files
        settingsFile.delete();
        subtitlesFile.delete();
    }

    private File saveSettings(String fileName, SublySettings settings) throws IOException {
        File file = File.createTempFile(fileName + "_settings", ".json");
        objectMapper.writeValue(file, settings);
        return file;
    }

    private File saveSubtitles(String fileName, List<Subtitle> subtitles) throws IOException {
        File file = File.createTempFile(fileName + "_subtitles", ".json");
        objectMapper.writeValue(file, subtitles);
        return file;
    }

    private File chooseTarget(String title, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Subly Project Files", "*.subly")
        );
        fileChooser.setInitialFileName(initialFileName);

        Stage stage = new SublyApplicationStage();
        File chosen = fileChooser.showSaveDialog(stage);
        if (chosen == null) return null;

        String path = chosen.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".subly")) {
            chosen = new File(path + ".subly");
        }
        return chosen;
    }
}
