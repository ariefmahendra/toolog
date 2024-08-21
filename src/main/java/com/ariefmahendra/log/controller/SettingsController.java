package com.ariefmahendra.log.controller;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;
import com.ariefmahendra.log.service.SettingsService;
import com.ariefmahendra.log.service.SettingsServiceImpl;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsController {
    public TextField hostTxt;
    public TextField usernameTxt;
    public TextField passwordTxt;
    public TextField defaultFolderTxt;
    public TextField portTxt;
    public Button saveSettingsBtn;
    public TextField bufferSizeTxt;

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
        defaultFolderTxt.setText(credentials.getLog().getDirectory());
        bufferSizeTxt.setText(credentials.getLog().getBufferSize());
    }

    public void saveCredentials(ActionEvent actionEvent) {
        String host = hostTxt.getText();
        String port = portTxt.getText();
        String username = usernameTxt.getText();
        String password = passwordTxt.getText();
        String defaultFolder = defaultFolderTxt.getText();
        String bufferSize = bufferSizeTxt.getText();

        LogModel logModel = new LogModel(defaultFolder, bufferSize);
        SftpModel sftpModel = new SftpModel(username, host, password, port);

        settingsService.settingCredentials(logModel, sftpModel);
        logger.info("Successfully saved credentials");
    }
}
