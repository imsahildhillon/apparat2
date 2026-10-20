package com.apparat.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    public static final LocalTime OPEN = LocalTime.of(9, 0);
    public static final LocalTime CLOSE = LocalTime.of(18, 0);
    public static final int SLOT_MINUTES = 30;

    public boolean isValidRange(LocalDateTime start, LocalDateTime end) {

        // 1. Start and end must not be null
        if (start == null || end == null) {
            return false;
        }

        // 2. Start time must be before end time
        if (!start.isBefore(end)) {
            return false;
        }

        // 3. Booking duration must be at least 30 minutes
        Duration duration = Duration.between(start, end);

        if (duration.toMinutes() < SLOT_MINUTES) {
            return false;
        }

        // 4. Booking must be on the same day
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }

        // 5. Start and end must align with 30-minute boundaries
        if (!isSlotBoundary(start.toLocalTime())
                || !isSlotBoundary(end.toLocalTime())) {
            return false;
        }

        // 6. Booking must fall within operating hours
        if (start.toLocalTime().isBefore(OPEN)
                || !start.toLocalTime().isBefore(CLOSE)) {
            return false;
        }

        if (end.toLocalTime().isBefore(OPEN)
                || end.toLocalTime().isAfter(CLOSE)) {
            return false;
        }

        return true;
    }

    private boolean isSlotBoundary(LocalTime time) {
        return time.getMinute() % SLOT_MINUTES == 0
                && time.getSecond() == 0
                && time.getNano() == 0;
    }

    public List<LocalDateTime> generateSlots(
            LocalDateTime start,
            LocalDateTime end) {

        if (!isValidRange(start, end)) {
            throw new IllegalArgumentException(
                    "Invalid booking time range."
            );
        }

        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime current = start;

        // End time is exclusive.
        while (current.isBefore(end)) {
            slots.add(current);
            current = current.plusMinutes(SLOT_MINUTES);
        }

        return slots;
    }


}
