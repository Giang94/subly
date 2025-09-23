package com.app.subly.component;

import com.app.subly.model.BackgroundType;

import java.util.Objects;

/**
 * Immutable background configuration for a chapter.
 * Use factory methods to create:
 * - transparent()
 * - solidColor("#RRGGBB" or "#AARRGGBB")
 * - image("file:///... or path")
 */
public final class ChapterBackground {

    private final BackgroundType type;
    private final String colorHex;
    private final String imageUri;

    private ChapterBackground(BackgroundType type, String colorHex, String imageUri) {
        this.type = Objects.requireNonNull(type, "type");
        this.colorHex = colorHex;
        this.imageUri = imageUri;
        validate();
    }

    public static ChapterBackground transparent() {
        return new ChapterBackground(BackgroundType.TRANSPARENT, null, null);
    }

    public static ChapterBackground solidColor(String colorHex) {
        if (!isValidHexColor(colorHex)) {
            throw new IllegalArgumentException("colorHex must be '#RRGGBB' or '#AARRGGBB'");
        }
        return new ChapterBackground(BackgroundType.SOLID_COLOR, colorHex, null);
    }

    public static ChapterBackground image(String imageUri) {
        if (imageUri == null || imageUri.trim().isEmpty()) {
            throw new IllegalArgumentException("imageUri must be non-empty");
        }
        return new ChapterBackground(BackgroundType.IMAGE, null, imageUri.trim());
    }

    private void validate() {
        switch (type) {
            case TRANSPARENT:
                if (colorHex != null || imageUri != null) {
                    throw new IllegalStateException("Transparent background must not carry color or image");
                }
                break;
            case SOLID_COLOR:
                if (colorHex == null || imageUri != null) {
                    throw new IllegalStateException("Solid color background requires colorHex and no image");
                }
                break;
            case IMAGE:
                if (imageUri == null || colorHex != null) {
                    throw new IllegalStateException("Image background requires imageUri and no color");
                }
                break;
            default:
                throw new IllegalStateException("Unknown background type");
        }
    }

    private static boolean isValidHexColor(String s) {
        return s != null && s.matches("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");
    }

    public BackgroundType getType() {
        return type;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getImageUri() {
        return imageUri;
    }
}