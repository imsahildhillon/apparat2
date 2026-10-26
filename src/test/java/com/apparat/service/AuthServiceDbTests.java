package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.SessionUser;
import com.apparat.model.Role;
import com.apparat.util.PasswordUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceDbTests {

    private static final String EMAIL = "zz.auth.test@apparat.test";
    private static final String NO_PASSWORD_EMAIL =
            "zz.nopassword.test@apparat.test";
    private static final String PASSWORD = "Correct#Horse1";

    private final AuthService authService = new AuthService();

    @BeforeEach
    void createUsers() throws SQLException {
        cleanUp();

        insertUser("ZZ Auth Test", EMAIL, "FACULTY",
                PasswordUtil.hash(PASSWORD));

        // password_hash is empty, as for a user who never got a password.
        insertUser("ZZ No Password", NO_PASSWORD_EMAIL, "STUDENT", "");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM users WHERE email IN (?, ?)")) {

            statement.setString(1, EMAIL);
            statement.setString(2, NO_PASSWORD_EMAIL);
            statement.executeUpdate();
        }
    }

    @Test
    void correctCredentialsReturnTheSessionUser() throws SQLException {
        SessionUser user = authService.login(EMAIL, PASSWORD);

        assertEquals("ZZ Auth Test", user.name());
        assertEquals(Role.FACULTY, user.role());
    }

    @Test
    void emailIsTrimmedBeforeLookup() throws SQLException {
        SessionUser user = authService.login("  " + EMAIL + "  ", PASSWORD);

        assertEquals(Role.FACULTY, user.role());
    }

    @Test
    void wrongPasswordGivesTheGenericMessage() {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(EMAIL, "wrong-password")
        );

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void unknownEmailGivesTheSameMessageAsAWrongPassword() {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login("nobody@apparat.test", PASSWORD)
        );

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void blankInputIsRejected() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("", PASSWORD));
        assertThrows(AuthenticationException.class,
                () -> authService.login(EMAIL, ""));
        assertThrows(AuthenticationException.class,
                () -> authService.login(null, null));
    }

    @Test
    void userWithoutAPasswordCannotLogIn() {
        assertThrows(AuthenticationException.class,
                () -> authService.login(NO_PASSWORD_EMAIL, ""));
        assertThrows(AuthenticationException.class,
                () -> authService.login(NO_PASSWORD_EMAIL, PASSWORD));
    }

    private void insertUser(
            String name, String email, String role, String hash
    ) throws SQLException {

        String sql = """
                INSERT INTO users (name, email, role, password_hash)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, role);
            statement.setString(4, hash);
            statement.executeUpdate();
        }
    }
}
