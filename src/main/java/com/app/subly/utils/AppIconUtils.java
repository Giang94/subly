package com.app.subly.utils;

import javafx.scene.image.Image;

import java.util.Objects;

public class AppIconUtils {

    public static final String APP_ICON_PATH = "/images/app_icon.png";

    public static Image getAppIcon() {
        return new Image(Objects.requireNonNull(AppIconUtils.class.getResourceAsStream(APP_ICON_PATH)));
    }
}
