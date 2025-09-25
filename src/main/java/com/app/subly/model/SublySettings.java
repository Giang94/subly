package com.app.subly.model;

import com.app.subly.model.enums.BackgroundType;
import com.app.subly.model.enums.BorderWeight;
import com.app.subly.model.enums.FontWeight;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublySettings implements Serializable {

    private BackgroundType backgroundType = BackgroundType.TRANSPARENT;
    private boolean projectorTransparent = true;
    private String projectorColor = "ffffff";
    private String projectorImageUri;

    private String subtitleFontFamily = "Arial";
    private String subtitleColor = "000000";
    private Integer subtitleFontSize = 48;
    private FontWeight fontWeight = FontWeight.NORMAL;
    private BorderWeight subtitleBorderWeight = BorderWeight.NORMAL;
    private String subtitleBorderColor = "000000";

    public String getSubtitleBorderColor() {
        return tidyUpColorCode(subtitleBorderColor);
    }

    public void setSubtitleBorderColor(String subtitleBorderColor) {
        this.subtitleBorderColor = tidyUpColorCode(subtitleBorderColor);
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
}
