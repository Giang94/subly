package com.app.subly.model;

public class Subtitle {
    private Integer id;
    private String text;

    public Subtitle(Integer id, String text) {
        this.id = id;
        this.text = text;
    }

    public Integer getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
