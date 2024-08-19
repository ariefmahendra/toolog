package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LatestLogService;
import com.ariefmahendra.log.service.LatestLogServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


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
                LatestLogService latestLogService = new LatestLogServiceImpl();
                return latestLogService.getLatestLog();                }
        };

        task.setOnSucceeded(e -> {
            wrapButton.setDisable(false);
            printButton.setDisable(false);
            clearButton.setDisable(false);
            progressIndicator.setVisible(false);
            String latestLog = task.getValue();
            logTextArea.setText(latestLog);
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
