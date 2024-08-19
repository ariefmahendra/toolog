package com.ariefmahendra.log.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SftpModel {
    private String username;
    private String remoteHost;
    private String password;
    private String port;
}
