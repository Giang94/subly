package com.app.subly.component;

import com.app.subly.SublyApplication;
import com.app.subly.controller.ControlPanelController;
import com.app.subly.project.SublyProjectSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class ControlPanel {
    private Scene scene;
    private final SublyProjectSession session;

//    public ControlPanel() throws IOException {
//        this.session = SublyProjectSession.newSessionWithDefaults();
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("/control_panel_view.fxml"));
//        Parent root = loader.load();
//        this.scene = new Scene(root);
//
//        ControlPanelController controller = loader.getController();
//        controller.setSession(session);
//    }

    public ControlPanel(SublyApplication app, Projector projector) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/control_panel_view.fxml"));
        Parent root = loader.load();
        scene = new Scene(root);

        // wire ShowScreen into controller
        ControlPanelController controller = loader.getController();
        controller.setShowScreen(app, projector);

        this.session = SublyProjectSession.newSessionWithDefaults();
        controller.setSession(session);
    }

    public Scene getScene() {
        return scene;
    }

    public SublyProjectSession getSession() {
        return session;
    }
}
