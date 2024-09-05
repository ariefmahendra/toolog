package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.shared.util.Downloader;
import com.ariefmahendra.log.shared.util.Network;
import com.ariefmahendra.log.shared.util.Reader;
import com.jcraft.jsch.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class LogServiceImpl implements LogService {
    private  String LOG_FILE_PATH;
    private  String HOST;
    private  String USERNAME;
    private  String PASSWORD;
    private  int PORT;
    private  int bufferSize;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LogServiceImpl.class);

    public LogServiceImpl() {
        SettingsService settingsService = new SettingsServiceImpl();
        CredentialsDto credentials = settingsService.getCredentials();
        LOG_FILE_PATH = credentials.getLog().getDirectory();
        HOST = credentials.getSftp().getRemoteHost();
        USERNAME = credentials.getSftp().getUsername();
        PASSWORD = credentials.getSftp().getPassword();
        PORT = Integer.parseInt(credentials.getSftp().getPort());
        bufferSize = Integer.parseInt(credentials.getLog().getBufferSize());
    }

    @Override
    public String getLatestLog() throws SettingsNotValidException, ConnectionException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.LOG_FILE_PATH.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }

        String logEntries;
        try {
            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.connect();

            // todo: refactor business logic with command tail
            String command = String.format("tail -n %d %s", bufferSize, LOG_FILE_PATH);
            logEntries = Network.remoteExecCommand(command).stream().map(line -> line + "\n").collect(Collectors.joining());
        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException("Connection Error", e);
        } catch (IOException e) {
            logger.error("Failed to read the log file", e);
            throw new RuntimeException("Error reading log file", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return logEntries;
    }

    private String downloadLogFile(String logFilePath) throws Exception {
        String currentDir = System.getProperty("user.dir");
        String[] filePath = logFilePath.split("/");
        String fileName = filePath[filePath.length - 1];
        String pathDownloaded = currentDir + "/Downloads/" + fileName;

        Downloader.downloadFileByThread(logFilePath, pathDownloaded);
        return pathDownloaded;
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
            Reader.readFileByCapacity(bufferSize, pathDownloaded);
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
