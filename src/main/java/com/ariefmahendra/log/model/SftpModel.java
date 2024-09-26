package com.ariefmahendra.log.model;

public class SftpModel {
    private String username;
    private String remoteHost;
    private String password;
    private String port;

    public SftpModel() {
    }

    public SftpModel(String username, String remoteHost, String password, String port) {
        this.username = username;
        this.remoteHost = remoteHost;
        this.password = password;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
