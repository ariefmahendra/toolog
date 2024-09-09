package com.ariefmahendra.log.shared.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Network {

    private static Session session;

    private static final Logger logger = LoggerFactory.getLogger(Network.class);

    public static Session setupJsch(String username, String remoteHost, String password, int port) throws JSchException {
        if (session != null){
            return session;
        }

        JSch jsch = new JSch();
        session = jsch.getSession(username, remoteHost, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking","no");
        session.connect();
        return session;
    }

    public static void disconnect() {
        if (session != null) {
            if (session.isConnected()) {
                try {
                    session.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> remoteExecCommand(String command) throws JSchException, IOException {
        List<String> resultLine = new ArrayList<>();
        ChannelExec channel;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream input = channel.getInputStream();
            channel.connect();
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    resultLine.add(inputLine);
                }
            } finally {
                if (input != null){
                    try {
                        input.close();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return resultLine;
    }

    public static boolean isConnected() {
        return session != null && session.isConnected();
    }

    public static void resetSession() {
        if (session != null && session.isConnected()) {
            disconnect();
        }
        session = null;
        logger.info("Session has been reset successfully.");
    }
}
