package com.app.subly.persistence;

import com.app.subly.model.SublySettings;

import java.io.*;
import java.util.Properties;

public class AppSettingsManager {

    private static final String SETTINGS_FILE = "settings/app_settings.properties";
    private static final String DEFAULT_PROJECTOR_COLOR = "33cc33";
    private static final String DEFAULT_SUBTITLE_COLOR = "ffff00";
    private static final double DEFAULT_SUBTITLE_FONT_SIZE = 24.0;

    public static SublySettings load() throws IOException {
        Properties properties = new Properties();
        SublySettings settings = new SublySettings();

        // Load from resources/settings/app_settings.properties
        try (InputStream in = AppSettingsManager.class.getClassLoader().getResourceAsStream(SETTINGS_FILE)) {
            if (in != null) {
                properties.load(in);

                settings.setProjectorTransparent("true".equalsIgnoreCase(properties.getProperty("projector.transparent", "false")));
                settings.setProjectorColor(properties.getProperty("projector.color", DEFAULT_PROJECTOR_COLOR));
                settings.setSubtitleColor(properties.getProperty("subtitle.color", DEFAULT_SUBTITLE_COLOR));
                settings.setSubtitleFontSize(Double.parseDouble(properties.getProperty("subtitle.fontSize", String.valueOf(DEFAULT_SUBTITLE_FONT_SIZE))));
            } else {
                // Resource not found → init defaults
                initWithDefaults(settings);
            }
        }

        return settings;
    }


    public static SublySettings loadFromFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return loadFromStream(in);
        }
    }

    private static SublySettings loadFromStream(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);

        SublySettings settings = new SublySettings();
        settings.setProjectorTransparent(Boolean.parseBoolean(properties.getProperty("projector.transparent", "false")));
        settings.setProjectorColor(properties.getProperty("projector.color", DEFAULT_PROJECTOR_COLOR));
        settings.setSubtitleColor(properties.getProperty("subtitle.color", DEFAULT_SUBTITLE_COLOR));
        settings.setSubtitleFontSize(Double.parseDouble(properties.getProperty("subtitle.fontSize", String.valueOf(DEFAULT_SUBTITLE_FONT_SIZE))));
        return settings;
    }

    private static void initWithDefaults(SublySettings settings) {
        settings.setProjectorTransparent(false);
        settings.setProjectorColor(DEFAULT_PROJECTOR_COLOR);
        settings.setSubtitleColor(DEFAULT_SUBTITLE_COLOR);
        settings.setSubtitleFontSize(DEFAULT_SUBTITLE_FONT_SIZE);
    }

    public static void save(SublySettings settings) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("projector.transparent", String.valueOf(settings.isProjectorTransparent()));
        properties.setProperty("projector.color", settings.getProjectorColor());
        properties.setProperty("subtitle.color", settings.getSubtitleColor());
        properties.setProperty("subtitle.fontSize", String.valueOf(settings.getSubtitleFontSize()));

        // Direct path to resources/settings
        try (FileOutputStream out = new FileOutputStream("src/main/resources/settings/app_settings.properties")) {
            properties.store(out, "App Settings");
        }
    }
}
