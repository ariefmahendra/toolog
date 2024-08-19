package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
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
    public LinkedList<String> searchLogByKeyword(String keyword) throws SettingsNotValidException, ConnectionException {
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

    private LinkedList<String> filterLogByKeyword(String logPayload, String keyword) {
        LinkedList<String> resultLogFiltered = new LinkedList<>();
        LinkedList<String> tempLog = new LinkedList<>();
        String[] splitLog = logPayload.split("\n");

        StringBuilder currentLog = new StringBuilder();
        boolean isStartWrite = false;

        for (String log : splitLog) {
            if (isStartWrite) {
                if (log.startsWith("ERROR") || log.startsWith("INFO") || log.startsWith("DEBUG")) {
                    tempLog.add(currentLog.toString());
                    currentLog.setLength(0);
                    isStartWrite = false;
                } else {
                    currentLog.append(log).append("\n");
                }
            } else if (log.startsWith("Type")) {
                currentLog.append(log).append("\n");
                isStartWrite = true;
            }
        }

        tempLog.forEach(log -> {
            if (log.toLowerCase().contains(keyword)) {
                resultLogFiltered.add(log);
            }
        });

        return separateLog(resultLogFiltered);
    }

    private LinkedList<String> separateLog(LinkedList<String> logs) {
        LinkedList<String> logSeparated = new LinkedList<>();
        for (String log : logs) {
            int start = log.indexOf("{");
            int end = log.lastIndexOf("}") + 1;
            String jsonContent = log.substring(start, end);

            String prettyJsonFormater = Reader.prettyJsonFormater(jsonContent);

            int startInfoPayload = 0;
            int endInfoPayload = start - 1;
            String infoPayload = log.substring(startInfoPayload, endInfoPayload);

            logSeparated.add("================================================");
            logSeparated.add(infoPayload);
            logSeparated.add(prettyJsonFormater);
        }
        return logSeparated;
    }

}
