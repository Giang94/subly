package com.app.subly.persistence;

import com.app.subly.model.SublyProjectFile;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                        subtitles = objectMapper.readValue(
                                in,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Subtitle.class)
                        );
                    }
                }
            }
        }

        if (settings == null || subtitles == null) {
            throw new IOException("Invalid .subly project file: missing settings or subtitles.");
        }

        return new SublyProjectFile(file.getName(), settings, subtitles);
    }

    public void saveProject(SublyProjectFile projectFile) throws IOException {
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
}
