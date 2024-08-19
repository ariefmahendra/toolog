package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.service.SearchLogService;
import com.ariefmahendra.log.service.SearchLogServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;


import java.util.LinkedList;

public class SearchLogController {
    @FXML
    public Button clearButton;
    public TextArea logTextArea;
    public Button wrapButton;
    public ProgressIndicator progressIndicator;
    public Button searchBtn;


    @FXML
    public void clearLog(ActionEvent actionEvent) {
        logTextArea.clear();
        logTextArea.setText("Not Found Log");
    }

    @FXML
    public void searchLog(ActionEvent actionEvent) {
        Task<LinkedList<String>> task = new Task<>() {
            @Override
            protected LinkedList<String> call() throws Exception {
                progressIndicator.setVisible(true);
                clearButton.setDisable(true);
                wrapButton.setDisable(true);
                searchBtn.setDisable(true);
                logTextArea.setText("Loading...");
                SearchLogService searchLogService = new SearchLogServiceImpl();
                LinkedList<String> resultLog;
                try {
                    resultLog = searchLogService.searchLogByKeyword("hello");
                }catch (GeneralException e){
                    throw e;
                }
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

            for (String logEntry : logResultFiltered) {
                logTextArea.appendText(logEntry + "\n");
            }
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
