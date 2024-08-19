package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.jcraft.jsch.JSchException;

import java.util.LinkedList;

public interface SearchLogService {
    LinkedList<String> searchLogByKeyword(String keyword) throws GeneralException, SettingsNotValidException, ConnectionException;
}
