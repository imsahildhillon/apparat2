package com.apparat.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every booking rule explains itself in plain words.
 */
class BookingMessageTests {

    private final BookingService bookingService = new BookingService();

    private LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2030, 3, day, hour, minute);
    }

    private String error(LocalDateTime start, LocalDateTime end) {
        return bookingService.findRangeError(start, end).orElse(null);
    }

    @Test
    void aValidRangeHasNoError() {
        assertEquals(Optional.empty(),
                bookingService.findRangeError(at(12, 10, 0), at(12, 11, 30)));
    }

    @Test
    void missingTimesAreExplained() {
        assertEquals("Please choose a start time and an end time.",
                error(null, at(12, 11, 0)));
        assertEquals("Please choose a start time and an end time.",
                error(at(12, 10, 0), null));
    }

    @Test
    void endBeforeOrEqualToStartIsExplained() {
        assertEquals("The end time must be after the start time.",
                error(at(12, 11, 0), at(12, 10, 0)));
        assertEquals("The end time must be after the start time.",
                error(at(12, 10, 0), at(12, 10, 0)));
    }

    @Test
    void tooShortIsExplained() {
        // Only possible off the half hour grid or under 30 minutes.
        assertEquals("A booking must be at least 30 minutes long.",
                error(at(12, 10, 0), at(12, 10, 29)));
    }

    @Test
    void differentDaysAreExplained() {
        assertEquals("A booking must start and end on the same day.",
                error(at(12, 17, 0), at(13, 10, 0)));
    }

    @Test
    void offTheHalfHourIsExplained() {
        String expected = "Bookings must start and end on the hour or half "
                + "hour, for example 10:00 or 10:30.";

        assertEquals(expected, error(at(12, 10, 10), at(12, 10, 40)));
        assertEquals(expected, error(at(12, 10, 0), at(12, 10, 45)));
    }

    @Test
    void outsideOpeningHoursIsExplainedWithTheHours() {
        String expected = "The lab is open from 09:00 to 18:00. "
                + "Please choose a time within these hours.";

        assertEquals(expected, error(at(12, 8, 30), at(12, 9, 30)));
        assertEquals(expected, error(at(12, 17, 30), at(12, 18, 30)));
        assertEquals(expected, error(at(12, 18, 0), at(12, 18, 30)));
    }

    @Test
    void isValidRangeAgreesWithTheExplanation() {
        LocalDateTime[][] cases = {
                {at(12, 10, 0), at(12, 11, 0)},
                {at(12, 10, 0), at(12, 10, 0)},
                {at(12, 10, 10), at(12, 10, 40)},
                {at(12, 8, 0), at(12, 9, 0)},
                {at(12, 17, 0), at(13, 10, 0)},
                {null, null},
        };

        for (LocalDateTime[] c : cases) {
            assertEquals(
                    bookingService.findRangeError(c[0], c[1]).isEmpty(),
                    bookingService.isValidRange(c[0], c[1]));
        }

        assertTrue(bookingService.isValidRange(at(12, 10, 0), at(12, 11, 0)));
        assertFalse(bookingService.isValidRange(at(12, 10, 0), at(12, 10, 0)));
    }

    @Test
    void createBookingRejectsAnInvalidRangeWithTheSpecificReason() {
        // The range is checked before any database work, so no DB is needed.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        1, 1, at(12, 10, 10), at(12, 10, 40)));

        assertEquals("Bookings must start and end on the hour or half "
                + "hour, for example 10:00 or 10:30.", exception.getMessage());
    }

    @Test
    void generateSlotsGivesTheSameSpecificReason() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.generateSlots(at(12, 7, 0), at(12, 8, 0)));

        assertTrue(exception.getMessage().startsWith("The lab is open from"));
    }
}
