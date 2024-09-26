package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LogService;
import com.ariefmahendra.log.service.LogServiceImpl;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import com.ariefmahendra.log.shared.dto.CredentialsDto;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LastLogController {

    public Button wrapButton;
    public Button printButton;
    public Button clearButton;
    public TextArea logTextArea;
    public ProgressIndicator progressIndicator;
    public TextField fileNameTxt;

    private String optionalPathFle;
    private double currentFontSize = 10;
    Logger logger = LoggerFactory.getLogger(LastLogController.class);

    public void setOptionalPathFle(String optionalPathFle) {
        this.optionalPathFle = optionalPathFle;
    }

    public void printLatestLog(ActionEvent actionEvent){
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                fileNameTxt.setDisable(true);
                wrapButton.setDisable(true);
                printButton.setDisable(true);
                clearButton.setDisable(true);
                progressIndicator.setVisible(true);
                logTextArea.setText("Loading...");
                LogService latestLogService = new LogServiceImpl();
                return latestLogService.getLatestLog(optionalPathFle);                }
        };

        task.setOnSucceeded(e -> {
            fileNameTxt.setDisable(false);
            wrapButton.setDisable(false);
            printButton.setDisable(false);
            clearButton.setDisable(false);
            progressIndicator.setVisible(false);

            String latestLog = task.getValue();
            logTextArea.setText(latestLog);
            logTextArea.appendText("");
            logTextArea.setScrollTop(Double.MAX_VALUE);

            currentFontSize = 10;
            logTextArea.setStyle("-fx-font-size: " + currentFontSize + "px;");
            zoomEvent();
        });

        task.setOnFailed(e -> {
            fileNameTxt.setDisable(false);
            wrapButton.setDisable(false);
            printButton.setDisable(false);
            clearButton.setDisable(false);
            progressIndicator.setVisible(false);
            logTextArea.clear();
            Alert alert = new Alert(Alert.AlertType.ERROR, "error: " + task.getException().getMessage());
            alert.showAndWait();
            logger.error(task.getException().getMessage(), task.getException());
        });
        new Thread(task).start();
    }

    private void zoomEvent() {
        logTextArea.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            logTextArea.requestFocus();
            if (new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN).match(keyEvent)) {
                currentFontSize += 1;
                logTextArea.setStyle("-fx-font-size: " + currentFontSize + "px;");
            } else if (new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN).match(keyEvent)) {
                if (currentFontSize > 10) {
                    currentFontSize -= 1;
                    logTextArea.setStyle("-fx-font-size: " + currentFontSize + "px;");
                }
            }
        });
    }

    public void initialize() {
        if (optionalPathFle != null) {
            String fileName = optionalPathFle.substring(optionalPathFle.lastIndexOf("/") + 1);
            fileNameTxt.setText(fileName);
        } else {
            SettingsService settingsService = new SettingsServiceImpl();
            CredentialsDto credentials = settingsService.getCredentials();
            String filePath = credentials.getLog().getDirectory();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            fileNameTxt.setText(fileName);
        }

        zoomEvent();
    }


    public void wrapLog(ActionEvent actionEvent) {
        logTextArea.setWrapText(!logTextArea.isWrapText());
    }

    public void clearLog(ActionEvent actionEvent) {
        logTextArea.clear();
    }
}
