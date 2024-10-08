package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import com.ariefmahendra.log.shared.util.Network;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;

public class SettingsController {
    public TextField hostTxt;
    public TextField usernameTxt;
    public TextField passwordTxt;
    public TextField defaultFileTxt;
    public TextField portTxt;
    public Button saveSettingsBtn;
    public TextField bufferSizeTxt;
    public Button connectBtn;
    public Button resetBtn;

    private SettingsService settingsService;
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    public void initialize() {
        bufferSizeTxt.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("\\d*")) {
                return change;
            }
            return null;
        }));

        portTxt.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("\\d*")) {
                return change;
            }

            return null;
        }));

        settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        hostTxt.setText(credentials.getSftp().getRemoteHost());
        usernameTxt.setText(credentials.getSftp().getUsername());
        passwordTxt.setText(credentials.getSftp().getPassword());
        portTxt.setText(credentials.getSftp().getPort());
        defaultFileTxt.setText(credentials.getLog().getDirectory());
        bufferSizeTxt.setText(credentials.getLog().getBufferSize());
    }

    public void saveCredentials(ActionEvent actionEvent) {

        String host = hostTxt.getText();
        String port = portTxt.getText();
        String username = usernameTxt.getText();
        String password = passwordTxt.getText();
        String defaultFolder = defaultFileTxt.getText();
        String bufferSize = bufferSizeTxt.getText();

        LogModel logModel = new LogModel(defaultFolder, bufferSize);
        SftpModel sftpModel = new SftpModel(username, host, password, port);

        try {
            if (host.isEmpty() || port.isEmpty() || username.isEmpty() || password.isEmpty() || bufferSize.isEmpty()){
                Alert failedAlert = new Alert(Alert.AlertType.ERROR, "Please fill all fields");
                failedAlert.showAndWait();
                return;
            }

            Network.resetSession();
            Network.setupJsch(usernameTxt.getText(), hostTxt.getText(), passwordTxt.getText(), Integer.parseInt(portTxt.getText()));
            settingsService.settingCredentials(logModel, sftpModel);
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Success save credentials");
            successAlert.showAndWait();
        } catch (Exception e){
            Alert failedAlert = new Alert(Alert.AlertType.ERROR, "Can't connect server");
            failedAlert.showAndWait();
            e.printStackTrace();
        }
    }

    public void connectSftp(ActionEvent actionEvent) {
        try {
            if (usernameTxt.getText().isEmpty() || hostTxt.getText().isEmpty() || passwordTxt.getText().isEmpty() || portTxt.getText().isEmpty()){
                Alert failedAlert = new Alert(Alert.AlertType.ERROR, "Please fill all fields");
                failedAlert.showAndWait();
                return;
            }
            Network.resetSession();
            Network.setupJsch(usernameTxt.getText(), hostTxt.getText(), passwordTxt.getText(), Integer.parseInt(portTxt.getText()));
            if (Network.isConnected()){
                Alert succesAlert = new Alert(Alert.AlertType.INFORMATION, "Success connected");
                succesAlert.showAndWait();
            } else {
                Alert failedAlert = new Alert(Alert.AlertType.ERROR, "Can't connect server");
                failedAlert.showAndWait();
            }
        } catch (Exception e){
            Alert failedAlert = new Alert(Alert.AlertType.ERROR, "Can't connect server");
            failedAlert.showAndWait();
            e.printStackTrace();
        }
    }

    public void resetCredentials(ActionEvent actionEvent) {
        try {
            settingsService.resetCredentials();
            initialize();
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Success reset credentials");
            successAlert.showAndWait();
        } catch (BackingStoreException e){
            logger.error(e.getMessage(), e);
            Alert failedAlert = new Alert(Alert.AlertType.ERROR, "Can't reset credentials");
            failedAlert.showAndWait();
        }
    }
}
