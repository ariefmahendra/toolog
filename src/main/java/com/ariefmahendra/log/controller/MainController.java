package com.ariefmahendra.log.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    public StackPane contentArea;

    public void showSearchPage(ActionEvent actionEvent) {
        showSearch();
    }

    public void showLatestPage(ActionEvent actionEvent) {
        showLatest();
    }

    public void showListPage(ActionEvent actionEvent) {
        showDirectory();
    }

    public void showSettingsPage(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ariefmahendra/log/pages/settings-view.fxml"));
            Parent root = loader.load();

            // Create a new stage (window)
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");

            // Set the scene with the loaded FXML root
            settingsStage.setScene(new Scene(root));

            // Set modality to block interaction with the main window
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(((Node) actionEvent.getSource()).getScene().getWindow());

            // Optional: Set window size or other properties
            settingsStage.setWidth(600);
            settingsStage.setHeight(400);
            settingsStage.setResizable(false);

            // Show the new window and wait until it's closed before returning to the main window
            settingsStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadChildren(FXMLLoader page) {
        try {
            Parent root = page.load();

            // Clear existing children and add the new root node
            contentArea.getChildren().add(root);

            // Set anchors for the root node
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showSearch(){
        FXMLLoader searchPage = new FXMLLoader(getClass().getResource("/com/ariefmahendra/log/pages/search-view.fxml"));
        loadChildren(searchPage);
    }

    private void showLatest(){
        FXMLLoader latestPage = new FXMLLoader(getClass().getResource("/com/ariefmahendra/log/pages/latest-view.fxml"));
        loadChildren(latestPage);
    }

    private void showDirectory(){
        FXMLLoader filePage = new FXMLLoader(getClass().getResource("/com/ariefmahendra/log/pages/file-view.fxml"));
        loadChildren(filePage);
    }
}
