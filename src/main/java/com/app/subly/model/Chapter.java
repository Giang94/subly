package com.app.subly.model;

import com.app.subly.component.ChapterBackground;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Chapter {

    private UUID id;
    private Integer index;
    private String title;
    private List<Subtitle> subtitles = new ArrayList<>();

    @JsonIgnore
    private ChapterBackground background = ChapterBackground.transparent();

    public Chapter() {
        ensureId();
    }

    public Chapter(UUID id, String title) {
        this.id = id;
        this.title = title;
    }


    public void setSubtitles(List<Subtitle> subtitles) {
        this.subtitles = (subtitles != null) ? subtitles : new ArrayList<>();
    }

    public void setBackground(ChapterBackground background) {
        this.background = (background != null) ? background : ChapterBackground.transparent();
    }

    @Override
    public String toString() {
        return this.title;
    }

    public void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}