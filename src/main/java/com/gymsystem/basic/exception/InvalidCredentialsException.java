package com.gymsystem.basic.exception;

/** 401 - wrong credentials (for example a wrong old password). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
