package com.gymsystem.basic.exception;

/** 422 - the request is valid but breaks a business rule. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
