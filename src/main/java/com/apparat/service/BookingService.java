package com.apparat.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


import com.apparat.config.DatabaseConnection;
import com.apparat.dao.BookingDao;
import com.apparat.dao.BookingCountDao;
import com.apparat.dao.BookingSlotDao;
import com.apparat.dao.MaintenanceDao;
import com.apparat.dao.ResourceDao;
import com.apparat.dao.UserDao;
import com.apparat.dto.BookingView;
import com.apparat.model.Booking;
import com.apparat.model.BookingStatus;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;
import com.apparat.model.Role;
import com.apparat.policy.BookingPolicy;
import com.apparat.service.SlotConflictException;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;



public class BookingService {

    public static final LocalTime OPEN = LocalTime.of(9, 0);
    public static final LocalTime CLOSE = LocalTime.of(18, 0);
    public static final int SLOT_MINUTES = 30;

    private final BookingDao bookingDao = new BookingDao();
    private final MaintenanceDao maintenanceDao = new MaintenanceDao();
    private final UserDao userDao = new UserDao();
    private final BookingCountDao bookingCountDao = new BookingCountDao();

    public boolean isValidRange(LocalDateTime start, LocalDateTime end) {
        return findRangeError(start, end).isEmpty();
    }

    /**
     * Says which rule a booking time range breaks, in plain words,
     * or returns empty when the range is fine. isValidRange uses this too,
     * so the rules exist in one place only.
     */
    public Optional<String> findRangeError(
            LocalDateTime start,
            LocalDateTime end
    ) {

        // 1. Start and end must not be null
        if (start == null || end == null) {
            return Optional.of("Please choose a start time and an end time.");
        }

        // 2. Start time must be before end time
        if (!start.isBefore(end)) {
            return Optional.of("The end time must be after the start time.");
        }

        // 3. Booking duration must be at least 30 minutes
        Duration duration = Duration.between(start, end);

        if (duration.toMinutes() < SLOT_MINUTES) {
            return Optional.of(
                    "A booking must be at least "
                            + SLOT_MINUTES + " minutes long."
            );
        }

        // 4. Booking must be on the same day
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return Optional.of("A booking must start and end on the same day.");
        }

        // 5. Start and end must align with 30-minute boundaries
        if (!isSlotBoundary(start.toLocalTime())
                || !isSlotBoundary(end.toLocalTime())) {
            return Optional.of(
                    "Bookings must start and end on the hour or half hour, "
                            + "for example 10:00 or 10:30."
            );
        }

        // 6. Booking must fall within operating hours
        if (start.toLocalTime().isBefore(OPEN)
                || !start.toLocalTime().isBefore(CLOSE)
                || end.toLocalTime().isBefore(OPEN)
                || end.toLocalTime().isAfter(CLOSE)) {
            return Optional.of(
                    "The lab is open from " + OPEN + " to " + CLOSE
                            + ". Please choose a time within these hours."
            );
        }

        return Optional.empty();
    }

    private boolean isSlotBoundary(LocalTime time) {
        return time.getMinute() % SLOT_MINUTES == 0
                && time.getSecond() == 0
                && time.getNano() == 0;
    }

    public List<LocalDateTime> generateSlots(
            LocalDateTime start,
            LocalDateTime end) {

        Optional<String> rangeError = findRangeError(start, end);

        if (rangeError.isPresent()) {
            throw new IllegalArgumentException(rangeError.get());
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
        Optional<String> rangeError = findRangeError(start, end);

        if (rangeError.isPresent()) {
            throw new IllegalArgumentException(rangeError.get());
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
                            "That instrument does not exist."
                    );
                }

                Resource resource = resourceOptional.get();

                if (resource.getStatus() != ResourceStatus.ACTIVE) {
                    throw new IllegalStateException(
                            "This instrument is not available for booking right now."
                    );
                }

                // Maintenance windows block bookings. The instrument row is
                // already locked, so no window can be added while we check.
                if (maintenanceDao.existsOverlapping(
                        connection, resourceId, start, end)) {
                    throw new MaintenanceConflictException(
                            "The instrument is under maintenance at that time."
                    );
                }

                // Limits for this user's role. The user row is locked first,
                // so two bookings by the same person cannot both pass the
                // "active bookings" count at the same moment.
                Optional<Role> role =
                        userDao.findRoleForUpdate(connection, userId);

                if (role.isEmpty()) {
                    throw new IllegalArgumentException(
                            "That user does not exist."
                    );
                }

                int activeBookings = bookingCountDao.countActiveByUser(
                        connection, userId, LocalDateTime.now()
                );

                Optional<String> limitError = BookingPolicy
                        .forRole(role.get())
                        .check(start, end, activeBookings);

                if (limitError.isPresent()) {
                    throw new BookingLimitException(limitError.get());
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
    public List<BookingView> listBookings(int userId)
            throws SQLException {

        return bookingDao.findByUserId(userId);
    }

    public void cancelBooking(int bookingId, int userId)
            throws SQLException {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Lock the booking row before checking its state.
                Optional<Booking> bookingOptional =
                        bookingDao.findByIdForUpdate(
                                connection, bookingId
                        );

                if (bookingOptional.isEmpty()) {
                    throw new BookingNotFoundException(
                            "Booking does not exist."
                    );
                }

                Booking booking = bookingOptional.get();

                // Enforce ownership in the service layer.
                if (booking.getUserId() != userId) {
                    throw new BookingOwnershipException(
                            "You cannot cancel another user's booking."
                    );
                }

                // Only confirmed bookings can be cancelled.
                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                    throw new BookingStateException(
                            "This booking is no longer active."
                    );
                }

                // Both operations use this transaction's connection.
                bookingDao.updateStatus(
                        connection,
                        bookingId,
                        BookingStatus.CANCELLED
                );

                bookingDao.deleteSlots(connection, bookingId);

                connection.commit();

            } catch (SQLException | RuntimeException e) {
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
