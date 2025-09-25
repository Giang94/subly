package com.app.subly.component;

public class ProjectorRef {
    private Projector projector;

    public Projector get() {
        return projector;
    }

    public void set(Projector projector) {
        this.projector = projector;
    }

    public boolean isVisible() {
        return projector != null && projector.isVisible();
    }

    public void showEnsure() {
        if (projector == null) {
            projector = new Projector();
        }
        projector.show();
    }

    public void hideIfVisible() {
        if (projector != null && projector.isVisible()) {
            projector.hide();
        }
    }
}