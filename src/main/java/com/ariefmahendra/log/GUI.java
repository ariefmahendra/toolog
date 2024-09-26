package com.ariefmahendra.log;

import com.ariefmahendra.log.controller.MainController;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;
import com.ariefmahendra.log.shared.util.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class GUI extends Application {
    private static final Logger log = LoggerFactory.getLogger(GUI.class);

    private Logger logger = LoggerFactory.getLogger(GUI.class);

    @Override
    public void start(Stage stage) throws IOException {
        if (isReleaseMode()) {
            clearPreferences();
        }

        FXMLLoader fxmlLoader = new FXMLLoader(GUI.class.getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 700, 400);
        stage.setTitle("Log Application");

        String iconPath = "/com/ariefmahendra/log/assets/icon/iconApp.png";
        try {
            Image appIcon = new Image(Objects.requireNonNull(GUI.class.getResourceAsStream(iconPath)));

            stage.getIcons().add(appIcon);
            stage.setScene(scene);
            stage.show();

            stage.setOnCloseRequest(event -> {
                Network.disconnect();
                System.exit(0);
            });

            MainController mainController = fxmlLoader.getController();
            mainController.showSearch();
        } catch (NullPointerException e){
            log.error("Icon Path is not ready!");
        }
    }

    private void clearPreferences() {
        try {
            Preferences sftpPref = Preferences.userNodeForPackage(SftpModel.class);
            sftpPref.clear();
            Preferences logPref = Preferences.userNodeForPackage(LogModel.class);
            logPref.clear();
            System.out.println("Preferences cleared for release");
        } catch (Exception e) {
            logger.error("Failed to clear preferences", e);
        }
    }

    private boolean isReleaseMode() {
        String mode = System.getProperty("mode");
        return "release".equalsIgnoreCase(mode);
    }

}