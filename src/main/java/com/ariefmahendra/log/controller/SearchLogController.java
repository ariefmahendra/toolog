package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LogService;
import com.ariefmahendra.log.service.LogServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


import java.util.LinkedList;

public class SearchLogController {
    @FXML
    public Button clearButton;
    public TextArea logTextArea;
    public Button wrapButton;
    public ProgressIndicator progressIndicator;
    public Button searchBtn;
    public TextField keyTxt;


    @FXML
    public void clearLog(ActionEvent actionEvent) {
        logTextArea.clear();
        logTextArea.setText("Not Found Log");
    }

    @FXML
    public void searchLog(ActionEvent actionEvent) {
        Task<LinkedList<String> > task = new Task<>() {
            @Override
            protected LinkedList<String>  call() throws Exception {
                progressIndicator.setVisible(true);
                clearButton.setDisable(true);
                wrapButton.setDisable(true);
                searchBtn.setDisable(true);
                logTextArea.setText("Loading...");
                LogService searchLogService = new LogServiceImpl();
                LinkedList<String> resultLog;
                resultLog = searchLogService.searchLogByKeyword(keyTxt.getText());
                return resultLog;
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            clearButton.setDisable(false);
            wrapButton.setDisable(false);
            searchBtn.setDisable(false);
            LinkedList<String> logResultFiltered = task.getValue();
            logTextArea.clear();
            logResultFiltered.forEach(log -> logTextArea.appendText(log + "\n"));
        });

        task.setOnFailed(e -> {
            clearButton.setDisable(false);
            wrapButton.setDisable(false);
            searchBtn.setDisable(false);
            progressIndicator.setVisible(false);
            logTextArea.setText(task.getException().getMessage());
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    public void wrapLog(ActionEvent actionEvent) {
        logTextArea.setWrapText(!logTextArea.isWrapText());
    }
}
