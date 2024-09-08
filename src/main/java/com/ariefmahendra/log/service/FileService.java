package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.model.FileModel;
import com.ariefmahendra.log.shared.dto.ListFileOrDirDto;

import java.util.Optional;

public interface FileService {
    ListFileOrDirDto getDir(Optional<String> path) throws ConnectionException, SettingsNotValidException;
    ListFileOrDirDto getBackDir(String currentPath) throws ConnectionException, SettingsNotValidException;
    ListFileOrDirDto getNextDir(String currentPath) throws ConnectionException, SettingsNotValidException;
    void setFileToDefaultFolder(FileModel fileModel) throws SettingsNotValidException;
}
