module com.app.subly {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.desktop;

    opens com.app.subly.model to com.fasterxml.jackson.databind;
    opens com.app.subly to javafx.fxml;
    opens com.app.subly.controller to javafx.fxml;
    opens com.app.subly.controller.manager to javafx.fxml;
    opens com.app.subly.component to javafx.fxml;
    exports com.app.subly;
    exports com.app.subly.controller;
    exports com.app.subly.model;
    exports com.app.subly.persistence;
    exports com.app.subly.utils;
    exports com.app.subly.component;
    exports com.app.subly.controller.manager;
    exports com.app.subly.model.enums;
    opens com.app.subly.model.enums to com.fasterxml.jackson.databind;
}