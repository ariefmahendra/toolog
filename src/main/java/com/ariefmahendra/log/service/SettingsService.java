package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;

public interface SettingsService {
    void settingCredentials(LogModel logModel, SftpModel sftpModel);
    CredentialsDto getCredentials();
}
