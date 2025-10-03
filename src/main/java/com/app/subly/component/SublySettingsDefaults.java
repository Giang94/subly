package com.app.subly.component;

import com.app.subly.model.SublySettings;

import static com.app.subly.persistence.AppSettingsIO.*;

public final class SublySettingsDefaults {
    private SublySettingsDefaults() {
    }

    public static void apply(SublySettings s) {
        if (s == null)
            return;
        if (s.getFontWeight() == null)
            s.setFontWeight(DEFAULT_FONT_WEIGHT);
        if (s.getSubtitleBorderWeight() == null)
            s.setSubtitleBorderWeight(DEFAULT_SUBTITLE_BORDER_WEIGHT);
        if (blank(s.getSubtitleBorderColor()))
            s.setSubtitleBorderColor(DEFAULT_SUBTITLE_BORDER_COLOR);
        if (blank(s.getSubtitleColor()))
            s.setSubtitleColor(DEFAULT_SUBTITLE_COLOR);
        if (blank(s.getSubtitleFontFamily()))
            s.setSubtitleFontFamily(DEFAULT_SUBTITLE_FONT_FAMILY);
        if (s.getSubtitleFontSize() <= 0)
            s.setSubtitleFontSize(DEFAULT_SUBTITLE_FONT_SIZE);
        if (s.getBackgroundType() == null)
            s.setBackgroundType(DEFAULT_BACKGROUND_TYPE);
        if (blank(s.getProjectorColor()))
            s.setProjectorColor(DEFAULT_PROJECTOR_COLOR);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}