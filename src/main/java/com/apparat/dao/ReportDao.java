package com.apparat.dao;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.UsageCount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    /**
     * Booked slots and bookings per instrument between two moments
     * (from inclusive, to exclusive). Instruments with no bookings
     * still appear, with zeros.
     */
    public List<UsageCount> findUsageCounts(
            LocalDateTime from,
            LocalDateTime to
    ) throws SQLException {

        String sql = """
                SELECT r.id,
                       r.name,
                       COUNT(DISTINCT s.booking_id) AS bookings,
                       COUNT(s.slot_start) AS slots
                FROM resources r
                LEFT JOIN booking_slots s
                       ON s.resource_id = r.id
                      AND s.slot_start >= ?
                      AND s.slot_start < ?
                GROUP BY r.id, r.name
                ORDER BY r.name ASC
                """;

        List<UsageCount> counts = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.add(new UsageCount(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("bookings"),
                            resultSet.getInt("slots")
                    ));
                }
            }
        }

        return counts;
    }
}
