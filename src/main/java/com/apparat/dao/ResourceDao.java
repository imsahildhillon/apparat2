
package com.apparat.dao;

import com.apparat.config.DatabaseConnection;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceDao {

    public List<Resource> findAll() throws SQLException {
        String sql = "SELECT id, name, location, status FROM resources";

        List<Resource> resources = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Resource resource = mapRow(resultSet);
                resources.add(resource);
            }
        }

        return resources;
    }

    public Optional<Resource> findById(int id) throws SQLException {
        String sql = """
                SELECT id, name, location, status
                FROM resources
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public Optional<Resource> findByIdForUpdate(
            Connection connection,
            int id
    ) throws SQLException {

        String sql = """
                SELECT id, name, location, status
                FROM resources
                WHERE id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    private Resource mapRow(ResultSet resultSet) throws SQLException {
        return new Resource(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("location"),
                ResourceStatus.valueOf(
                        resultSet.getString("status")
                )
        );
    }
}
