package com.app.subly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublySettings implements Serializable {

    private BackgroundType backgroundType = BackgroundType.TRANSPARENT; // default
    private boolean projectorTransparent = true; // default
    private String projectorColor = "33cc33";   // default black
    private String subtitleColor = "FFFF00";    // default yellow
    private Integer subtitleFontSize = 24;        // default size
    private String projectorImageUri;

    public String getProjectorImageUri() {
        return projectorImageUri;
    }

    public void setProjectorImageUri(String projectorImageUri) {
        this.projectorImageUri = projectorImageUri;
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

    private String tidyUpColorCode(String colorCode) {
        if (!colorCode.startsWith("#")) {
            return "#" + colorCode;
        }
        return colorCode;
    }

    public BackgroundType getBackgroundType() {
        return backgroundType;
    }

    public void setBackgroundType(BackgroundType backgroundType) {
        this.backgroundType = backgroundType;
    }

    public boolean isProjectorTransparent() {
        return projectorTransparent;
    }

    public void setProjectorTransparent(boolean projectorTransparent) {
        this.projectorTransparent = projectorTransparent;
    }

    public Integer getSubtitleFontSize() {
        return subtitleFontSize;
    }

    public void setSubtitleFontSize(Integer subtitleFontSize) {
        this.subtitleFontSize = subtitleFontSize;
    }
}
