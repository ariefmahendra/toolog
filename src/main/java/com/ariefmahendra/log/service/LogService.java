package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;

import java.util.List;

public interface LogService {
    String getLatestLog(String optionalPathFile) throws SettingsNotValidException, ConnectionException;
    List<String> searchLogByKeyword(String keyword, String optionalPathFile) throws GeneralException, SettingsNotValidException, ConnectionException;
}
