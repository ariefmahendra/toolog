package com.ariefmahendra.log.shared.dto;

import com.ariefmahendra.log.model.LogModel;
import com.ariefmahendra.log.model.SftpModel;

public class CredentialsDto {
    private LogModel log;
    private SftpModel sftp;

    public CredentialsDto() {
    }

    public CredentialsDto(LogModel log, SftpModel sftp) {
        this.log = log;
        this.sftp = sftp;
    }

    public LogModel getLog() {
        return log;
    }

    public void setLog(LogModel log) {
        this.log = log;
    }

    public SftpModel getSftp() {
        return sftp;
    }

    public void setSftp(SftpModel sftp) {
        this.sftp = sftp;
    }
}
