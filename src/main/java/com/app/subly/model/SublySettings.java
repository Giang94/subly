package com.app.subly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublySettings implements Serializable {

    private boolean projectorTransparent = true; // default
    private String projectorColor = "33cc33";   // default black
    private String subtitleColor = "FFFF00";    // default yellow
    private double subtitleFontSize = 24;        // default size

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
}
