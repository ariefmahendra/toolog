package com.ariefmahendra.log.shared.dto;

import com.ariefmahendra.log.model.DirectoryModel;
import com.ariefmahendra.log.model.FileModel;
import java.util.ArrayList;
import java.util.List;

public class ListFileOrDirDto {
    private List<FileModel> files = new ArrayList<>();
    private List<DirectoryModel> directories = new ArrayList<>();
    private String parentPath;

    public ListFileOrDirDto() {
    }

    public ListFileOrDirDto(List<FileModel> files, List<DirectoryModel> directories, String parentPath) {
        this.files = files;
        this.directories = directories;
        this.parentPath = parentPath;
    }

    public List<FileModel> getFiles() {
        return files;
    }

    public void setFiles(List<FileModel> files) {
        this.files = files;
    }

    public List<DirectoryModel> getDirectories() {
        return directories;
    }

    public void setDirectories(List<DirectoryModel> directories) {
        this.directories = directories;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
}
