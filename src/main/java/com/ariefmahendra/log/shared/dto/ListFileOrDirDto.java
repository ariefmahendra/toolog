package com.ariefmahendra.log.shared.dto;

import com.ariefmahendra.log.model.DirectoryModel;
import com.ariefmahendra.log.model.FileModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ListFileOrDirDto {
    private List<FileModel> files = new ArrayList<>();
    private List<DirectoryModel> directories = new ArrayList<>();
    private String parentPath;
}
