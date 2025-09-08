package com.app.subly.utils;

import javafx.scene.paint.Color;

public class ColorConvertUtils {

    public static java.awt.Color javaFxToAwtColor(javafx.scene.paint.Color javaFxColor) {
        return new java.awt.Color(
                (float) javaFxColor.getRed(),
                (float) javaFxColor.getGreen(),
                (float) javaFxColor.getBlue(),
                (float) javaFxColor.getOpacity()
        );
    }

    public static javafx.scene.paint.Color awtToJavaFxColor(java.awt.Color awtColor) {
        return javafx.scene.paint.Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                awtColor.getAlpha() / 255.0);
    }

    public static String toHexString(javafx.scene.paint.Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        int a = (int) Math.round(color.getOpacity() * 255);

        // If you want alpha too (ARGB)
        // return String.format("#%02X%02X%02X%02X", r, g, b, a);

        // If you just want RGB
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static javafx.scene.paint.Color toJavaFxColor(String hexString) {
        return Color.web(hexString);
    }
}
