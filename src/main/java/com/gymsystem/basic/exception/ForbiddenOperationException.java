package com.gymsystem.basic.exception;

/** 403 - the caller is authenticated but may not do this. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
