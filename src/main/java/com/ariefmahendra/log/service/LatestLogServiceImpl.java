package com.ariefmahendra.log.service;

import com.ariefmahendra.log.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.shared.util.Downloader;
import com.ariefmahendra.log.shared.util.Network;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class LatestLogServiceImpl implements LatestLogService{
    private final String LOG_FILE_PATH;
    private final String HOST;
    private final String USERNAME;
    private final String PASSWORD;
    private final int PORT;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LatestLogServiceImpl.class);

    public LatestLogServiceImpl() {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        LOG_FILE_PATH = credentials.getLog().getDirectory();
        HOST = credentials.getSftp().getRemoteHost();
        USERNAME = credentials.getSftp().getUsername();
        PASSWORD = credentials.getSftp().getPassword();
        PORT = Integer.parseInt(credentials.getSftp().getPort());
    }

    @Override
    public String getLatestLog() throws SettingsNotValidException, ConnectionException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.LOG_FILE_PATH.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }

        StringBuilder logEntries = new StringBuilder();
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            String pathDownloaded = downloadLogFile(LOG_FILE_PATH);

            logEntries.append(readLogFile(pathDownloaded));
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to sftp server error", e);
        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException("Connection Error", e);
        } catch (IOException e) {
            logger.error("Failed to read the log file", e);
            throw new RuntimeException("Error reading log file", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (sftpChannel != null){
                sftpChannel.disconnect();
            }

            if (session != null){
                session.disconnect();
            }
        }
        return logEntries.toString();
    }

    private String downloadLogFile(String logFilePath) throws Exception {
        String currentDir = System.getProperty("user.dir");
        String[] filePath = logFilePath.split("/");
        String fileName = filePath[filePath.length - 1];
        String pathDownloaded = currentDir + "/Downloads/" + fileName;

        Downloader.downloadFileByThread(logFilePath, pathDownloaded);
        return pathDownloaded;
    }

    private String readLogFile(String pathDownloaded) throws IOException {
        logger.info("STARTING READ");
        StringBuilder logEntries = new StringBuilder();
        try (RandomAccessFile raf = new RandomAccessFile(pathDownloaded, "r")) {
            raf.seek(raf.length() - 400000);
            byte[] fileReaderByCapacity = new byte[400000];
            raf.read(fileReaderByCapacity);
            logEntries.append(new String(fileReaderByCapacity, StandardCharsets.UTF_8));
        }
        logger.info("END READ");
        return logEntries.toString();
    }

}
