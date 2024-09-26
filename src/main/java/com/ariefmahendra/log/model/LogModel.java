package com.ariefmahendra.log.model;

public class LogModel {
    private String directory;
    private String bufferSize;

    public LogModel(String directory, String bufferSize) {
        this.directory = directory;
        this.bufferSize = bufferSize;
    }

    public LogModel() {
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(String bufferSize) {
        this.bufferSize = bufferSize;
    }
}
