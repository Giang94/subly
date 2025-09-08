package com.app.subly.model;

public class Subtitle {
    private Integer id;
    private String primaryText;
    private String secondaryText;

    public Subtitle(Integer id, String primaryText, String secondaryText) {
        this.id = id;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }

    public String getSecondaryText() {
        return secondaryText;
    }

    public void setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
    }
}
