package com.ariefmahendra.log.model;

public class DirectoryModel {
    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public DirectoryModel(String directory, String date, long size) {
        this.directory = directory;
        this.date = date;
        this.size = size;
    }

    public DirectoryModel() {
    }

    private String directory;
    private String date;
    private long size;
}
