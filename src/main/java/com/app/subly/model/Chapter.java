package com.app.subly.model;

import com.app.subly.component.ChapterBackground;

import java.util.ArrayList;
import java.util.List;

public class Chapter {
    private Integer id;
    private String title;
    private List<Subtitle> subtitles = new ArrayList<>();
    private ChapterBackground background = ChapterBackground.transparent();

    public Chapter() {
    }

    public Chapter(Integer id, String title) {
        this.id = id;
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Subtitle> getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(List<Subtitle> subtitles) {
        this.subtitles = (subtitles != null) ? subtitles : new ArrayList<>();
    }

    public ChapterBackground getBackground() {
        return background;
    }

    public void setBackground(ChapterBackground background) {
        this.background = (background != null) ? background : ChapterBackground.transparent();
    }

    @Override
    public String toString() {
        return this.title;
    }
}