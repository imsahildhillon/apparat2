package com.apparat.dao;

import com.apparat.model.ResourceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Write operations for instruments. Reads stay in ResourceDao.
 */
public class ResourceAdminDao {

    public int insert(
            Connection connection,
            String name,
            String location,
            ResourceStatus status
    ) throws SQLException {

        String sql = """
                INSERT INTO resources (name, location, status)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setString(2, location);
            statement.setString(3, status.name());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }

                throw new SQLException(
                        "Creating resource failed: no generated ID returned."
                );
            }
        }
    }

    public int update(
            Connection connection,
            int id,
            String name,
            String location
    ) throws SQLException {

        String sql = """
                UPDATE resources
                SET name = ?, location = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, location);
            statement.setInt(3, id);

            return statement.executeUpdate();
        }
    }

    public int updateStatus(
            Connection connection,
            int id,
            ResourceStatus status
    ) throws SQLException {

        String sql = """
                UPDATE resources
                SET status = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, id);

            return statement.executeUpdate();
        }
    }
}
