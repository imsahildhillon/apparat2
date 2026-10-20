package com.apparat.dao;

import com.apparat.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class BookingDao {
    public Booking insert(Connection connection, Booking booking)
            throws SQLException {

        String sql = """
                INSERT INTO bookings
                    (user_id, resource_id, start_at, end_at, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, booking.getUserId());
            statement.setInt(2, booking.getResourceId());
            statement.setTimestamp(
                    3, Timestamp.valueOf(booking.getStart()));
            statement.setTimestamp(
                    4, Timestamp.valueOf(booking.getEnd()));
            statement.setString(5, booking.getStatus().name());

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted != 1) {
                throw new SQLException(
                        "Creating booking failed: expected one inserted row."
                );
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int bookingId = generatedKeys.getInt(1);

                    // Return a Booking containing the database-generated ID.
                    return new Booking(
                            bookingId,
                            booking.getUserId(),
                            booking.getResourceId(),
                            booking.getStart(),
                            booking.getEnd(),
                            booking.getStatus()
                    );
                }

                throw new SQLException(
                        "Creating booking failed: no generated ID returned."
                );
            }
        }
    }
}
