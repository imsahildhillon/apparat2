package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.BookingSlotDao;
import com.apparat.model.Booking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceDbTests {

    private final BookingService bookingService =
            new BookingService();

    private final List<Integer> createdBookingIds =
            new ArrayList<>();

    private final int userId = 1;
    private final int resourceId = 1;

    private final LocalDateTime start =
            LocalDateTime.of(2026, 10, 25, 10, 0);

    private final LocalDateTime end =
            LocalDateTime.of(2026, 10, 25, 11, 0);

    @Test
    void createsBookingWithTwoSlots() throws SQLException {
        int bookingsBefore = countRows("bookings");
        int slotsBefore = countRows("booking_slots");

        Booking savedBooking = bookingService.createBooking(
                userId, resourceId, start, end
        );

        createdBookingIds.add(savedBooking.getId());

        assertEquals(bookingsBefore + 1, countRows("bookings"));
        assertEquals(slotsBefore + 2, countRows("booking_slots"));
    }

    @Test
    void rollsBackWhenSlotIsTaken() throws SQLException {
        int bookingsBefore = countRows("bookings");
        int slotsBefore = countRows("booking_slots");

        Booking savedBooking = bookingService.createBooking(
                userId, resourceId, start, end
        );

        createdBookingIds.add(savedBooking.getId());

        assertEquals(bookingsBefore + 1, countRows("bookings"));
        assertEquals(slotsBefore + 2, countRows("booking_slots"));

        LocalDateTime conflictStart =
                LocalDateTime.of(2026, 10, 25, 10, 30);

        LocalDateTime conflictEnd =
                LocalDateTime.of(2026, 10, 25, 11, 30);

        assertThrows(
                SlotConflictException.class,
                () -> bookingService.createBooking(
                        userId,
                        resourceId,
                        conflictStart,
                        conflictEnd
                )
        );

        // The failed attempt must not add any rows.
        assertEquals(bookingsBefore + 1, countRows("bookings"));
        assertEquals(slotsBefore + 2, countRows("booking_slots"));
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                BookingSlotDao slotDao = new BookingSlotDao();

                for (int bookingId : createdBookingIds) {
                    slotDao.deleteByBookingId(connection, bookingId);

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         "DELETE FROM bookings WHERE id = ?")) {

                        statement.setInt(1, bookingId);
                        statement.executeUpdate();
                    }
                }

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

    private int countRows(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}

