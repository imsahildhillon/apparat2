package com.apparat.service;

import com.apparat.dto.SessionUser;
import com.apparat.model.Role;
import com.apparat.support.TestUsers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An administrator's changes reach a logged-in user on their next request.
 */
class AccountStateDbTests {

    private final AuthService authService = new AuthService();

    private int userId;
    private SessionUser sessionUser;

    @BeforeEach
    void createUser() throws SQLException {
        TestUsers.cleanUp();
        userId = TestUsers.create("state", "STUDENT", true);
        sessionUser = new SessionUser(userId, "ZZ state", Role.STUDENT);
    }

    @AfterEach
    void cleanUp() throws SQLException {
        TestUsers.cleanUp();
    }

    @Test
    void anActiveUserStaysLoggedIn() throws SQLException {
        Optional<SessionUser> fresh = authService.refresh(sessionUser);

        assertTrue(fresh.isPresent());
        assertEquals(Role.STUDENT, fresh.get().role());
    }

    @Test
    void aRoleChangeShowsUpOnTheNextRefresh() throws SQLException {
        TestUsers.setRole(userId, "TECHNICIAN");

        // The session still says STUDENT, the database now says TECHNICIAN.
        Optional<SessionUser> fresh = authService.refresh(sessionUser);

        assertEquals(Role.TECHNICIAN, fresh.orElseThrow().role());
    }

    @Test
    void aDeactivatedUserIsLoggedOutOnTheNextRefresh() throws SQLException {
        TestUsers.setActive(userId, false);

        assertEquals(Optional.empty(), authService.refresh(sessionUser));
    }

    @Test
    void aDeletedUserIsLoggedOutOnTheNextRefresh() throws SQLException {
        TestUsers.delete(userId);

        assertEquals(Optional.empty(), authService.refresh(sessionUser));
    }

    @Test
    void aDeactivatedUserCannotLogInEvenWithTheRightPassword()
            throws SQLException {

        TestUsers.setActive(userId, false);

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(
                        TestUsers.email("state"), TestUsers.PASSWORD));

        assertEquals("This account has been deactivated. "
                + "Please contact an administrator.", exception.getMessage());
    }

    @Test
    void aWrongPasswordOnADeactivatedAccountRevealsNothing()
            throws SQLException {

        TestUsers.setActive(userId, false);

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(TestUsers.email("state"), "wrong"));

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void reactivatingAllowsLoginAgain() throws SQLException {
        TestUsers.setActive(userId, false);
        TestUsers.setActive(userId, true);

        SessionUser user = authService.login(
                TestUsers.email("state"), TestUsers.PASSWORD);

        assertEquals(userId, user.id());
    }
}
