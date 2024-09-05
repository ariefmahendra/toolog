package com.ariefmahendra.log.service;

import com.ariefmahendra.log.shared.dto.CredentialsDto;
import com.ariefmahendra.log.exceptions.ConnectionException;
import com.ariefmahendra.log.exceptions.SettingsNotValidException;
import com.ariefmahendra.log.shared.util.Network;
import com.ariefmahendra.log.shared.util.Reader;
import com.jcraft.jsch.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.ariefmahendra.log.shared.util.Reader.prettyJsonFormater;

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
        validateSettings();

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


    @Override
    public List<String> searchLogByKeyword(String keyword) throws SettingsNotValidException, ConnectionException {
        validateSettings();

        List<String> logResult = new ArrayList<>();
        try {
            Session session = Network.setupJsch(USERNAME, HOST, PASSWORD, PORT);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.connect();

            // todo: refactor business logic with command tail
            String command = String.format("tail -n %d %s", bufferSize, LOG_FILE_PATH);
            List<String> logEntriesPerLine = Network.remoteExecCommand(command);

            boolean isWrite = false;
            StringBuilder logSb = new StringBuilder();

            // todo: filter log entries only get request and response message
            for (String log : logEntriesPerLine) {
                if (log.startsWith("Type")){
                    isWrite = true;
                } else if (log.startsWith("DEBUG") || log.startsWith("ERROR") || log.startsWith("INFO")){
                    isWrite = false;
                }

                if (isWrite){
                    logSb.append(log).append("\n");
                } else {
                    if (!logSb.isEmpty()){
                        String logContent = logSb.toString();

                        int startIndexJsonContent = logContent.indexOf("{");
                        if (startIndexJsonContent > 0){
                            String rawJson = logSb.substring(startIndexJsonContent);
                            String jsonFormatted = prettyJsonFormater(rawJson);
                            logContent = logSb.substring(0, startIndexJsonContent - 1) + "\n" +jsonFormatted;
                        }

                        if (logContent.toUpperCase().contains(keyword.toUpperCase())){
                            logResult.add(logContent + "\n");
                        }
                    }

                    logSb.setLength(0);
                }
            }

        } catch (JSchException e){
            logger.error("Failed to connect to SFTP server", e);
            throw new ConnectionException("Connection Error", e);
        } catch (IOException e) {
            logger.error("Failed to read the log file", e);
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
