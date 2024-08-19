package com.ariefmahendra.log.service;

import com.ariefmahendra.log.dto.CredentialsDto;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;

import java.util.prefs.Preferences;

public class SettingsServiceImpl implements SettingsService{
    @Override
    public void settingCredentials(LogModel logModel, SftpModel sftpModel) {
        Preferences.userNodeForPackage(logModel.getClass()).put("logDirectory", logModel.getDirectory());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("username", sftpModel.getUsername());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("remoteHost", sftpModel.getRemoteHost());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("port", sftpModel.getPort());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("password", sftpModel.getPassword());
    }

    @Override
    public CredentialsDto getCredentials() {
        CredentialsDto credentialsDto = new CredentialsDto();

        // Retrieve log directory from Preferences
        Preferences logPreferences = Preferences.userNodeForPackage(LogModel.class);
        String logDirectory = logPreferences.get("logDirectory", "");

        // Retrieve SFTP details from Preferences
        Preferences sftpPreferences = Preferences.userNodeForPackage(SftpModel.class);
        String username = sftpPreferences.get("username", "");
        String remoteHost = sftpPreferences.get("remoteHost", "");
        String port = sftpPreferences.get("port", "");
        String password = sftpPreferences.get("password", "");

        // create log model
        LogModel log = new LogModel();
        log.setDirectory(logDirectory);

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
