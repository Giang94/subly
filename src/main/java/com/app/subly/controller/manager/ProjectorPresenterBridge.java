package com.app.subly.controller.manager;

import com.app.subly.component.Projector;
import com.app.subly.model.SublySettings;

public class ProjectorPresenterBridge implements PresentingModeNavigator.Listener {

    private final Projector projector;
    private final SublySettings settings;

    public ProjectorPresenterBridge(Projector projector, SublySettings settings) {
        this.projector = projector;
        this.settings = settings;
    }

    @Override
    public void onPositionChanged(int chapterIndex, int rowIndex) {
        projector.show(settings, chapterIndex, rowIndex);
    }

    @Override
    public void onEndReached() {
        // Optional: show end indicator
    }

    @Override
    public void onStartReached() {
        // Optional: feedback at start
    }
}