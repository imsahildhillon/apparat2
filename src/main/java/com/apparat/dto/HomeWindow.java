package com.apparat.dto;

/**
 * A maintenance window prepared for the home page.
 */
public record HomeWindow(
        String instrument,
        String when,
        String reason
) {}
