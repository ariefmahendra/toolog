package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.LogService;
import com.ariefmahendra.log.service.LogServiceImpl;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import com.ariefmahendra.log.shared.dto.CredentialsDto;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SearchLogController {
    @FXML
    public Button clearButton;
    public TextArea logTextArea;
    public Button wrapButton;
    public ProgressIndicator progressIndicator;
    public Button searchBtn;
    public TextField keyTxt;
    public TextField fileNameTxt;

    private String optionalPathFle;
    private double currentFontSize = 10;
    private Logger logger = LoggerFactory.getLogger(SearchLogController.class);

    public void setOptionalPathFle(String optionalPathFle) {
        this.optionalPathFle = optionalPathFle;
    }

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
                fileNameTxt.setDisable(true);
                progressIndicator.setVisible(true);
                clearButton.setDisable(true);
                wrapButton.setDisable(true);
                searchBtn.setDisable(true);
                logTextArea.setText("Loading...");
                LogService searchLogService = new LogServiceImpl();
                return searchLogService.searchLogByKeyword(keyTxt.getText(), optionalPathFle);
            }
        };

        task.setOnSucceeded(e -> {
            fileNameTxt.setDisable(false);
            progressIndicator.setVisible(false);
            clearButton.setDisable(false);
            wrapButton.setDisable(false);
            searchBtn.setDisable(false);

            List<String> logResultFiltered = task.getValue();
            logTextArea.clear();
            logResultFiltered.forEach(log -> logTextArea.appendText(log + "\n"));
            logTextArea.setScrollTop(Double.MAX_VALUE);

            currentFontSize = 10;
            logTextArea.setStyle("-fx-font-size: " + currentFontSize + "px;");
            zoomEvent();
        });

        task.setOnFailed(e -> {
            fileNameTxt.setDisable(false);
            clearButton.setDisable(false);
            wrapButton.setDisable(false);
            searchBtn.setDisable(false);
            progressIndicator.setVisible(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "error: " + task.getException().getMessage());
            alert.showAndWait();
            logger.error("error: {}", task.getException().getMessage(), task.getException());
        });

        new Thread(task).start();
    }

    public void initialize(){
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        String filePath = credentials.getLog().getDirectory();
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        fileNameTxt.setText(fileName);

        zoomEvent();

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

    public void wrapLog(ActionEvent actionEvent) {
        logTextArea.setWrapText(!logTextArea.isWrapText());
    }
}
