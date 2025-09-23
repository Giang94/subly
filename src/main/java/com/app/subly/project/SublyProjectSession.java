package com.app.subly.project;

import com.app.subly.model.SublySettings;
import com.app.subly.persistence.AppSettingsManager;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class SublyProjectSession {

    private SublySettings settings;
    private File projectFile;
    private boolean dirty;

    public SublyProjectSession(SublySettings initial) {
        this.settings = initial;
        this.dirty = false;
    }

    public static SublyProjectSession newSessionWithDefaults() throws IOException {
        // Start from app defaults/resources
        return new SublyProjectSession(AppSettingsManager.load());
    }

    public static SublyProjectSession loadFrom(File file) throws IOException {
        SublyProjectSession s = new SublyProjectSession(AppSettingsManager.loadFromFile(file));
        s.projectFile = file;
        s.dirty = false;
        return s;
    }

    public SublySettings getSettings() {
        return settings;
    }

    public File getProjectFile() {
        return projectFile;
    }

    public void setProjectFile(File file) {
        this.projectFile = file;
    }

    public boolean hasCurrentFile() {
        return this.projectFile != null;
    }

    public boolean isDirty() {
        return dirty;
    }

    // Apply any UI change to the in-memory settings
    public void update(Consumer<SublySettings> mutator) {
        mutator.accept(settings);
        dirty = true;
    }
}