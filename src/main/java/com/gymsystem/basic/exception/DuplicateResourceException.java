package com.gymsystem.basic.exception;

/** 409 - a unique value (email, phone, name...) is already used. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
