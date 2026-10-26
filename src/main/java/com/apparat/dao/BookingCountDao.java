package com.apparat.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class BookingCountDao {

    /**
     * Confirmed bookings of one user that have not finished yet.
     * Cancelled and finished bookings do not count.
     *
     * This is a locking read (FOR UPDATE) on purpose. A plain SELECT inside
     * a transaction reads a snapshot taken earlier, so a booking another
     * thread saved while this one waited for the user lock would be missed.
     * A locking read always sees the latest saved data.
     */
    public int countActiveByUser(
            Connection connection,
            int userId,
            LocalDateTime now
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM bookings
                WHERE user_id = ?
                  AND status = 'CONFIRMED'
                  AND end_at > ?
                FOR UPDATE
                """;

        int count = 0;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setTimestamp(2, Timestamp.valueOf(now));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    count++;
                }
            }
        }

        return count;
    }
}
