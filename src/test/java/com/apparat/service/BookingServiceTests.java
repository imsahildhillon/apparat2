package com.apparat.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceTests {

    private final BookingService bookingService = new BookingService();

    // 1. 10:00–10:29: invalid, duration below 30 minutes
    @Test
    void rejectsBookingShorterThan30Minutes() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 10, 29);

        assertFalse(bookingService.isValidRange(start, end));
    }

    // 2. 10:00–10:30: valid, exactly 30 minutes
    @Test
    void acceptsBookingOfExactly30Minutes() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 10, 30);

        assertTrue(bookingService.isValidRange(start, end));
    }

    // 3. 09:00–09:30: valid, starts at opening time
    @Test
    void acceptsBookingStartingAtOpeningTime() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 9, 30);

        assertTrue(bookingService.isValidRange(start, end));
    }

    // 4. 10:00–10:00: invalid, zero duration
    @Test
    void rejectsBookingWithSameStartAndEnd() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 10, 0);

        assertFalse(bookingService.isValidRange(start, end));
    }

    // 5. 11:00–10:00: invalid, end before start
    @Test
    void rejectsBookingWhenEndIsBeforeStart() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 11, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 10, 0);

        assertFalse(bookingService.isValidRange(start, end));
    }

    // 6. 17:00 today–10:00 tomorrow: invalid, different days
    @Test
    void rejectsBookingAcrossDifferentDays() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 17, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 9, 10, 0);

        assertFalse(bookingService.isValidRange(start, end));
    }

    // 7. 17:30–18:00: valid, ends exactly at closing time
    @Test
    void acceptsBookingEndingAtClosingTime() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 17, 30);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 18, 0);

        assertTrue(bookingService.isValidRange(start, end));
    }

    // 8. 08:30–09:30: invalid, starts before opening time
    @Test
    void rejectsBookingStartingBeforeOpeningTime() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 8, 30);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 9, 30);

        assertFalse(bookingService.isValidRange(start, end));
    }

    // 9. 17:30–18:30: invalid, ends after closing time
    @Test
    void rejectsBookingEndingAfterClosingTime() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 17, 30);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 18, 30);

        assertFalse(bookingService.isValidRange(start, end));
    }
}