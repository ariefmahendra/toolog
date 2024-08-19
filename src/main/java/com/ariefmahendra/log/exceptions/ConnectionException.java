package com.ariefmahendra.log.exceptions;

public class ConnectionException extends Exception{
    public ConnectionException(String message, Exception e) {
        super(message, e);
    }
}
