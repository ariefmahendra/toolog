package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;

public interface LatestLogService {
    String getLatestLog() throws SettingsNotValidException, ConnectionException;
}
