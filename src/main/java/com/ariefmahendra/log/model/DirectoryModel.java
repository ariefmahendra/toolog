package com.ariefmahendra.log.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DirectoryModel {
    private String directory;
    private Date date;
    private long size;
}
