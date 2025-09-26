package com.app.subly.model;

import com.app.subly.model.enums.BackgroundType;
import com.app.subly.model.enums.BorderWeight;
import com.app.subly.model.enums.FontWeight;
import lombok.*;

import java.io.Serializable;

import static com.app.subly.persistence.AppSettingsIO.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublySettings implements Serializable {

    private String projectorImageUri;
    private BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;
    private String projectorColor = DEFAULT_PROJECTOR_COLOR;

    private String subtitleFontFamily = DEFAULT_SUBTITLE_FONT_FAMILY;
    private String subtitleColor = DEFAULT_SUBTITLE_COLOR;
    private Integer subtitleFontSize = DEFAULT_SUBTITLE_FONT_SIZE;
    private FontWeight fontWeight = DEFAULT_FONT_WEIGHT;
    private BorderWeight subtitleBorderWeight = DEFAULT_SUBTITLE_BORDER_WEIGHT;
    private String subtitleBorderColor = DEFAULT_SUBTITLE_BORDER_COLOR;

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
