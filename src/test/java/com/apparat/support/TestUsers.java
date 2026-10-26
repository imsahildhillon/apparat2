package com.apparat.support;

import com.apparat.config.DatabaseConnection;
import com.apparat.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates and removes throwaway users for tests. Every email starts with
 * "zz." so cleanUp can remove them all without touching real data.
 */
public class TestUsers {

    public static final String PREFIX = "zz.";
    public static final String PASSWORD = "Test#Pass1";

    private TestUsers() {
    }

    public static int create(String shortName, String role, boolean active)
            throws SQLException {

        String sql = """
                INSERT INTO users (name, email, role, password_hash, active)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, "ZZ " + shortName);
            statement.setString(2, email(shortName));
            statement.setString(3, role);
            statement.setString(4, PasswordUtil.hash(PASSWORD));
            statement.setBoolean(5, active);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public static String email(String shortName) {
        return PREFIX + shortName.toLowerCase() + "@apparat.test";
    }

    public static void setActive(int id, boolean active) throws SQLException {
        run("UPDATE users SET active = ? WHERE id = ?", active ? 1 : 0, id);
    }

    public static void setRole(int id, String role) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET role = ? WHERE id = ?")) {

            statement.setString(1, role);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public static void delete(int id) throws SQLException {
        run("DELETE FROM users WHERE id = ?", id);
    }

    /**
     * Removes every test user, and anything that points at them.
     */
    public static void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String[] statements = {
                    "DELETE FROM booking_slots WHERE booking_id IN "
                            + "(SELECT id FROM bookings WHERE user_id IN "
                            + "(SELECT id FROM users WHERE email LIKE 'zz.%'))",
                    "DELETE FROM bookings WHERE user_id IN "
                            + "(SELECT id FROM users WHERE email LIKE 'zz.%')",
                    "DELETE FROM maintenance_windows WHERE created_by IN "
                            + "(SELECT id FROM users WHERE email LIKE 'zz.%')",
                    "DELETE FROM users WHERE email LIKE 'zz.%'"
            };

            for (String sql : statements) {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {
                    statement.executeUpdate();
                }
            }
        }
    }

    private static void run(String sql, int... values) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < values.length; i++) {
                statement.setInt(i + 1, values[i]);
            }

            statement.executeUpdate();
        }
    }
}
