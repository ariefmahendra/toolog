package com.ariefmahendra.log.service;

import com.ariefmahendra.log.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.GeneralException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.shared.util.Downloader;
import com.ariefmahendra.log.shared.util.Network;
import com.ariefmahendra.log.shared.util.Reader;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.LinkedList;

public class SearchLogServiceImpl implements SearchLogService {
    private final String LOG_FILE_PATH;
    private final String HOST;
    private final String USERNAME;
    private final String PASSWORD;
    private final int PORT;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SearchLogServiceImpl.class);

    public SearchLogServiceImpl() {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        LOG_FILE_PATH = credentials.getLog().getDirectory();
        HOST = credentials.getSftp().getRemoteHost();
        USERNAME = credentials.getSftp().getUsername();
        PASSWORD = credentials.getSftp().getPassword();
        PORT = Integer.parseInt(credentials.getSftp().getPort());
    }

    @Override
    public LinkedList<String> searchLogByKeyword(String keyword) throws GeneralException, SettingsNotValidException, ConnectionException {
        validateSettings();

        LinkedList<String> logResult;
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            String pathDownloaded = downloadLogFile();
            Reader.readFileByCapacity(400000, pathDownloaded);
            String log = Reader.getResultLog();
            logResult = filterLogByKeyword(log, keyword);
        } catch (SftpException e) {
            logger.error("Failed to connect or interact with SFTP server", e);
            throw new ConnectionException("Connection to sftp server error", e);
        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException("Connection Error", e);
        } catch (IOException e) {
            logger.error("Failed to read the log file", e);
            throw new RuntimeException("Error reading log file", e);
        } finally {
            if (session != null){
                session.disconnect();
            }
            if (sftpChannel != null){
                sftpChannel.disconnect();
            }
        }

        return logResult;
    }

    private void validateSettings() throws SettingsNotValidException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.LOG_FILE_PATH.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }
    }

    private String downloadLogFile() throws SftpException, JSchException, IOException, ConnectionException {
        String currentDir = System.getProperty("user.dir");
        String fileName = LOG_FILE_PATH.substring(LOG_FILE_PATH.lastIndexOf('/') + 1);
        String pathDownloaded = currentDir + "/Downloads/" + fileName;

        Downloader.downloadFileByThread(LOG_FILE_PATH, pathDownloaded);
        return pathDownloaded;
    }

    private LinkedList<String> filterLogByKeyword(String log, String keyword) {
        LinkedList<String> filteredLog = new LinkedList<>();
        String[] splitLog = log.split("\n");

        StringBuilder resultLogFiltered = new StringBuilder();
        boolean isStartWrite = false;

        for (String logEntry : splitLog) {
            if (logEntry.startsWith("Type")) {
                isStartWrite = true;
            } else if (logEntry.startsWith("DEBUG") || logEntry.startsWith("ERROR")) {
                isStartWrite = false;
                if (!resultLogFiltered.isEmpty()) {
                    filteredLog.add(resultLogFiltered.toString());
                }
                resultLogFiltered = new StringBuilder();
            }
            if (isStartWrite) {
                resultLogFiltered.append(logEntry).append("\n");
            }
        }

        return filteredLog;
    }
}
