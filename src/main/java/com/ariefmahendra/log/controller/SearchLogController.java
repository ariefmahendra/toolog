package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LogService;
import com.ariefmahendra.log.service.LogServiceImpl;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;


import java.util.ArrayList;
import java.util.List;

public class SearchLogController {
    @FXML
    public Button clearButton;
    public TextArea logTextArea;
    public Button wrapButton;
    public ProgressIndicator progressIndicator;
    public Button searchBtn;
    public TextField keyTxt;

    private List<int[]> selectedRanges = new ArrayList<>();

    @FXML
    public void clearLog(ActionEvent actionEvent) {
        logTextArea.clear();
        logTextArea.setText("Not Found Log");
    }

    @FXML
    public void searchLog(ActionEvent actionEvent) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String>  call() throws Exception {
                progressIndicator.setVisible(true);
                clearButton.setDisable(true);
                wrapButton.setDisable(true);
                searchBtn.setDisable(true);
                logTextArea.setText("Loading...");
                LogService searchLogService = new LogServiceImpl();
                return searchLogService.searchLogByKeyword(keyTxt.getText());
            }
        };

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            clearButton.setDisable(false);
            wrapButton.setDisable(false);
            searchBtn.setDisable(false);
            List<String> logResultFiltered = task.getValue();
            logTextArea.clear();
            logResultFiltered.forEach(log -> logTextArea.appendText(log + "\n"));
            logTextArea.setScrollTop(Double.MAX_VALUE);
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

    public void initialize(){
        logTextArea.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isControlDown()){
                int caretPosition = logTextArea.getCaretPosition();
                logTextArea.getCursor();
                String text = logTextArea.getText();

                int start, end;
                start = text.lastIndexOf("\n\n", caretPosition - 1);
                end = text.indexOf("\n\n", caretPosition);

                if (start == -1) start = 0;
                if (end == -1) end = text.length();

                logTextArea.selectRange(start, end);
            }
        });
    }

    public void wrapLog(ActionEvent actionEvent) {
        logTextArea.setWrapText(!logTextArea.isWrapText());
    }
}
