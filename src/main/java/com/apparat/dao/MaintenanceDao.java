package com.apparat.dao;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.MaintenanceView;
import com.apparat.model.MaintenanceWindow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Two periods overlap when each one starts before the other ends:
 * existing.start < new.end AND existing.end > new.start.
 * A window that ends exactly when another begins does not overlap it.
 */
public class MaintenanceDao {

    public int insert(
            Connection connection,
            int resourceId,
            LocalDateTime start,
            LocalDateTime end,
            String reason,
            int createdBy
    ) throws SQLException {

        String sql = """
                INSERT INTO maintenance_windows
                    (resource_id, start_at, end_at, reason, created_by)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, resourceId);
            statement.setTimestamp(2, Timestamp.valueOf(start));
            statement.setTimestamp(3, Timestamp.valueOf(end));
            statement.setString(4, reason);
            statement.setInt(5, createdBy);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }

                throw new SQLException(
                        "Creating maintenance window failed: no ID returned."
                );
            }
        }
    }

    public int delete(Connection connection, int id) throws SQLException {

        String sql = "DELETE FROM maintenance_windows WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            return statement.executeUpdate();
        }
    }

    /**
     * Is any maintenance window overlapping this period?
     * Runs on the caller's connection, so it sees the caller's transaction.
     */
    public boolean existsOverlapping(
            Connection connection,
            int resourceId,
            LocalDateTime start,
            LocalDateTime end
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM maintenance_windows
                WHERE resource_id = ?
                  AND start_at < ?
                  AND end_at > ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resourceId);
            statement.setTimestamp(2, Timestamp.valueOf(end));
            statement.setTimestamp(3, Timestamp.valueOf(start));

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    /**
     * Confirmed bookings overlapping this period. Cancelled ones do not count.
     */
    public int countConfirmedBookingsOverlapping(
            Connection connection,
            int resourceId,
            LocalDateTime start,
            LocalDateTime end
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM bookings
                WHERE resource_id = ?
                  AND status = 'CONFIRMED'
                  AND start_at < ?
                  AND end_at > ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resourceId);
            statement.setTimestamp(2, Timestamp.valueOf(end));
            statement.setTimestamp(3, Timestamp.valueOf(start));

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    /**
     * Windows overlapping a period, used to mark the availability grid.
     */
    public List<MaintenanceWindow> findOverlapping(
            int resourceId,
            LocalDateTime from,
            LocalDateTime to
    ) throws SQLException {

        String sql = """
                SELECT id, resource_id, start_at, end_at, reason, created_by
                FROM maintenance_windows
                WHERE resource_id = ?
                  AND start_at < ?
                  AND end_at > ?
                ORDER BY start_at ASC
                """;

        List<MaintenanceWindow> windows = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, resourceId);
            statement.setTimestamp(2, Timestamp.valueOf(to));
            statement.setTimestamp(3, Timestamp.valueOf(from));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    windows.add(new MaintenanceWindow(
                            resultSet.getInt("id"),
                            resultSet.getInt("resource_id"),
                            resultSet.getTimestamp("start_at")
                                    .toLocalDateTime(),
                            resultSet.getTimestamp("end_at")
                                    .toLocalDateTime(),
                            resultSet.getString("reason"),
                            resultSet.getInt("created_by")
                    ));
                }
            }
        }

        return windows;
    }

    /**
     * Windows that have not finished yet, for the staff page.
     */
    public List<MaintenanceView> findUpcoming(LocalDateTime now)
            throws SQLException {

        String sql = """
                SELECT m.id, r.name AS resource_name,
                       m.start_at, m.end_at, m.reason
                FROM maintenance_windows m
                JOIN resources r ON r.id = m.resource_id
                WHERE m.end_at > ?
                ORDER BY m.start_at ASC, m.id ASC
                """;

        List<MaintenanceView> windows = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setTimestamp(1, Timestamp.valueOf(now));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    windows.add(new MaintenanceView(
                            resultSet.getInt("id"),
                            resultSet.getString("resource_name"),
                            resultSet.getTimestamp("start_at")
                                    .toLocalDateTime(),
                            resultSet.getTimestamp("end_at")
                                    .toLocalDateTime(),
                            resultSet.getString("reason")
                    ));
                }
            }
        }

        return windows;
    }
}
