package com.apparat.dto;

/**
 * A booking prepared for the home page, with the time already written out.
 */
public record HomeBooking(
        int id,
        String instrument,
        String when
) {}
