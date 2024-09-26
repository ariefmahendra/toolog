package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.shared.util.Network;
import com.jcraft.jsch.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ariefmahendra.log.shared.util.Reader.prettyJsonFormater;

public class LogServiceImpl implements LogService {
    private String LOG_FILE_PATH;
    private final String HOST;
    private final String USERNAME;
    private final String PASSWORD;
    private final int PORT;
    private final int bufferSize;

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
    public String getLatestLog(String optionalPathFile) throws SettingsNotValidException, ConnectionException {
        validateSettings();
        if (optionalPathFile != null){
            LOG_FILE_PATH = optionalPathFile;
        }

        String logEntries;
        try {
            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.connect();

            // Todo: refactor business logic with command tail
            String command = String.format("tail -n %d %s", bufferSize, LOG_FILE_PATH);
            logEntries = Network.remoteExecCommand(command).stream().map(line -> line + "\n").collect(Collectors.joining());
        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Failed to read the log file", e);
            throw new RuntimeException("Error reading log file", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return logEntries;
    }


    @Override
    public List<String> searchLogByKeyword(String keyword, String optionalPathFile) throws SettingsNotValidException, ConnectionException {
        validateSettings();
        if (optionalPathFile != null){
            LOG_FILE_PATH = optionalPathFile;
        }

        List<String> logResult = new ArrayList<>();
        try {
            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.connect();

            // Todo: refactor business logic with command tail
            String command = String.format("tail -n %d %s", bufferSize, LOG_FILE_PATH);
            List<String> logEntriesPerLine = Network.remoteExecCommand(command);

            Pattern logPattern = Pattern.compile("^(DEBUG|ERROR|INFO) (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})");

            StringBuilder logSb = new StringBuilder();
            String lastTimestamp = "";
            boolean isWrite = false;

            for (String log : logEntriesPerLine) {
                Matcher matcher = logPattern.matcher(log);
                if (matcher.find()) {
                    lastTimestamp = matcher.group(2);
                }

                if (log.startsWith("Type")) {
                    isWrite = true;
                    if (!lastTimestamp.isEmpty()) {
                        logSb.append("Time : ").append(lastTimestamp).append("\n");
                    }
                } else if (log.startsWith("DEBUG") || log.startsWith("ERROR") || log.startsWith("INFO")) {
                    isWrite = false;
                }

                if (isWrite) {
                    logSb.append(log).append("\n");
                } else {
                    if (!logSb.isEmpty()) {
                        String logContent = logSb.toString();
                        int startIndexJsonContent = logContent.indexOf("{");
                        if (startIndexJsonContent > 0) {
                            String rawJson = logSb.substring(startIndexJsonContent);
                            String jsonFormatted = prettyJsonFormater(rawJson);
                            logContent = logSb.substring(0, startIndexJsonContent - 1) + "\n" + jsonFormatted;
                        }

                        if (logContent.toUpperCase().contains(keyword.toUpperCase())) {
                            logResult.add(logContent + "\n");
                        }
                    }

                    logSb.setLength(0);
                }
            }
        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server");
            throw new ConnectionException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Failed to read the log file");
            throw new RuntimeException("Error reading log file", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return logResult;
    }

    private void validateSettings() throws SettingsNotValidException {
        if (this.HOST.isEmpty() || this.USERNAME.isEmpty() || this.PASSWORD.isEmpty() || this.LOG_FILE_PATH.isEmpty() || this.PORT == 0) {
            throw new SettingsNotValidException("Please set your credentials first, then try again.");
        }
    }
}
