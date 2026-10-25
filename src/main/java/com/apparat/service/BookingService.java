package com.apparat.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


import com.apparat.config.DatabaseConnection;
import com.apparat.dao.BookingDao;
import com.apparat.dao.BookingSlotDao;
import com.apparat.dao.ResourceDao;
import com.apparat.model.Booking;
import com.apparat.model.BookingStatus;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;
import com.apparat.service.SlotConflictException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;



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


    public Booking createBooking(
            int userId,
            int resourceId,
            LocalDateTime start,
            LocalDateTime end
    ) throws SQLException {

        // 1. Validate the booking time range.
        if (!isValidRange(start, end)) {
            throw new IllegalArgumentException(
                    "Invalid booking time range."
            );
        }

        // 2. Generate slots before opening the database connection.
        List<LocalDateTime> slots = generateSlots(start, end);

        // 3. Create DAO objects.
        ResourceDao resourceDao = new ResourceDao();
        BookingDao bookingDao = new BookingDao();
        BookingSlotDao bookingSlotDao = new BookingSlotDao();

        // 4. Open one connection and begin the transaction.
        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // 5. Lock the resource row and check its status.
                Optional<Resource> resourceOptional =
                        resourceDao.findByIdForUpdate(
                                connection,
                                resourceId
                        );

                if (resourceOptional.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Resource does not exist."
                    );
                }

                Resource resource = resourceOptional.get();

                if (resource.getStatus() != ResourceStatus.ACTIVE) {
                    throw new IllegalStateException(
                            "Resource is not active."
                    );
                }

                // 6. Create the booking.
                Booking booking = new Booking(
                        0,
                        userId,
                        resourceId,
                        start,
                        end,
                        BookingStatus.CONFIRMED
                );

                // 7. Insert the booking and retrieve its generated ID.
                Booking savedBooking =
                        bookingDao.insert(connection, booking);

                // 8. Insert all requested booking slots.
                for (LocalDateTime slotStart : slots) {
                    try {
                        bookingSlotDao.insert(
                                connection,
                                resourceId,
                                slotStart,
                                savedBooking.getId()
                        );
                    } catch (SQLException e) {
                        // Translate duplicate-key errors from slot insertion.
                        if (e.getErrorCode() == 1062) {
                            throw new SlotConflictException(
                                    "One or more requested slots are already booked.",
                                    e
                            );
                        }

                        throw e;
                    }
                }

                // 9. Commit only when every insert succeeds.
                connection.commit();

                return savedBooking;

            } catch (SQLException | RuntimeException e) {
                // Roll back every change if the transaction fails.
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }

                throw e;
            }
        }
    }

}
