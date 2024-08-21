package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;

import java.util.prefs.Preferences;

public class SettingsServiceImpl implements SettingsService{
    @Override
    public void settingCredentials(LogModel logModel, SftpModel sftpModel) {
        Preferences.userNodeForPackage(logModel.getClass()).put("logDirectory", logModel.getDirectory());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("username", sftpModel.getUsername());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("remoteHost", sftpModel.getRemoteHost());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("port", String.valueOf(sftpModel.getPort()));
        Preferences.userNodeForPackage(sftpModel.getClass()).put("password", sftpModel.getPassword());
        Preferences.userNodeForPackage(logModel.getClass()).put("bufferSize", logModel.getBufferSize());
    }

    @Override
    public CredentialsDto getCredentials() {
        CredentialsDto credentialsDto = new CredentialsDto();

        // Retrieve log directory from Preferences
        Preferences logPreferences = Preferences.userNodeForPackage(LogModel.class);
        String logDirectory = logPreferences.get("logDirectory", "");
        String bufferSize = logPreferences.get("bufferSize", "400000");

        // Retrieve SFTP details from Preferences
        Preferences sftpPreferences = Preferences.userNodeForPackage(SftpModel.class);
        String username = sftpPreferences.get("username", "");
        String remoteHost = sftpPreferences.get("remoteHost", "");
        String port = sftpPreferences.get("port", "22");
        String password = sftpPreferences.get("password", "");

        // create log model
        LogModel log = new LogModel();
        log.setDirectory(logDirectory);
        log.setBufferSize(bufferSize);

        // create sftp model
        SftpModel sftp = new SftpModel();
        sftp.setRemoteHost(remoteHost);
        sftp.setPort(port);
        sftp.setUsername(username);
        sftp.setPassword(password);

        // Set retrieved values in the CredentialsDto
        credentialsDto.setLog(log);
        credentialsDto.setSftp(sftp);

        return credentialsDto;
    }
}
