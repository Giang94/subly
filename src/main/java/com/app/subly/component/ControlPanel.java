package com.app.subly.component;

import com.app.subly.SublyApplication;
import com.app.subly.controller.migrate.ControlPanelController;
import com.app.subly.project.SublyProjectSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter
public class ControlPanel {
    private Scene scene;
    private final SublyProjectSession session;

    public ControlPanel(SublyApplication app, Projector projector) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/control_panel_view.fxml"));
        Parent root = loader.load();
        scene = new Scene(root);

        // wire ShowScreen into controller
        ControlPanelController controller = loader.getController();
        controller.setShowScreen(app, projector);

        this.session = new SublyProjectSession();
        this.session.ensureAtLeastOneChapter();
        controller.setSession(session);
    }
}
