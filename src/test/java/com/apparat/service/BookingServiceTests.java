package com.apparat.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void oneHourBookingProducesTwoSlots() {
        LocalDateTime start =
                LocalDateTime.of(2026, 10, 21, 10, 0);
        LocalDateTime end =
                LocalDateTime.of(2026, 10, 21, 11, 0);

        List<LocalDateTime> slots =
                bookingService.generateSlots(start, end);

        assertEquals(
                List.of(
                        LocalDateTime.of(2026, 10, 21, 10, 0),
                        LocalDateTime.of(2026, 10, 21, 10, 30)
                ),
                slots
        );
    }

    @Test
    void thirtyMinuteBookingProducesOneSlot() {
        LocalDateTime start =
                LocalDateTime.of(2026, 10, 21, 10, 0);
        LocalDateTime end =
                LocalDateTime.of(2026, 10, 21, 10, 30);

        List<LocalDateTime> slots =
                bookingService.generateSlots(start, end);

        assertEquals(
                List.of(start),
                slots
        );
    }

    @Test
    void misalignedBookingIsRejected() {
        LocalDateTime start =
                LocalDateTime.of(2026, 10, 21, 10, 10);
        LocalDateTime end =
                LocalDateTime.of(2026, 10, 21, 10, 40);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.generateSlots(start, end)
        );
    }
    @Test
    void bookingStartingAtClosingTimeIsRejected() {
        LocalDateTime start =
                LocalDateTime.of(2026, 10, 21, 18, 0);
        LocalDateTime end =
                LocalDateTime.of(2026, 10, 21, 18, 30);

        assertFalse(bookingService.isValidRange(start, end));
    }
    @Test
    void backToBackBookingsHaveNoSharedSlots() {
        LocalDateTime firstStart =
                LocalDateTime.of(2026, 10, 21, 10, 30);
        LocalDateTime firstEnd =
                LocalDateTime.of(2026, 10, 21, 11, 30);

        LocalDateTime secondStart =
                LocalDateTime.of(2026, 10, 21, 11, 30);
        LocalDateTime secondEnd =
                LocalDateTime.of(2026, 10, 21, 12, 0);

        List<LocalDateTime> firstSlots =
                bookingService.generateSlots(firstStart, firstEnd);

        List<LocalDateTime> secondSlots =
                bookingService.generateSlots(secondStart, secondEnd);

        assertFalse(
                firstSlots.stream().anyMatch(secondSlots::contains),
                "Back-to-back bookings must not share a slot."
        );

        assertEquals(
                List.of(
                        LocalDateTime.of(2026, 10, 21, 10, 30),
                        LocalDateTime.of(2026, 10, 21, 11, 0)
                ),
                firstSlots
        );

        assertEquals(
                List.of(
                        LocalDateTime.of(2026, 10, 21, 11, 30)
                ),
                secondSlots
        );
    }
}