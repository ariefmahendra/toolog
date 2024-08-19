package com.ariefmahendra.log.shared.util;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Network {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors() + 4;

    public static Session setupJsch(String username, String remoteHost, String password, int port) throws JSchException {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(username, remoteHost, port);
        jschSession.setPassword(password);
        jschSession.setConfig("StrictHostKeyChecking","no");
        jschSession.connect();
        return jschSession;
    }
}
