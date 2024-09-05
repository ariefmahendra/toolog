package com.ariefmahendra.log;

import com.ariefmahendra.log.controller.MainController;
import com.ariefmahendra.log.shared.util.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

        MainController mainController = new MainController();
        mainController.showSearchPage(null);
    }

    public static void main(String[] args) {
        launch();
    }
}