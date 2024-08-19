package com.ariefmahendra.log.dto;

import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CredentialsDto {
    private LogModel log;
    private SftpModel sftp;
}
