package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LogService;
import com.ariefmahendra.log.service.LogServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    public void initialize() {
        logTextArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {

                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Search");
                dialog.setHeaderText("Enter the word or phrase to search");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(this::findAndSelectString);
                event.consume();
            }
        });
    }

    private void findAndSelectString(String lookingFor)
    {
        Pattern pattern = Pattern.compile("\\b" + lookingFor + "\\b");
        Matcher matcher = pattern.matcher(logTextArea.getText());
        boolean found = matcher.find(0);
        if(found){
            logTextArea.selectRange(matcher.start(), matcher.end());
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search");
            alert.setHeaderText(null);
            alert.setContentText("Text not found!");
            alert.showAndWait();
        }
    }

    public void wrapLog(ActionEvent actionEvent) {
        logTextArea.setWrapText(!logTextArea.isWrapText());
    }

    public void clearLog(ActionEvent actionEvent) {
        logTextArea.clear();
    }
}
