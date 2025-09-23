package com.app.subly.component;

import com.app.subly.utils.AppIconUtils;
import javafx.stage.Stage;

public class SublyApplicationStage extends Stage {

    public SublyApplicationStage() {
        super();
        getIcons().add(AppIconUtils.getAppIcon());
    }
}