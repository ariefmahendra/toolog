package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.service.FileService;
import com.ariefmahendra.log.service.FileServiceImpl;
import com.ariefmahendra.log.shared.dto.ListFileOrDirDto;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileController {

    public ListView<String> fileListView;
    public ProgressIndicator progressIndicator;
    public Button backBtn;
    private static String currentPath;

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public void initialize() {
        Task<ListFileOrDirDto> task = new Task<>() {
            @Override
            protected ListFileOrDirDto call() throws Exception {
                FileService fileService = new FileServiceImpl();
                return fileService.getDir();
            }
        };

        task.setOnRunning(event -> {
            progressIndicator.setVisible(true);
            fileListView.setDisable(true);
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            fileListView.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error Cause = " + task.getMessage());
            alert.showAndWait();
        });

        task.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            fileListView.setDisable(false);
            ListFileOrDirDto result = task.getValue();
            fileListView.getItems().clear();

            if (!result.getFiles().isEmpty()) {
                currentPath = result.getFiles().get(0).getDirectory();
            }

            fileListView.getItems().addAll(result.getDirectories().stream()
                    .map(item -> "Directory = " + item.getDirectory())
                    .filter(item -> !item.contains("..") && !item.contains("."))
                    .toList());

            fileListView.getItems().addAll(result.getFiles().stream()
                    .map(file -> "File = " + file.getDirectory())
                    .toList());
        });

        new Thread(task).start();

        // handle on double click
        fileListView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Double click to open file");
                alert.showAndWait();
            }
            if (mouseEvent.getClickCount() == 2){
                Task<ListFileOrDirDto> nextDirTask = new Task<>() {
                    @Override
                    protected ListFileOrDirDto call() throws Exception {
                        String lastPath = fileListView.getSelectionModel().getSelectedItem();
                        String newPath = currentPath + "/" + lastPath;

                        boolean isfile = newPath.startsWith("File");
                        if (isfile){
                            return null;
                        }

                        int startIndex = newPath.indexOf("=");
                        String absolutePath = newPath.substring(startIndex + 2);
                        logger.debug("absolutePath = {}", absolutePath);
                        FileService fileService = new FileServiceImpl();
                        return fileService.getNextDir(absolutePath + "/");
                    }
                };

                nextDirTask.setOnRunning(event -> {
                    progressIndicator.setVisible(true);
                    fileListView.setDisable(true);
                });


                nextDirTask.setOnFailed(event -> {
                    progressIndicator.setVisible(false);
                    fileListView.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error Cause = " + nextDirTask.getMessage());
                    alert.showAndWait();
                });
                
                nextDirTask.setOnSucceeded(event -> {
                    progressIndicator.setVisible(false);
                    fileListView.setDisable(false);
                    ListFileOrDirDto result = nextDirTask.getValue();
                    fileListView.getItems().clear();
                    if (result.getFiles().isEmpty()) {
                        if (!result.getDirectories().isEmpty()) {
                            currentPath = result.getDirectories().get(0).getDirectory();
                        }
                    }else {
                        currentPath = result.getFiles().get(0).getDirectory();
                    }

                    fileListView.getItems().addAll(result.getDirectories().stream()
                            .map(item -> "Directory = " + item.getDirectory())
                            .filter(item -> !item.contains("..") && !item.contains("."))
                            .toList());

                    fileListView.getItems().addAll(result.getFiles().stream()
                            .map(file -> "File = " + file.getDirectory())
                            .toList());
                });
                new Thread(nextDirTask).start();
            }
        });

        // handle on back btn
        backBtn.setOnMouseClicked(mouseEvent -> {
            Task<ListFileOrDirDto> backDirTask = new Task<>() {
                @Override
                protected ListFileOrDirDto call() throws Exception {
                    long count = currentPath.chars().filter(c -> c == '/').count();
                    if (count == 1) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "This is root directory");
                        alert.showAndWait();
                    }
                    int i = currentPath.lastIndexOf("/");
                    logger.debug("start index = {}", i);
                    String lastPath;
                    if (i == 1){
                        lastPath = "/";
                    } else {
                        lastPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                    }
                    logger.debug("lastPath = {}", lastPath);
                    FileService fileService = new FileServiceImpl();
                    return fileService.getBackDir(lastPath);
                }
            };

            backDirTask.setOnRunning(event -> {
                progressIndicator.setVisible(true);
                fileListView.setDisable(true);
            });

            backDirTask.setOnFailed(event -> {
                progressIndicator.setVisible(false);
                fileListView.setDisable(false);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error Cause = " + backDirTask.getMessage());
                alert.showAndWait();
            });

            backDirTask.setOnSucceeded(event -> {
                progressIndicator.setVisible(false);
                fileListView.setDisable(false);
                ListFileOrDirDto result = backDirTask.getValue();
                fileListView.getItems().clear();
                if (result.getFiles().isEmpty()) {
                    currentPath = result.getDirectories().get(0).getDirectory();
                } else {
                    currentPath = result.getFiles().get(0).getDirectory();
                }
                fileListView.getItems().addAll(result.getDirectories().stream()
                        .map(item -> "Directory = " + item.getDirectory())
                        .filter(item -> !item.contains("..") && !item.contains("."))
                        .toList());

                fileListView.getItems().addAll(result.getFiles().stream()
                        .map(file -> "File = " + file.getDirectory())
                        .toList());
            });
            new Thread(backDirTask).start();
        });
    }
}
