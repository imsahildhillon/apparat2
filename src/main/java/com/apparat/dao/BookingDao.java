
package com.apparat.dao;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.BookingView;
import com.apparat.model.Booking;
import com.apparat.model.BookingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /**
     * Returns bookings belonging to one user, including resource names.
     */
    public List<BookingView> findByUserId(int userId) throws SQLException {

        String sql = """
                SELECT b.id,
                       r.name AS resource_name,
                       b.start_at,
                       b.end_at,
                       b.status
                FROM bookings b
                JOIN resources r ON r.id = b.resource_id
                WHERE b.user_id = ?
                ORDER BY b.start_at ASC, b.id ASC
                """;

        List<BookingView> bookings = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    bookings.add(new BookingView(
                            rs.getInt("id"),
                            rs.getString("resource_name"),
                            rs.getTimestamp("start_at").toLocalDateTime(),
                            rs.getTimestamp("end_at").toLocalDateTime(),
                            rs.getString("status")
                    ));
                }
            }
        }

        return bookings;
    }

    public Optional<Booking> findByIdForUpdate(
            Connection connection, int bookingId) throws SQLException {

        String sql = """
                SELECT id, user_id, resource_id, start_at, end_at, status
                FROM bookings
                WHERE id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new Booking(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("resource_id"),
                        rs.getTimestamp("start_at").toLocalDateTime(),
                        rs.getTimestamp("end_at").toLocalDateTime(),
                        BookingStatus.valueOf(rs.getString("status"))
                ));
            }
        }
    }


    public void updateStatus(
            Connection connection,
            int bookingId,
            BookingStatus status) throws SQLException {

        String sql = """
                UPDATE bookings
                SET status = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, bookingId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated != 1) {
                throw new SQLException(
                        "Updating booking status failed: expected one row."
                );
            }
        }
    }


    public void deleteSlots(Connection connection, int bookingId)
            throws SQLException {

        String sql = """
                DELETE FROM booking_slots
                WHERE booking_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            statement.executeUpdate();
        }
    }
}

