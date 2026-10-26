package com.apparat.dao;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.UserAccount;
import com.apparat.dto.UserCredentials;
import com.apparat.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public Optional<UserCredentials> findCredentialsByEmail(String email)
            throws SQLException {

        String sql = """
                SELECT id, name, email, role, password_hash, active
                FROM users
                WHERE email = ?
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new UserCredentials(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("email"),
                            Role.valueOf(resultSet.getString("role")),
                            resultSet.getString("password_hash"),
                            resultSet.getBoolean("active")
                    ));
                }

                return Optional.empty();
            }
        }
    }

    public Optional<UserAccount> findAccountById(int id) throws SQLException {

        String sql = """
                SELECT id, name, email, role, active
                FROM users
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
                    return Optional.of(mapAccount(resultSet));
                }

                return Optional.empty();
            }
        }
    }

    public List<UserAccount> findAll() throws SQLException {

        String sql = """
                SELECT id, name, email, role, active
                FROM users
                ORDER BY id ASC
                """;

        List<UserAccount> users = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                users.add(mapAccount(resultSet));
            }
        }

        return users;
    }

    /**
     * The user's role, with the user row locked until the transaction ends.
     * Used while booking, so two bookings by one person run one after the other.
     */
    public Optional<Role> findRoleForUpdate(Connection connection, int id)
            throws SQLException {

        String sql = "SELECT role FROM users WHERE id = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(Role.valueOf(resultSet.getString("role")));
                }

                return Optional.empty();
            }
        }
    }

    private UserAccount mapAccount(ResultSet resultSet) throws SQLException {
        return new UserAccount(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("active")
        );
    }
}
