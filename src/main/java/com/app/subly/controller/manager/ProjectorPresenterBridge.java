package com.app.subly.controller.manager;

import com.app.subly.component.Projector;
import com.app.subly.model.Chapter;
import com.app.subly.model.SublySettings;
import com.app.subly.model.Subtitle;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;

public class ProjectorPresenterBridge implements PresentingModeNavigator.Listener {

    private final Projector projector;
    private final SublySettings settings;
    ListView<Chapter> chapterListView;
    TableView<Subtitle> subtitleTable;

    public ProjectorPresenterBridge(Projector projector,
                                    SublySettings settings,
                                    ListView<Chapter> chapterListView,
                                    TableView<Subtitle> subtitleTable
    ) {
        this.projector = projector;
        this.settings = settings;
        this.chapterListView = chapterListView;
        this.subtitleTable = subtitleTable;
    }

    @Override
    public void onPositionChanged(int chapterIndex, int rowIndex) {
        projector.show();
        chapterListView.getSelectionModel().select(chapterIndex);
        subtitleTable.getSelectionModel().select(rowIndex);
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