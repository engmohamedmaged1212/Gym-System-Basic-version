package com.gymsystem.basic.common;

/** Regex constants shared by the DTO validations. */
public final class Patterns {
    private Patterns() {
    }

    /** 8-15 digits, optional leading + (01012345678, +201012345678) */
    public static final String PHONE = "^\\+?[0-9]{8,15}$";
}

