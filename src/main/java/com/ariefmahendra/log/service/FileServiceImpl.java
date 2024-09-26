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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class FileServiceImpl implements FileService {

    private String LOG_FILE_PATH;
    private String HOST;
    private String USERNAME;
    private String PASSWORD;
    private int PORT;

    private String currentDirectory;

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    public FileServiceImpl() {
        currentDirectory = "/";
    }

    @Override
    public ListFileOrDirDto getDir(Optional<String> path) throws ConnectionException, SettingsNotValidException {
        getCredentials();
        validateCredentials();

        ListFileOrDirDto listFileOrDirDto = new ListFileOrDirDto();
        String parentPath = "/"; // must be refactor to prefs
        String finalPath;
        if (path.isPresent()) {
            finalPath = path.get();
        } else {
            finalPath = "/";
        }

        Channel channel = null;

        try {
            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp= (ChannelSftp) channel;

            Vector<ChannelSftp.LsEntry> ls = channelSftp.ls(finalPath);

            ls.forEach(item -> {
                if (item instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;

                    long timestamp = lsEntry.getAttrs().getMTime() * 1000L;
                    Date modificationDate = new Date(timestamp);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = dateFormat.format(modificationDate);

                    if (lsEntry.getAttrs().isDir()) {
                        DirectoryModel directoryModel = new DirectoryModel();
                        directoryModel.setDirectory(finalPath + lsEntry.getFilename());
                        directoryModel.setDate(formattedDate);
                        directoryModel.setSize(lsEntry.getAttrs().getSize());
                        listFileOrDirDto.getDirectories().add(directoryModel);
                    } else {
                        FileModel fileModel = new FileModel();
                        fileModel.setName(lsEntry.getFilename());
                        fileModel.setDirectory(finalPath);
                        fileModel.setSize(lsEntry.getAttrs().getSize());
                        fileModel.setDate(formattedDate);
                        listFileOrDirDto.getFiles().add(fileModel);
                    }
                }
            });

            listFileOrDirDto.setParentPath(parentPath);

            Preferences prefs = Preferences.userNodeForPackage(FileServiceImpl.class);
            prefs.put("currentDirectory", finalPath);
            prefs.flush();

        } catch (SftpException e) {
            throw new ConnectionException(e.getMessage(), e);
        } catch (JSchException e){
            throw new ConnectionException(e.getMessage(), e);
        }
        catch (BackingStoreException e) {
            throw new RuntimeException("Error saving preferences", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error", e);
        } finally {
            if (channel != null){
                channel.disconnect();
            }
        }

        return listFileOrDirDto;
    }

    @Override
    public ListFileOrDirDto getBackDir(String path) throws ConnectionException, SettingsNotValidException {
        getCredentials();
        validateCredentials();

        logger.debug("Current Path = {}", path);
        ListFileOrDirDto listFileOrDirDto = new ListFileOrDirDto();
        String parentPath;
        Channel channel = null;

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

            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp= (ChannelSftp) channel;

            Vector<ChannelSftp.LsEntry> ls = channelSftp.ls(parentPath);

            String finalParentPath = parentPath;
            ls.forEach(item -> {
                if (item instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;

                    long timestamp = lsEntry.getAttrs().getMTime() * 1000L;
                    Date modificationDate = new Date(timestamp);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = dateFormat.format(modificationDate);

                    String finalPath = finalParentPath.endsWith("/") ? finalParentPath + lsEntry.getFilename() : finalParentPath + "/" + lsEntry.getFilename();

                    if (lsEntry.getAttrs().isDir()) {
                        DirectoryModel directoryModel = new DirectoryModel();
                        directoryModel.setDirectory(finalPath);
                        directoryModel.setDate(formattedDate);
                        directoryModel.setSize(lsEntry.getAttrs().getSize());
                        listFileOrDirDto.getDirectories().add(directoryModel);
                    } else {
                        FileModel fileModel = new FileModel();
                        fileModel.setName(lsEntry.getFilename());
                        fileModel.setDirectory(finalParentPath);
                        fileModel.setSize(lsEntry.getAttrs().getSize());
                        fileModel.setDate(formattedDate);
                        listFileOrDirDto.getFiles().add(fileModel);
                    }
                }
            });

            listFileOrDirDto.setParentPath(parentPath);
        } catch (SftpException e) {
            throw new ConnectionException(e.getMessage(), e);
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error saving preferences", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error", e);
        } finally {
            if (channel != null){
                channel.disconnect();
            }
        }

        return listFileOrDirDto;
    }

    @Override
    public ListFileOrDirDto getNextDir(String path) throws ConnectionException, SettingsNotValidException {
        getCredentials();
        validateCredentials();

        ListFileOrDirDto listFileOrDirDto = new ListFileOrDirDto();
        String parentPath;
        Channel channel = null;
        try {
            if (path.equals("/")) {
                parentPath = "/";
            } else {
                parentPath = path.substring(0, path.lastIndexOf("/"));
            }

            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp= (ChannelSftp) channel;

            Vector<ChannelSftp.LsEntry> ls = channelSftp.ls(path);

            ls.forEach(item -> {
                if (item instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;

                    // Convert SFTP timestamp to java.util.Date
                    long timestamp = lsEntry.getAttrs().getMTime() * 1000L;
                    Date modificationDate = new Date(timestamp);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = dateFormat.format(modificationDate);

                    if (lsEntry.getAttrs().isDir()) {
                        DirectoryModel directoryModel = new DirectoryModel();
                        directoryModel.setDirectory(path + lsEntry.getFilename());
                        directoryModel.setDate(formattedDate);
                        directoryModel.setSize(lsEntry.getAttrs().getSize());
                        listFileOrDirDto.getDirectories().add(directoryModel);
                    } else {
                        FileModel fileModel = new FileModel();
                        fileModel.setName(lsEntry.getFilename());
                        fileModel.setDirectory(path);
                        fileModel.setSize(lsEntry.getAttrs().getSize());
                        fileModel.setDate(formattedDate);
                        listFileOrDirDto.getFiles().add(fileModel);
                    }
                }
            });

            listFileOrDirDto.setParentPath(parentPath);

            Preferences prefs = Preferences.userNodeForPackage(FileServiceImpl.class);
            prefs.put("currentDirectory", path);
            prefs.flush();
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server");
            throw new ConnectionException(e.getMessage(), e);
        } catch (BackingStoreException e) {
            logger.error("Failed to save preferences");
            throw new RuntimeException("Error saving preferences", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred");
            throw new RuntimeException("Unexpected error", e);
        } finally {
            if (channel != null){
                channel.disconnect();
            }
        }

        return listFileOrDirDto;
    }

    private void validateCredentials() throws SettingsNotValidException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }
    }

    private void getCredentials(){
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        LOG_FILE_PATH = credentials.getLog().getDirectory();
        HOST = credentials.getSftp().getRemoteHost();
        USERNAME = credentials.getSftp().getUsername();
        PASSWORD = credentials.getSftp().getPassword();
        PORT = Integer.parseInt(credentials.getSftp().getPort());
    }
}
