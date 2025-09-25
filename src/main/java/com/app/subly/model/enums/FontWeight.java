package com.app.subly.model.enums;

public enum FontWeight {
    LIGHT(200),
    NORMAL(400),
    BOLD(700),
    HEAVY(900);

    private final int weightValue;

    FontWeight(int weightValue) {
        this.weightValue = weightValue;
    }

    public int getWeightValue() {
        return weightValue;
    }
}
