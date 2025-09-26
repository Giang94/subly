package com.app.subly.persistence;

import com.app.subly.model.SublySettings;
import com.app.subly.model.enums.BackgroundType;
import com.app.subly.model.enums.BorderWeight;
import com.app.subly.model.enums.FontWeight;
import javafx.scene.text.Font;

public class AppSettingsIO {

    // Background defaults values
    public static final String DEFAULT_PROJECTOR_COLOR = "F0F4F8";
    public static final BackgroundType DEFAULT_BACKGROUND_TYPE = BackgroundType.SOLID_COLOR;

    // Text defaults values
    public static final String DEFAULT_SUBTITLE_FONT_FAMILY = Font.getDefault().getFamily();
    public static final String DEFAULT_SUBTITLE_COLOR = "1A1A1A";
    public static final Integer DEFAULT_SUBTITLE_FONT_SIZE = 72;
    public static final FontWeight DEFAULT_FONT_WEIGHT = FontWeight.NORMAL;
    public static final BorderWeight DEFAULT_SUBTITLE_BORDER_WEIGHT = BorderWeight.NORMAL;
    public static final String DEFAULT_SUBTITLE_BORDER_COLOR = "007ACC";

    public static SublySettings load() {
        SublySettings settings = new SublySettings();
        initWithDefaults(settings);
        return settings;
    }

    private static void initWithDefaults(SublySettings settings) {
        settings.setProjectorColor(DEFAULT_PROJECTOR_COLOR);
        settings.setBackgroundType(DEFAULT_BACKGROUND_TYPE);
        settings.setSubtitleFontFamily(DEFAULT_SUBTITLE_FONT_FAMILY);
        settings.setSubtitleColor(DEFAULT_SUBTITLE_COLOR);
        settings.setSubtitleFontSize(DEFAULT_SUBTITLE_FONT_SIZE);
        settings.setFontWeight(DEFAULT_FONT_WEIGHT);
        settings.setSubtitleBorderWeight(DEFAULT_SUBTITLE_BORDER_WEIGHT);
        settings.setSubtitleBorderColor(DEFAULT_SUBTITLE_BORDER_COLOR);
    }
}
