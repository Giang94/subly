package com.app.subly.component;

import com.app.subly.SublyApplication;
import com.app.subly.controller.ControlPanelController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class ControlPanel {
    private Scene scene;

    public ControlPanel(SublyApplication app, Projector projector) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/control_panel_view.fxml"));
        Parent root = loader.load();
        scene = new Scene(root);

        // wire ShowScreen into controller
        ControlPanelController controller = loader.getController();
        controller.setShowScreen(app, projector);
    }

    public Scene getScene() {
        return scene;
    }
}
