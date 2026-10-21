package com.apparat.service;

import java.lang.*;

public class SlotConflictException extends RuntimeException {

    public SlotConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

