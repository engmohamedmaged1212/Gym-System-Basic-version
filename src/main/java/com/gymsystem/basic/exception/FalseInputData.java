package com.gymsystem.basic.exception;

/** 400 - the input data is logically wrong. */
public class FalseInputData extends RuntimeException {
    public FalseInputData(String message) {
        super(message);
    }
}
