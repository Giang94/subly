package com.app.subly.utils;

import com.app.subly.model.enums.FontWeight;
import javafx.scene.text.Font;

public final class Fonts {

    private Fonts() {
    }

    private static void load(String path) {
        Font.loadFont(Fonts.class.getResourceAsStream(path), 12);
    }

    public static javafx.scene.text.FontWeight mapFxWeight(FontWeight w) {
        return switch (w) {
            case LIGHT -> javafx.scene.text.FontWeight.LIGHT;
            case NORMAL -> javafx.scene.text.FontWeight.NORMAL;
            case BOLD -> javafx.scene.text.FontWeight.BOLD;
            case HEAVY -> javafx.scene.text.FontWeight.EXTRA_BOLD;
        };
    }
}