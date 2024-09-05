package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;

import java.util.LinkedList;

public interface LogService {
    String getLatestLog() throws SettingsNotValidException, ConnectionException;
    LinkedList<String> searchLogByKeyword(String keyword) throws GeneralException, SettingsNotValidException, ConnectionException;
}
