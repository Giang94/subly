package com.app.subly.utils;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class DialogHelper {

    private DialogHelper() {
    }

    public static void showError(Stage owner,
                                 String title,
                                 String header,
                                 String message) {
        runFx(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (owner != null) alert.initOwner(owner);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(message);
            // Center after size is known
            alert.setOnShown(e -> centerOnOwner(alert, owner));
            alert.showAndWait();
        });
    }

    public static void showImageLoadFailure(Stage owner, String attemptedUri) {
        showError(owner,
                "Image Load Failed",
                "Could not load background image",
                "Path:\n" + attemptedUri);
    }

    private static void centerOnOwner(Alert alert, Stage owner) {
        Window dialogWindow = alert.getDialogPane().getScene().getWindow();
        if (owner != null) {
            double x = owner.getX() + (owner.getWidth() - dialogWindow.getWidth()) / 2.0;
            double y = owner.getY() + (owner.getHeight() - dialogWindow.getHeight()) / 2.0;
            dialogWindow.setX(x);
            dialogWindow.setY(y);
        } else {
            Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            double x = vb.getMinX() + (vb.getWidth() - dialogWindow.getWidth()) / 2.0;
            double y = vb.getMinY() + (vb.getHeight() - dialogWindow.getHeight()) / 2.0;
            dialogWindow.setX(x);
            dialogWindow.setY(y);
        }
    }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}