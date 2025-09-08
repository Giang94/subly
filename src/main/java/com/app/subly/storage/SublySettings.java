package com.app.subly.storage;

import java.io.Serializable;

public class SublySettings implements Serializable {
    private boolean projectorTransparent = true; // default
    private String projectorColor = "33cc33";   // default black
    private String subtitleColor = "FFFF00";    // default yellow
    private double subtitleFontSize = 24;        // default size

    public boolean isProjectorTransparent() {
        return projectorTransparent;
    }

    public void setProjectorTransparent(boolean projectorTransparent) {
        this.projectorTransparent = projectorTransparent;
    }

    public String getProjectorColor() {
        return tidyUpColorCode(projectorColor);
    }

    public void setProjectorColor(String projectorColor) {
        this.projectorColor = tidyUpColorCode(projectorColor);
    }

    public String getSubtitleColor() {
        return tidyUpColorCode(subtitleColor);
    }

    public void setSubtitleColor(String subtitleColor) {
        this.subtitleColor = tidyUpColorCode(subtitleColor);
    }

    public double getSubtitleFontSize() {
        return subtitleFontSize;
    }

    public void setSubtitleFontSize(double subtitleFontSize) {
        this.subtitleFontSize = subtitleFontSize;
    }

    private String tidyUpColorCode(String colorCode) {
        if (!colorCode.startsWith("#")) {
            return "#" + colorCode;
        }
        return colorCode;
    }
}
