package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.UserAdminDao;
import com.apparat.dao.UserDao;
import com.apparat.dto.UserAccount;
import com.apparat.model.Role;
import com.apparat.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What an administrator may do with user accounts. Only administrators reach
 * this class (AuthFilter checks MANAGE_USERS), and the rules here stop an
 * administrator from locking the system out of administration.
 */
public class UserAdminService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 255;

    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao userDao = new UserDao();
    private final UserAdminDao userAdminDao = new UserAdminDao();

    public List<UserAccount> listUsers() throws SQLException {
        return userDao.findAll();
    }

    public int createUser(
            String name,
            String email,
            String roleText,
            String password
    ) throws SQLException {

        // 1. Validate every field and say exactly what is wrong.
        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Please enter a name.");
        }

        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "The name must be at most " + MAX_NAME_LENGTH
                            + " characters."
            );
        }

        String cleanEmail = email == null ? "" : email.trim();

        if (cleanEmail.length() > MAX_EMAIL_LENGTH
                || !EMAIL.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address."
            );
        }

        Role role = parseRole(roleText);
        validatePassword(password);

        // 2. Save. The database allows each email only once.
        try (Connection connection = DatabaseConnection.getConnection()) {
            return userAdminDao.insert(
                    connection, cleanName, cleanEmail, role,
                    PasswordUtil.hash(password)
            );

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DuplicateUserException(
                        "A user with this email already exists."
                );
            }

            throw e;
        }
    }

    public void changeRole(int actingAdminId, int userId, Role newRole)
            throws SQLException {

        if (newRole == null) {
            throw new IllegalArgumentException(
                    "Please choose a valid role."
            );
        }

        if (userId == actingAdminId) {
            throw new UserRuleException("You cannot change your own role.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Lock the administrators first, then the user. Every change
                // that could remove an administrator does it in this order.
                int activeAdmins = userAdminDao.lockActiveAdmins(connection);

                UserAccount target = lockUser(connection, userId);

                boolean removesAnAdmin = target.role() == Role.ADMIN
                        && target.active()
                        && newRole != Role.ADMIN;

                if (removesAnAdmin && activeAdmins <= 1) {
                    throw new UserRuleException(LAST_ADMIN);
                }

                userAdminDao.updateRole(connection, userId, newRole);

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        }
    }

    public void setActive(int actingAdminId, int userId, boolean active)
            throws SQLException {

        if (userId == actingAdminId && !active) {
            throw new UserRuleException(
                    "You cannot deactivate your own account."
            );
        }

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int activeAdmins = userAdminDao.lockActiveAdmins(connection);

                UserAccount target = lockUser(connection, userId);

                boolean removesAnAdmin = target.role() == Role.ADMIN
                        && target.active()
                        && !active;

                if (removesAnAdmin && activeAdmins <= 1) {
                    throw new UserRuleException(LAST_ADMIN);
                }

                userAdminDao.updateActive(connection, userId, active);

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        }
    }

    public void resetPassword(int userId, String newPassword)
            throws SQLException {

        validatePassword(newPassword);

        try (Connection connection = DatabaseConnection.getConnection()) {
            int rows = userAdminDao.updatePasswordHash(
                    connection, userId, PasswordUtil.hash(newPassword)
            );

            if (rows == 0) {
                throw new UserNotFoundException("That user does not exist.");
            }
        }
    }

    private static final String LAST_ADMIN =
            "There must always be at least one active administrator.";

    private Role parseRole(String roleText) {

        if (roleText == null || roleText.isBlank()) {
            throw new IllegalArgumentException("Please choose a role.");
        }

        try {
            return Role.valueOf(roleText.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Please choose a valid role.");
        }
    }

    private void validatePassword(String password) {

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "The password must be at least "
                            + MIN_PASSWORD_LENGTH + " characters long."
            );
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "The password must be at most "
                            + MAX_PASSWORD_LENGTH + " characters long."
            );
        }

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException(
                    "The password must contain at least one letter "
                            + "and one digit."
            );
        }
    }

    private UserAccount lockUser(Connection connection, int userId)
            throws SQLException {

        Optional<UserAccount> user =
                userAdminDao.findAccountForUpdate(connection, userId);

        if (user.isEmpty()) {
            throw new UserNotFoundException("That user does not exist.");
        }

        return user.get();
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }
}
