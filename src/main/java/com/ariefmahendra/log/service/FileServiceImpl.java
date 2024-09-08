package com.ariefmahendra.log.service;

import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.model.DirectoryModel;
import com.ariefmahendra.log.model.FileModel;
import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.shared.dto.ListFileOrDirDto;
import com.ariefmahendra.log.shared.util.Network;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class FileServiceImpl implements FileService {

    private final String LOG_FILE_PATH;
    private final String HOST;
    private final String USERNAME;
    private final String PASSWORD;
    private final int PORT;
    private static ChannelSftp sftpChannel = null;
    private static Session session = null;

    private String currentDirectory;

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    public FileServiceImpl() throws ConnectionException {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        LOG_FILE_PATH = credentials.getLog().getDirectory();
        HOST = credentials.getSftp().getRemoteHost();
        USERNAME = credentials.getSftp().getUsername();
        PASSWORD = credentials.getSftp().getPassword();
        PORT = Integer.parseInt(credentials.getSftp().getPort());

        // currentDirectory = Preferences.userNodeForPackage(FileServiceImpl.class).get("currentDirectory", "/");
        currentDirectory = "/";

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
    public ListFileOrDirDto getDir(Optional<String> path) throws ConnectionException, SettingsNotValidException {
        ListFileOrDirDto listFileOrDirDto = new ListFileOrDirDto();
        String parentPath = "/"; // must be refactor to prefs
        String finalPath;
        if (path.isPresent()) {
            finalPath = path.get();
        } else {
            finalPath = "/";
        }

        try {
            Vector<ChannelSftp.LsEntry> ls = sftpChannel.ls(finalPath);

            ls.forEach(item -> {
                if (item instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;

                    long timestamp = lsEntry.getAttrs().getMTime() * 1000L;
                    Date modificationDate = new Date(timestamp);

                    if (lsEntry.getAttrs().isDir()) {
                        DirectoryModel directoryModel = new DirectoryModel();
                        directoryModel.setDirectory(finalPath + lsEntry.getFilename());
                        directoryModel.setDate(modificationDate);
                        directoryModel.setSize(lsEntry.getAttrs().getSize());
                        listFileOrDirDto.getDirectories().add(directoryModel);
                    } else {
                        FileModel fileModel = new FileModel();
                        fileModel.setName(lsEntry.getFilename());
                        fileModel.setDirectory(finalPath);
                        fileModel.setSize(lsEntry.getAttrs().getSize());
                        fileModel.setDate(modificationDate);
                        listFileOrDirDto.getFiles().add(fileModel);
                    }
                }
            });

            listFileOrDirDto.setParentPath(parentPath);

            Preferences prefs = Preferences.userNodeForPackage(FileServiceImpl.class);
            prefs.put("currentDirectory", finalPath);
            prefs.flush();

        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to SFTP server error", e);
        } catch (BackingStoreException e) {
            logger.error("Failed to save preferences", e);
            throw new RuntimeException("Error saving preferences", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException("Unexpected error", e);
        }

        return listFileOrDirDto;
    }

    @Override
    public ListFileOrDirDto getBackDir(String path) throws ConnectionException, SettingsNotValidException {
        validateCredentials();
        logger.debug("Current Path = {}", path);
        ListFileOrDirDto listFileOrDirDto = new ListFileOrDirDto();
        String parentPath;

        try {
            if (path.equals("/") || path.equals("")) {
                parentPath = "/";
            } else {
                String trimmedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
                parentPath = trimmedPath.substring(0, trimmedPath.lastIndexOf("/"));
                if (parentPath.isEmpty()) {
                    parentPath = "/";
                }
            }

            Preferences prefs = Preferences.userNodeForPackage(FileServiceImpl.class);
            prefs.put("currentDirectory", parentPath);
            prefs.flush();

            Vector<ChannelSftp.LsEntry> ls = sftpChannel.ls(parentPath);

            String finalParentPath = parentPath;
            ls.forEach(item -> {
                if (item instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;

                    long timestamp = lsEntry.getAttrs().getMTime() * 1000L;
                    Date modificationDate = new Date(timestamp);

                    String finalPath = finalParentPath.endsWith("/") ? finalParentPath + lsEntry.getFilename() : finalParentPath + "/" + lsEntry.getFilename();

                    if (lsEntry.getAttrs().isDir()) {
                        DirectoryModel directoryModel = new DirectoryModel();
                        directoryModel.setDirectory(finalPath);
                        directoryModel.setDate(modificationDate);
                        directoryModel.setSize(lsEntry.getAttrs().getSize());
                        listFileOrDirDto.getDirectories().add(directoryModel);
                    } else {
                        FileModel fileModel = new FileModel();
                        fileModel.setName(lsEntry.getFilename());
                        fileModel.setDirectory(finalParentPath);
                        fileModel.setSize(lsEntry.getAttrs().getSize());
                        fileModel.setDate(modificationDate);
                        listFileOrDirDto.getFiles().add(fileModel);
                    }
                }
            });

            listFileOrDirDto.setParentPath(parentPath);
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to SFTP server error", e);
        } catch (BackingStoreException e) {
            logger.error("Failed to save preferences", e);
            throw new RuntimeException("Error saving preferences", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException("Unexpected error", e);
        }

        return listFileOrDirDto;
    }

    @Override
    public ListFileOrDirDto getNextDir(String path) throws ConnectionException, SettingsNotValidException {
        validateCredentials();
        ListFileOrDirDto listFileOrDirDto = new ListFileOrDirDto();
        String parentPath;
        try {

            if (path.equals("/")) {
                parentPath = "/";
            } else {
                parentPath = path.substring(0, path.lastIndexOf("/"));
            }

            Vector<ChannelSftp.LsEntry> ls = sftpChannel.ls(path);

            ls.forEach(item -> {
                if (item instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;

                    // Convert SFTP timestamp to java.util.Date
                    long timestamp = lsEntry.getAttrs().getMTime() * 1000L;
                    Date modificationDate = new Date(timestamp);

                    if (lsEntry.getAttrs().isDir()) {
                        DirectoryModel directoryModel = new DirectoryModel();
                        directoryModel.setDirectory(path + lsEntry.getFilename());
                        directoryModel.setDate(modificationDate);
                        directoryModel.setSize(lsEntry.getAttrs().getSize());
                        listFileOrDirDto.getDirectories().add(directoryModel);
                    } else {
                        FileModel fileModel = new FileModel();
                        fileModel.setName(lsEntry.getFilename());
                        fileModel.setDirectory(path);
                        fileModel.setSize(lsEntry.getAttrs().getSize());
                        fileModel.setDate(modificationDate);
                        listFileOrDirDto.getFiles().add(fileModel);
                    }
                }
            });

            listFileOrDirDto.setParentPath(parentPath);

            Preferences prefs = Preferences.userNodeForPackage(FileServiceImpl.class);
            prefs.put("currentDirectory", path);
            prefs.flush();

        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to SFTP server error", e);
        } catch (BackingStoreException e) {
            logger.error("Failed to save preferences", e);
            throw new RuntimeException("Error saving preferences", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException("Unexpected error", e);
        }

        return listFileOrDirDto;
    }

    @Override
    public void setFileToDefaultFolder(FileModel fileModel) throws SettingsNotValidException {
        validateCredentials();
    }


    private void validateCredentials() throws SettingsNotValidException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.LOG_FILE_PATH.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }
    }
}
