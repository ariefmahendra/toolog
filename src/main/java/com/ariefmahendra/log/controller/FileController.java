package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.model.DirectoryModel;
import com.ariefmahendra.log.model.FileModel;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.service.FileService;
import com.ariefmahendra.log.service.FileServiceImpl;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.shared.dto.ListFileOrDirDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

public class FileController {

    public ProgressIndicator progressIndicator;
    public Button backBtn;
    public Button sortingBtn;
    public TableView<FileOrDirModel> tableView;
    public TableColumn<FileOrDirModel, String> typeCol;
    public TableColumn<FileOrDirModel, String> nameCol;
    public TableColumn<FileOrDirModel, String> dirCol;
    public TableColumn<FileOrDirModel, String> dateCol;

    private static String currentPath;
    private static String selectedFile;
    private static String parentPath;
    private static Task<ListFileOrDirDto> task;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public void initialize() {
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        dirCol.setCellValueFactory(cellData -> cellData.getValue().directoryProperty());
        dateCol.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        FileService fileService = new FileServiceImpl();
        loadDir(fileService);

        tableView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                FileOrDirModel selectedItem = tableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String path = selectedItem.getDirectory();
                    if (selectedItem.getType().equals("File")){
                        selectedFile = currentPath + "/" + selectedItem.getName();
                        showModalWindow(mouseEvent, selectedItem.getName());
                        return;
                    }

                    Task<ListFileOrDirDto> nextDirTask = new Task<>() {
                        @Override
                        protected ListFileOrDirDto call() throws Exception {
                            String nextPath = path + "/";
                            currentPath = nextPath;
                            return fileService.getNextDir(nextPath);
                        }
                    };

                    nextDirTask.setOnRunning(event -> {
                        progressIndicator.setVisible(true);
                        tableView.setDisable(true);
                    });

                    nextDirTask.setOnFailed(event -> {
                        progressIndicator.setVisible(false);
                        tableView.setDisable(false);
                        Throwable exception = task.getException();
                        if (exception != null) {
                            exception.printStackTrace();
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error to next directory");
                            alert.showAndWait();
                        } else {
                            System.out.println("Task failed but no exception was thrown.");
                        }
                    });

                    nextDirTask.setOnSucceeded(event -> {
                        progressIndicator.setVisible(false);
                        tableView.setDisable(false);
                        ListFileOrDirDto result = nextDirTask.getValue();
                        tableView.getItems().clear();
                        parentPath = result.getParentPath();
                        ObservableList<FileOrDirModel> items = FXCollections.observableArrayList();
                        items.addAll(result.getDirectories().stream()
                                .map(dir -> new FileOrDirModel("Directory", dir.getDirectory(), dir.getDirectory(), dir.getDate().toString()))
                                .filter(item -> !item.getDirectory().contains("..") && !item.getDirectory().contains("."))
                                .toList());

                        items.addAll(result.getFiles().stream()
                                .map(file -> new FileOrDirModel("File", file.getName(), file.getDirectory(), file.getDate().toString()))
                                .toList());

                        tableView.setItems(items);
                    });

                    new Thread(nextDirTask).start();
                }
            }
        });

        backBtn.setOnMouseClicked(mouseEvent -> {
            Task<ListFileOrDirDto> backDirTask = new Task<>() {
                @Override
                protected ListFileOrDirDto call() throws Exception {
                    currentPath = parentPath;
                    return fileService.getBackDir(parentPath);
                }
            };

            backDirTask.setOnRunning(event -> {
                progressIndicator.setVisible(true);
                tableView.setDisable(true);
            });

            backDirTask.setOnFailed(event -> {
                progressIndicator.setVisible(false);
                tableView.setDisable(false);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error Cause = " + task.getException().getMessage());
                task.getException().printStackTrace();
                alert.showAndWait();
            });

            backDirTask.setOnSucceeded(event -> {
                progressIndicator.setVisible(false);
                tableView.setDisable(false);
                ListFileOrDirDto result = backDirTask.getValue();
                tableView.getItems().clear();
                parentPath = result.getParentPath();
                ObservableList<FileOrDirModel> items = FXCollections.observableArrayList();
                items.addAll(result.getDirectories().stream()
                        .map(dir -> new FileOrDirModel("Directory", dir.getDirectory(), dir.getDirectory(), dir.getDate().toString()))
                        .filter(item -> !item.getDirectory().contains("..") && !item.getDirectory().contains("."))
                        .toList());

                items.addAll(result.getFiles().stream()
                        .map(file -> new FileOrDirModel("File", file.getName(), file.getDirectory(), file.getDate().toString()))
                        .toList());

                tableView.setItems(items);
            });

            new Thread(backDirTask).start();
        });

        sortingBtn.setOnAction(event -> {
            if (sortingBtn.getText().equals("Sorting by Date")) {
                loadDir(fileService);
                sortingBtn.setText("Dont Sorting");
                return;
            }

            if (sortingBtn.getText().equals("Dont Sorting")) {
                loadDir(fileService);
                sortingBtn.setText("Sorting by Date");
            }
        });
    }

    private void loadDir(FileService fileService){
        task = new Task<>() {
            @Override
            protected ListFileOrDirDto call() throws Exception {
                return fileService.getDir(Optional.ofNullable(currentPath));
            }
        };

        task.setOnRunning(event -> {
            progressIndicator.setVisible(true);
            tableView.setDisable(true);
            sortingBtn.setDisable(true);
            backBtn.setDisable(true);
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            tableView.setDisable(false);
            backBtn.setDisable(false);
            sortingBtn.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error Cause = " + task.getException().getMessage());
            task.getException().printStackTrace();
            alert.showAndWait();
        });

        task.setOnSucceeded(event -> {
            sortingBtn.setDisable(false);
            backBtn.setDisable(false);
            progressIndicator.setVisible(false);
            tableView.setDisable(false);
            ListFileOrDirDto result = task.getValue();
            ObservableList<FileOrDirModel> items = FXCollections.observableArrayList();

            parentPath = result.getParentPath();

            if (!result.getFiles().isEmpty()) {
                currentPath = result.getFiles().get(0).getDirectory();
            }

            if (sortingBtn.getText().equals("Sorting by Date")) {
                items.addAll(result.getDirectories().stream()
                        .map(dir -> new FileOrDirModel("Directory", dir.getDirectory(), dir.getDirectory(), dir.getDate().toString()))
                        .filter(item -> !item.getDirectory().contains("..") && !item.getDirectory().contains("."))
                        .toList());

                items.addAll(result.getFiles().stream()
                        .map(file -> new FileOrDirModel("File", file.getName(), file.getDirectory(), file.getDate().toString()))
                        .toList());
            } else {
                items.addAll(result.getDirectories().stream()
                        .sorted(Comparator.comparing(DirectoryModel::getDate).reversed())
                        .map(dir -> new FileOrDirModel("Directory", dir.getDirectory(), dir.getDirectory(), dir.getDate().toString()))
                        .filter(item -> !item.getDirectory().contains("..") && !item.getDirectory().contains("."))
                        .toList());

                items.addAll(result.getFiles().stream()
                        .sorted(Comparator.comparing(FileModel::getDate).reversed())
                        .map(file -> new FileOrDirModel("File", file.getName(), file.getDirectory(), file.getDate().toString()))
                        .toList());
            }

            tableView.setItems(items);
        });

        new Thread(task).start();
    }

    private void showModalWindow(MouseEvent mouseEvent, String name) {
        try {
            Stage modalStage = new Stage();
            modalStage.setTitle("options");
            modalStage.initModality(Modality.APPLICATION_MODAL);
            Stage primaryStage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            modalStage.initOwner(primaryStage);
            VBox vbox = new VBox(5);
            vbox.setStyle("-fx-padding: 5;");

            TextField fileNameTxt = new TextField(name);
            fileNameTxt.setEditable(false);
            fileNameTxt.setFocusTraversable(false);

            Button latestLogBtn = new Button("Latest Log");
            latestLogBtn.setFocusTraversable(false);

            Button searchByKeyBtn = new Button("Search");
            searchByKeyBtn.setFocusTraversable(false);

            Button setDefaultFileTxt = new Button("Set Default File");
            setDefaultFileTxt.setFocusTraversable(false);

            Button downloadFileBtn = new Button("Download File");
            downloadFileBtn.setFocusTraversable(false);

            vbox.getChildren().addAll(fileNameTxt, latestLogBtn, searchByKeyBtn, setDefaultFileTxt, downloadFileBtn);

            latestLogBtn.setOnAction(event -> {
                // Todo: show latest log page and passing path file to latest log page
            });

            searchByKeyBtn.setOnAction(event -> {
                // Todo: show search log page and passing path file to latest log page
            });

            setDefaultFileTxt.setOnAction(event -> {
                // Todo: set default file path
                SettingsService settingsService = new SettingsServiceImpl();
                CredentialsDto credentials = settingsService.getCredentials();

                String fullPath = currentPath + name;

                LogModel log = credentials.getLog();
                log.setDirectory(fullPath);

                settingsService.settingCredentials(log, credentials.getSftp());
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Success");
                successAlert.showAndWait();
            });

            downloadFileBtn.setOnAction(event -> {
                // Todo: download file by threading
            });

            Scene scene = new Scene(vbox, 200, 160);
            modalStage.setScene(scene);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setResizable(false);
            modalStage.setFullScreen(false);
            modalStage.setMaximized(false);
            modalStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class FileOrDirModel {
        private final StringProperty type;
        private final StringProperty name;
        private final StringProperty directory;
        private final StringProperty date;

        public FileOrDirModel(String type, String name, String directory, String date) {
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.directory = new SimpleStringProperty(directory);
            this.date = new SimpleStringProperty(date);
        }

        public StringProperty typeProperty() {
            return type;
        }

        public StringProperty nameProperty() {
            return name;
        }

        public StringProperty directoryProperty() {
            return directory;
        }

        public StringProperty dateProperty() {
            return date;
        }

        public String getType() {
            return type.get();
        }

        public String getName() {
            return name.get();
        }

        public String getDirectory() {
            return directory.get();
        }

        public String getDate() {
            return date.get();
        }
    }
}
