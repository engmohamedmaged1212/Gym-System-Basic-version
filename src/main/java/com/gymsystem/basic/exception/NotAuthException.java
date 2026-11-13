package com.gymsystem.basic.exception;

/** 401 - the caller is not authenticated / the token or session is not valid. */
public class NotAuthException extends RuntimeException {
    public NotAuthException(String message) {
        super(message);
    }
}
