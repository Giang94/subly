package com.app.subly.model.enums;

public enum BorderWeight {
    NONE(0, 0),
    NORMAL(2.5, 0.60),
    BOLD(4, 0.70),
    HEAVY(6, 0.75);

    private final double radius;
    private final double spread;

    public double getRadius() {
        return radius;
    }

    public double getSpread() {
        return spread;
    }

    public boolean isNone() {
        return this == NONE;
    }

    BorderWeight(double radius, double spread) {
        this.radius = radius;
        this.spread = spread;
    }
}
