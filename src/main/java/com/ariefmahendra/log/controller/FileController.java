package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.GUI;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.service.*;
import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.shared.dto.ListFileOrDirDto;
import com.ariefmahendra.log.shared.util.Downloader;
import com.ariefmahendra.log.shared.util.Reader;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class FileController {

    public ProgressIndicator progressIndicator;
    public Button backBtn;
    public TableView<FileOrDirModel> tableView;
    public TableColumn<FileOrDirModel, String> typeCol;
    public TableColumn<FileOrDirModel, String> nameCol;
    public TableColumn<FileOrDirModel, String> dirCol;
    public TableColumn<FileOrDirModel, String> dateCol;
    public TableColumn<FileOrDirModel, String> sizeCol;
    public Button refreshBtn;

    private String currentPath;
    private String parentPath;
    private final Logger logger = LoggerFactory.getLogger(FileController.class);

    public FileController() {

    }

    public void initialize() {
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        dirCol.setCellValueFactory(cellData -> cellData.getValue().directoryProperty());
        dateCol.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        sizeCol.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());

        FileService fileService = new FileServiceImpl();
        loadDir(fileService);

        tableView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                FileOrDirModel selectedItem = tableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String path = selectedItem.getDirectory();
                    if (selectedItem.getType().equals("File")){
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

                    nextOrBackDir(nextDirTask);
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

            nextOrBackDir(backDirTask);
        });

        refreshBtn.setOnAction(event -> loadDir(fileService));
    }

    private void nextOrBackDir(Task<ListFileOrDirDto> nextDirTask) {
        nextDirTask.setOnRunning(event -> {
            progressIndicator.setVisible(true);
            tableView.setDisable(true);
        });

        nextDirTask.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            tableView.setDisable(false);
            logger.error(nextDirTask.getException().getMessage(), nextDirTask.getException());
            Alert alert = new Alert(Alert.AlertType.ERROR, "error: " + nextDirTask.getException().getMessage());
            alert.showAndWait();
        });

        nextDirTask.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            tableView.setDisable(false);
            ListFileOrDirDto result = nextDirTask.getValue();
            tableView.getItems().clear();
            parentPath = result.getParentPath();
            ObservableList<FileOrDirModel> items = FXCollections.observableArrayList();
            addToTableTree(result, items);
        });

        new Thread(nextDirTask).start();
    }

    private void loadDir(FileService fileService){
        Task<ListFileOrDirDto> task = new Task<>() {
            @Override
            protected ListFileOrDirDto call() throws Exception {
                return fileService.getDir(Optional.ofNullable(currentPath));
            }
        };

        task.setOnRunning(event -> {
            progressIndicator.setVisible(true);
            tableView.setDisable(true);
            backBtn.setDisable(true);
            refreshBtn.setDisable(true);
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            tableView.setDisable(false);
            backBtn.setDisable(false);
            refreshBtn.setDisable(false);
            logger.error(task.getException().getMessage(), task.getException());
            Alert alert = new Alert(Alert.AlertType.ERROR, "error: " + task.getException().getMessage());
            alert.showAndWait();
        });

        task.setOnSucceeded(event -> {
            backBtn.setDisable(false);
            progressIndicator.setVisible(false);
            tableView.setDisable(false);
            refreshBtn.setDisable(false);
            ListFileOrDirDto result = task.getValue();
            ObservableList<FileOrDirModel> items = FXCollections.observableArrayList();
            parentPath = result.getParentPath();
            if (!result.getFiles().isEmpty()) {
                currentPath = result.getFiles().get(0).getDirectory();
            }

            addToTableTree(result, items);
        });

        new Thread(task).start();
    }

    private void addToTableTree(ListFileOrDirDto result, ObservableList<FileOrDirModel> items) {
        items.addAll(result.getDirectories().stream()
                .map(dir -> new FileOrDirModel("Directory", dir.getDirectory().substring(dir.getDirectory().lastIndexOf("/") + 1), dir.getDirectory(), dir.getDate(), dir.getSize()))
                .filter(item -> !item.getDirectory().contains("..") && !item.getDirectory().contains("."))
                .toList());

        items.addAll(result.getFiles().stream()
                .map(file -> new FileOrDirModel("File", file.getName(), file.getDirectory(), file.getDate(), file.getSize()))
                .toList());

        tableView.setItems(items);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ariefmahendra/log/pages/latest-view.fxml"));
                String pathFile = currentPath + name;
                initWindow(loader, event, pathFile);
            });

            searchByKeyBtn.setOnAction(event -> {
                // Todo: show search log page and passing path file to latest log page
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ariefmahendra/log/pages/search-view.fxml"));
                String pathFIle = currentPath + name;
                initWindow(loader, event, pathFIle);
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
                String fullPath = currentPath + name;
                DirectoryChooser directoryChooser = new DirectoryChooser();
                File selectedDirectory = directoryChooser.showDialog(modalStage);

                if (selectedDirectory != null) {
                    String chosenPath = selectedDirectory.getAbsolutePath();
                    String pathDownloaded = String.format("%s/%s", chosenPath, name);
                    System.out.println("File will be downloaded to: " + pathDownloaded);

                    Task<Void> downloadTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            Downloader.downloadFileByThread(fullPath, pathDownloaded);
                            return null;
                        }
                    };

                    Stage progressStage = new Stage();
                    progressStage.setTitle("Downloading...");

                    VBox downloadedVbox = new VBox(10);
                    downloadedVbox.setPadding(new Insets(20));
                    downloadedVbox.setAlignment(Pos.CENTER);

                    Label fileNameLabel = new Label("Downloading: " + name);
                    fileNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                    ProgressIndicator progressIndicator = new ProgressIndicator();
                    progressIndicator.setPrefSize(50, 50);
                    progressIndicator.setStyle("-fx-progress-color: #1E90FF;");

                    downloadedVbox.getChildren().addAll(fileNameLabel, progressIndicator);

                    Scene progressScene = new Scene(downloadedVbox, 300, 120);
                    progressStage.setScene(progressScene);
                    progressStage.show();

                    downloadTask.setOnRunning(e -> progressStage.show());

                    downloadTask.setOnFailed(e -> {
                        logger.error("Download failed: {}", downloadTask.getException().getMessage(), downloadTask.getException());
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Download failed: " + downloadTask.getException().getMessage());
                            alert.showAndWait();
                        });
                    });

                    downloadTask.setOnSucceeded(e -> Platform.runLater(() -> {
                        progressStage.close();
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Download completed successfully");
                        successAlert.show();
                    }));

                    // Start the task on a new thread
                    new Thread(downloadTask).start();
                } else {
                    System.out.println("No directory selected");
                }
            });

            Scene scene = new Scene(vbox, 200, 160);
            modalStage.setScene(scene);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setResizable(false);
            modalStage.setFullScreen(false);
            modalStage.setMaximized(false);
            modalStage.showAndWait();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void initWindow(FXMLLoader loader, ActionEvent event, String pathFile){
        try {
            Parent pageLoader = loader.load();
            Object controller = loader.getController();

            if (controller instanceof SearchLogController searchLogController){
                searchLogController.setOptionalPathFle(pathFile);
                String fileName = pathFile.substring(pathFile.lastIndexOf("/") + 1);
                searchLogController.fileNameTxt.setText(fileName);
            } else if (controller instanceof LastLogController lastLogController){
                lastLogController.setOptionalPathFle(pathFile);
                String fileName = pathFile.substring(pathFile.lastIndexOf("/") + 1);
                lastLogController.fileNameTxt.setText(fileName);
            }

            String iconPath = "/com/ariefmahendra/log/assets/icon/iconApp.png";
            Image appIcon = new Image(Objects.requireNonNull(GUI.class.getResourceAsStream(iconPath)));

            Stage stage = new Stage();
            stage.getIcons().add(appIcon);
            stage.setTitle("Log Viewer");

            stage.setScene(new Scene(pageLoader));

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());

            stage.setWidth(600);
            stage.setHeight(400);
            stage.setResizable(true);
            stage.showAndWait();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.showAndWait();
        }
    }

    public static class FileOrDirModel {
        private final StringProperty type;
        private final StringProperty name;
        private final StringProperty directory;
        private final StringProperty date;
        private final StringProperty size;

        public FileOrDirModel(String type, String name, String directory, String date, Long size) {
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.directory = new SimpleStringProperty(directory);
            this.date = new SimpleStringProperty(date);
            this.size = new SimpleStringProperty(Reader.formatFileSize(size));
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

        public StringProperty sizeProperty() {
            return size;
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

        public String getSize() {
            return size.get();
        }
    }
}
