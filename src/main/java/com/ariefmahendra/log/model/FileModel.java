package com.ariefmahendra.log.model;

public class FileModel {
    private String name;
    private long size;
    private String date;
    private String Directory;

    public FileModel() {
    }

    public FileModel(String name, long size, String date, String directory) {
        this.name = name;
        this.size = size;
        this.date = date;
        Directory = directory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDirectory() {
        return Directory;
    }

    public void setDirectory(String directory) {
        Directory = directory;
    }
}
