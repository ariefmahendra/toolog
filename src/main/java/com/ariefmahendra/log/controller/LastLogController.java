package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LogService;
import com.ariefmahendra.log.service.LogServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Font;


public class LastLogController {

    public Button wrapButton;
    public Button printButton;
    public Button clearButton;
    public TextArea logTextArea;
    public ProgressIndicator progressIndicator;
    public TextField fileNameTxt;

    public void printLatestLog(ActionEvent actionEvent){
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                wrapButton.setDisable(true);
                printButton.setDisable(true);
                clearButton.setDisable(true);
                progressIndicator.setVisible(true);
                logTextArea.setText("Loading...");
                LogService latestLogService = new LogServiceImpl();
                return latestLogService.getLatestLog();                }
        };

        task.setOnSucceeded(e -> {
            wrapButton.setDisable(false);
            printButton.setDisable(false);
            clearButton.setDisable(false);
            progressIndicator.setVisible(false);
            String latestLog = task.getValue();
            logTextArea.setText(latestLog);
            logTextArea.appendText("");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });

        task.setOnFailed(e -> {
            wrapButton.setDisable(false);
            printButton.setDisable(false);
            clearButton.setDisable(false);
            progressIndicator.setVisible(false);
            logTextArea.setText(task.getException().getMessage());
            task.getException().printStackTrace();
        });
        new Thread(task).start();
    }

    public void wrapLog(ActionEvent actionEvent) {
        logTextArea.setWrapText(!logTextArea.isWrapText());
    }

    public void clearLog(ActionEvent actionEvent) {
        logTextArea.clear();
    }
}
