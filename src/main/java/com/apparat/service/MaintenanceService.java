package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.MaintenanceDao;
import com.apparat.dao.ResourceDao;
import com.apparat.dto.MaintenanceView;
import com.apparat.model.Resource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Maintenance windows: periods when an instrument cannot be booked.
 *
 * Adding a window and creating a booking both lock the instrument's row
 * (SELECT ... FOR UPDATE) before checking for overlaps, so the two take
 * turns. A booking and a window can never both be saved for the same time.
 */
public class MaintenanceService {

    public static final int MAX_REASON_LENGTH = 200;

    private final ResourceDao resourceDao = new ResourceDao();
    private final MaintenanceDao maintenanceDao = new MaintenanceDao();
    private final BookingService bookingService = new BookingService();

    public int addWindow(
            int resourceId,
            LocalDateTime start,
            LocalDateTime end,
            String reason,
            int createdBy
    ) throws SQLException {

        // 1. Same time rules as bookings: one day, on the half hour,
        //    inside opening hours. Long outages use INACTIVE instead.
        if (!bookingService.isValidRange(start, end)) {
            throw new IllegalArgumentException(
                    "Maintenance must be on one day, start and end on the "
                            + "half hour, stay within opening hours "
                            + "(09:00-18:00) and last at least 30 minutes."
            );
        }

        String cleanReason = reason == null ? null : reason.trim();

        if (cleanReason == null || cleanReason.isEmpty()) {
            throw new IllegalArgumentException("Please give a reason.");
        }

        if (cleanReason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "The reason must be at most "
                            + MAX_REASON_LENGTH + " characters."
            );
        }

        // 2. One transaction: lock, check, save.
        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Lock the instrument row first. createBooking does the same,
                // so a booking cannot slip in while we are checking.
                Optional<Resource> resource =
                        resourceDao.findByIdForUpdate(connection, resourceId);

                if (resource.isEmpty()) {
                    throw new ResourceNotFoundException(
                            "Instrument does not exist."
                    );
                }

                if (maintenanceDao.existsOverlapping(
                        connection, resourceId, start, end)) {
                    throw new MaintenanceConflictException(
                            "Another maintenance window already covers "
                                    + "part of this time."
                    );
                }

                int bookings = maintenanceDao
                        .countConfirmedBookingsOverlapping(
                                connection, resourceId, start, end);

                if (bookings > 0) {
                    throw new MaintenanceConflictException(
                            bookings + " confirmed booking(s) already "
                                    + "overlap this time. Cancel them first."
                    );
                }

                int id = maintenanceDao.insert(
                        connection, resourceId, start, end,
                        cleanReason, createdBy
                );

                connection.commit();

                return id;

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

    public void deleteWindow(int id) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (maintenanceDao.delete(connection, id) == 0) {
                throw new MaintenanceNotFoundException(
                        "Maintenance window does not exist."
                );
            }
        }
    }

    public List<MaintenanceView> listUpcoming() throws SQLException {
        return maintenanceDao.findUpcoming(LocalDateTime.now());
    }
}
