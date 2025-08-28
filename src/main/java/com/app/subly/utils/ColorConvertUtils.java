package com.app.subly.utils;

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
}
