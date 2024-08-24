package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.model.DirectoryModel;
import com.ariefmahendra.log.model.FileModel;
import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.shared.dto.ListFileOrDirDto;
import com.ariefmahendra.log.shared.util.Network;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FileServiceImpl implements FileService {

    private final String LOG_FILE_PATH;
    private final String HOST;
    private final String USERNAME;
    private final String PASSWORD;
    private final int PORT;
    private static ChannelSftp sftpChannel = null;
    private static Session session = null;

    private static final Logger logger = LoggerFactory.getLogger(LatestLogServiceImpl.class);

    public FileServiceImpl() throws ConnectionException {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        LOG_FILE_PATH = credentials.getLog().getDirectory();
        HOST = credentials.getSftp().getRemoteHost();
        USERNAME = credentials.getSftp().getUsername();
        PASSWORD = credentials.getSftp().getPassword();
        PORT = Integer.parseInt(credentials.getSftp().getPort());

        if (session == null || sftpChannel == null)  {
            try {
                session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
                sftpChannel = (ChannelSftp) session.openChannel("sftp");
                sftpChannel.connect();
            } catch (JSchException e) {
                throw new ConnectionException("Connection to sftp server error", e);
            }
        }
    }

    @Override
    public ListFileOrDirDto getDir(Optional<String> currentPath) throws ConnectionException, SettingsNotValidException {
        validateCredentials();
        if (currentPath.isEmpty()) {
            return loadDir(LOG_FILE_PATH);
        }
        return loadDir(currentPath.get());
    }

    @Override
    public ListFileOrDirDto getBackDir(String currentPath) throws ConnectionException, SettingsNotValidException {
        validateCredentials();
        return loadDir(currentPath);
    }

    @Override
    public ListFileOrDirDto getNextDir(String currentPath) throws ConnectionException, SettingsNotValidException {
        validateCredentials();
        return loadDir(currentPath);
    }

    @Override
    public void setFileToDefaultFolder(FileModel fileModel) throws SettingsNotValidException {
        validateCredentials();
    }

    private ListFileOrDirDto loadDir(String currentPath) throws ConnectionException {
        ListFileOrDirDto listFileOrDirDto;
        try {
            String absolutePath = getAbsolutePath(currentPath);
            Vector ls = sftpChannel.ls(absolutePath);
            listFileOrDirDto = getDir(ls, absolutePath);
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to sftp server error", e);
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
        return listFileOrDirDto;
    }

    private static ListFileOrDirDto getDir(Vector ls, String absolutePath) {
        List<FileModel> files = new LinkedList<>();
        List<DirectoryModel> directories = new LinkedList<>();
        for (Object entry : ls) {
            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) entry;
            String fileName = lsEntry.getFilename();
            long fileSize = lsEntry.getAttrs().getSize();
            Date fileDate = new Date((long) lsEntry.getAttrs().getMTime() * 1000);
            String filePath = absolutePath + "/" + fileName;
            if (!lsEntry.getAttrs().isDir()) {
                FileModel fileModel = new FileModel(fileName, fileSize, fileDate, filePath);
                files.add(fileModel);
            } else {
                DirectoryModel directoryModel = new DirectoryModel(filePath, fileDate, fileSize);
                directories.add(directoryModel);
            }
        }
        return new ListFileOrDirDto(files, directories);
    }

    private static String getAbsolutePath(String path) {
        int lastIndex = path.lastIndexOf("/");
        return path.substring(0, lastIndex);
    }

    private void validateCredentials() throws SettingsNotValidException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.LOG_FILE_PATH.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }
    }
}
