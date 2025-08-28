package com.app.subly.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppSettingsManager {

    private static final String SETTINGS_FILE = "app_settings.properties";
    private static final String DEFAULT_PROJECTOR_COLOR = "33cc33";
    private static final String DEFAULT_SUBTITLE_COLOR = "ffff00";
    private static final double DEFAULT_SUBTITLE_FONT_SIZE = 24.0;

    public static AppSettings load() throws IOException {
        Properties properties = new Properties();
        File file = new File(SETTINGS_FILE);

        AppSettings settings = new AppSettings();

        if (file.exists()) {
            // Load existing properties
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
            }

            settings.setProjectorTransparent("true".equalsIgnoreCase(properties.getProperty("projector.transparent", "false")));
            settings.setProjectorColor(properties.getProperty("projector.color", DEFAULT_PROJECTOR_COLOR));
            settings.setSubtitleColor(properties.getProperty("subtitle.color", DEFAULT_SUBTITLE_COLOR));
            settings.setSubtitleFontSize(Double.parseDouble(properties.getProperty("subtitle.fontSize", String.valueOf(DEFAULT_SUBTITLE_FONT_SIZE))));
        } else {
            // File not found → init with default values
            settings.setProjectorTransparent(false);
            settings.setProjectorColor(DEFAULT_PROJECTOR_COLOR);
            settings.setSubtitleColor(DEFAULT_SUBTITLE_COLOR);
            settings.setSubtitleFontSize(DEFAULT_SUBTITLE_FONT_SIZE);

            // Save defaults to file
            properties.setProperty("projector.transparent", String.valueOf(settings.isProjectorTransparent()));
            properties.setProperty("projector.color", settings.getProjectorColor());
            properties.setProperty("subtitle.color", settings.getSubtitleColor());
            properties.setProperty("subtitle.fontSize", String.valueOf(settings.getSubtitleFontSize()));

            try (FileOutputStream out = new FileOutputStream(file)) {
                properties.store(out, "Default App Settings");
            }
        }

        return settings;
    }

    public static void save(AppSettings settings) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("projector.transparent", String.valueOf(settings.isProjectorTransparent()));
        properties.setProperty("projector.color", settings.getProjectorColor());
        properties.setProperty("subtitle.color", settings.getSubtitleColor());
        properties.setProperty("subtitle.fontSize", String.valueOf(settings.getSubtitleFontSize()));

        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(out, "App Settings");
        }
    }
}
