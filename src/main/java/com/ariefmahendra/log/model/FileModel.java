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
public class FileModel {
    private String name;
    private long size;
    private Date date;
    private String Directory;
}
