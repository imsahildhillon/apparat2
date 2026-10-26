package com.apparat.service;

public class BookingLimitException extends RuntimeException {

    public BookingLimitException(String message) {
        super(message);
    }
}
