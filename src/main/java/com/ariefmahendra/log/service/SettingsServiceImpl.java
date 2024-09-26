package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SettingsServiceImpl implements SettingsService{
    @Override
    public void settingCredentials(LogModel logModel, SftpModel sftpModel) {
        Preferences.userNodeForPackage(logModel.getClass()).put("defaultFile", logModel.getDirectory());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("username", sftpModel.getUsername());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("remoteHost", sftpModel.getRemoteHost());
        Preferences.userNodeForPackage(sftpModel.getClass()).put("port", String.valueOf(sftpModel.getPort()));
        Preferences.userNodeForPackage(sftpModel.getClass()).put("password", sftpModel.getPassword());
        Preferences.userNodeForPackage(logModel.getClass()).put("bufferSize", logModel.getBufferSize());
    }

    @Override
    public CredentialsDto getCredentials() {
        CredentialsDto credentialsDto = new CredentialsDto();

        Preferences logPreferences = Preferences.userNodeForPackage(LogModel.class);
        String logDirectory = logPreferences.get("defaultFile", "");
        String bufferSize = logPreferences.get("bufferSize", "1000");

        Preferences sftpPreferences = Preferences.userNodeForPackage(SftpModel.class);
        String username = sftpPreferences.get("username", "");
        String remoteHost = sftpPreferences.get("remoteHost", "");
        String port = sftpPreferences.get("port", "22");
        String password = sftpPreferences.get("password", "");

        LogModel log = new LogModel();
        log.setDirectory(logDirectory);
        log.setBufferSize(bufferSize);

        SftpModel sftp = new SftpModel();
        sftp.setRemoteHost(remoteHost);
        sftp.setPort(port);
        sftp.setUsername(username);
        sftp.setPassword(password);

        credentialsDto.setLog(log);
        credentialsDto.setSftp(sftp);
        return credentialsDto;
    }

    @Override
    public void resetCredentials() throws BackingStoreException {
        Preferences.userNodeForPackage(LogModel.class).clear();
        Preferences.userNodeForPackage(SftpModel.class).clear();
    }
}
