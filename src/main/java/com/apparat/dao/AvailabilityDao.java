package com.apparat.dao;

import com.apparat.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class AvailabilityDao {

    /**
     * Start times of every slot already taken for one instrument on one day.
     * Cancelled bookings leave no slot rows, so they never appear here.
     */
    public Set<LocalDateTime> findBookedSlotStarts(
            int resourceId,
            LocalDate date
    ) throws SQLException {

        String sql = """
                SELECT slot_start
                FROM booking_slots
                WHERE resource_id = ?
                  AND slot_start >= ?
                  AND slot_start < ?
                """;

        Set<LocalDateTime> bookedStarts = new HashSet<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, resourceId);
            statement.setTimestamp(
                    2, Timestamp.valueOf(date.atStartOfDay()));
            statement.setTimestamp(
                    3, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bookedStarts.add(
                            resultSet.getTimestamp("slot_start")
                                    .toLocalDateTime()
                    );
                }
            }
        }

        return bookedStarts;
    }
}
