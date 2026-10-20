package com.apparat.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class BookingSlotDao {

    public void insert(
            Connection connection,
            int resourceId,
            LocalDateTime slotStart,
            int bookingId
    ) throws SQLException {

        String sql = """
                INSERT INTO booking_slots
                    (resource_id, slot_start, booking_id)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, resourceId);
            statement.setTimestamp(2, Timestamp.valueOf(slotStart));
            statement.setInt(3, bookingId);

            statement.executeUpdate();
        }
    }

    public int deleteByBookingId(
            Connection connection,
            int bookingId
    ) throws SQLException {

        String sql = """
                DELETE FROM booking_slots
                WHERE booking_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, bookingId);

            return statement.executeUpdate();
        }
    }
}

