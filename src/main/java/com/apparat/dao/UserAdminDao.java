package com.apparat.dao;

import com.apparat.dto.UserAccount;
import com.apparat.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Write operations for user accounts. Reads for login stay in UserDao.
 */
public class UserAdminDao {

    public int insert(
            Connection connection,
            String name,
            String email,
            Role role,
            String passwordHash
    ) throws SQLException {

        String sql = """
                INSERT INTO users (name, email, role, password_hash)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, role.name());
            statement.setString(4, passwordHash);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }

                throw new SQLException(
                        "Creating user failed: no generated ID returned."
                );
            }
        }
    }

    public int updateRole(Connection connection, int id, Role role)
            throws SQLException {

        return run(connection, "UPDATE users SET role = ? WHERE id = ?",
                role.name(), id);
    }

    public int updatePasswordHash(Connection connection, int id, String hash)
            throws SQLException {

        return run(connection,
                "UPDATE users SET password_hash = ? WHERE id = ?", hash, id);
    }

    public int updateActive(Connection connection, int id, boolean active)
            throws SQLException {

        return run(connection, "UPDATE users SET active = ? WHERE id = ?",
                active ? 1 : 0, id);
    }

    /**
     * Locks every active administrator's row until the transaction ends and
     * returns how many there are. Role changes take this first, so two of
     * them can never run at the same time.
     */
    public int lockActiveAdmins(Connection connection) throws SQLException {

        String sql = """
                SELECT id FROM users
                WHERE role = 'ADMIN' AND active = 1
                FOR UPDATE
                """;

        int count = 0;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                count++;
            }
        }

        return count;
    }

    public Optional<UserAccount> findAccountForUpdate(
            Connection connection, int id) throws SQLException {

        String sql = """
                SELECT id, name, email, role, active
                FROM users
                WHERE id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new UserAccount(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("email"),
                            Role.valueOf(resultSet.getString("role")),
                            resultSet.getBoolean("active")
                    ));
                }

                return Optional.empty();
            }
        }
    }

    private int run(Connection connection, String sql, Object value, int id)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            statement.setInt(2, id);

            return statement.executeUpdate();
        }
    }
}
