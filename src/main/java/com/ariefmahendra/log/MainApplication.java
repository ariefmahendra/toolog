package com.ariefmahendra.log;

import com.ariefmahendra.log.controller.MainController;
import com.ariefmahendra.log.shared.util.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Log Application");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> {
            Network.disconnect();
            System.exit(0);
        });

        MainController mainController = fxmlLoader.getController();
        mainController.showSearchPage(null);
    }

    public static void main(String[] args) {
        launch();
    }
}