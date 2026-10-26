package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.SessionUser;
import com.apparat.dto.UserAccount;
import com.apparat.model.Role;
import com.apparat.support.TestUsers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAdminServiceDbTests {

    private static final String DEMO_ADMIN = "admin@apparat.test";

    private final UserAdminService userAdminService = new UserAdminService();
    private final AuthService authService = new AuthService();

    private int adminA;
    private int adminB;
    private int student;

    @BeforeEach
    void createUsers() throws SQLException {
        TestUsers.cleanUp();
        setDemoAdminActive(true);

        adminA = TestUsers.create("adminone", "ADMIN", true);
        adminB = TestUsers.create("admintwo", "ADMIN", true);
        student = TestUsers.create("somestudent", "STUDENT", true);
    }

    @AfterEach
    void cleanUp() throws SQLException {
        TestUsers.cleanUp();
        setDemoAdminActive(true);
    }

    // The demo administrator would otherwise count as "another administrator".
    private void setDemoAdminActive(boolean active) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET active = ? WHERE email = ?")) {

            statement.setInt(1, active ? 1 : 0);
            statement.setString(2, DEMO_ADMIN);
            statement.executeUpdate();
        }
    }

    // ---------------------------------------------------------- creating

    @Test
    void aCreatedUserCanLogInWithTheirPassword() throws SQLException {
        int id = userAdminService.createUser(
                "  ZZ New Person  ", "  zz.newperson@apparat.test ",
                "faculty", "Fresh#Pass9");

        SessionUser user = authService.login(
                "zz.newperson@apparat.test", "Fresh#Pass9");

        assertEquals(id, user.id());
        assertEquals("ZZ New Person", user.name());
        assertEquals(Role.FACULTY, user.role());
    }

    @Test
    void theCreatedAccountStartsActive() throws SQLException {
        int id = userAdminService.createUser(
                "ZZ Active", "zz.active@apparat.test", "STUDENT", "Fresh#Pass9");

        UserAccount account = userAdminService.listUsers().stream()
                .filter(u -> u.id() == id).findFirst().orElseThrow();

        assertTrue(account.active());
    }

    @Test
    void everyBadFieldGetsItsOwnMessage() {
        assertEquals("Please enter a name.", message(
                () -> userAdminService.createUser("  ", "zz.a@apparat.test", "STUDENT", "Fresh#Pass9")));

        assertEquals("The name must be at most 100 characters.", message(
                () -> userAdminService.createUser("x".repeat(101), "zz.a@apparat.test", "STUDENT", "Fresh#Pass9")));

        assertEquals("Please enter a valid email address.", message(
                () -> userAdminService.createUser("ZZ A", "not-an-email", "STUDENT", "Fresh#Pass9")));

        assertEquals("Please enter a valid email address.", message(
                () -> userAdminService.createUser("ZZ A", null, "STUDENT", "Fresh#Pass9")));

        assertEquals("Please choose a role.", message(
                () -> userAdminService.createUser("ZZ A", "zz.a@apparat.test", "", "Fresh#Pass9")));

        assertEquals("Please choose a valid role.", message(
                () -> userAdminService.createUser("ZZ A", "zz.a@apparat.test", "SUPERUSER", "Fresh#Pass9")));
    }

    @Test
    void weakPasswordsAreExplained() {
        assertEquals("The password must be at least 8 characters long.", message(
                () -> userAdminService.createUser("ZZ A", "zz.a@apparat.test", "STUDENT", "Ab1")));

        assertEquals("The password must contain at least one letter and one digit.", message(
                () -> userAdminService.createUser("ZZ A", "zz.a@apparat.test", "STUDENT", "onlyletters")));

        assertEquals("The password must contain at least one letter and one digit.", message(
                () -> userAdminService.createUser("ZZ A", "zz.a@apparat.test", "STUDENT", "12345678")));
    }

    @Test
    void anEmailCanBeUsedOnlyOnce() throws SQLException {
        userAdminService.createUser(
                "ZZ First", "zz.same@apparat.test", "STUDENT", "Fresh#Pass9");

        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> userAdminService.createUser(
                        "ZZ Second", "ZZ.SAME@apparat.test", "FACULTY", "Fresh#Pass9"));

        assertEquals("A user with this email already exists.",
                exception.getMessage());
    }

    // ------------------------------------------------------------- roles

    @Test
    void anAdministratorCanChangeAnotherUsersRole() throws SQLException {
        userAdminService.changeRole(adminA, student, Role.TECHNICIAN);

        assertEquals(Role.TECHNICIAN, roleOf(student));
    }

    @Test
    void youCannotChangeYourOwnRole() {
        UserRuleException exception = assertThrows(UserRuleException.class,
                () -> userAdminService.changeRole(adminA, adminA, Role.STUDENT));

        assertEquals("You cannot change your own role.", exception.getMessage());
    }

    @Test
    void anUnknownUserIsReported() {
        assertThrows(UserNotFoundException.class,
                () -> userAdminService.changeRole(adminA, 999_999, Role.STUDENT));
        assertThrows(UserNotFoundException.class,
                () -> userAdminService.setActive(adminA, 999_999, false));
        assertThrows(UserNotFoundException.class,
                () -> userAdminService.resetPassword(999_999, "Fresh#Pass9"));
    }

    @Test
    void anInvalidRoleIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> userAdminService.changeRole(adminA, student, null));
    }

    @Test
    void theLastActiveAdministratorCannotBeDemoted() throws SQLException {
        setDemoAdminActive(false);
        TestUsers.setActive(adminA, false);
        // adminB is now the only active administrator.

        UserRuleException exception = assertThrows(UserRuleException.class,
                () -> userAdminService.changeRole(student, adminB, Role.STUDENT));

        assertEquals("There must always be at least one active administrator.",
                exception.getMessage());
        assertEquals(Role.ADMIN, roleOf(adminB));
    }

    @Test
    void anAdministratorCanBeDemotedWhenAnotherRemains() throws SQLException {
        userAdminService.changeRole(adminA, adminB, Role.TECHNICIAN);

        assertEquals(Role.TECHNICIAN, roleOf(adminB));
    }

    // ------------------------------------------------ deactivating accounts

    @Test
    void aDeactivatedUserCannotLogInAndAReactivatedOneCan() throws SQLException {
        userAdminService.setActive(adminA, student, false);

        assertThrows(AuthenticationException.class,
                () -> authService.login(TestUsers.email("somestudent"), TestUsers.PASSWORD));

        userAdminService.setActive(adminA, student, true);

        assertEquals(student, authService.login(
                TestUsers.email("somestudent"), TestUsers.PASSWORD).id());
    }

    @Test
    void aLoggedInUserIsLoggedOutWhenDeactivated() throws SQLException {
        SessionUser session = new SessionUser(student, "ZZ somestudent", Role.STUDENT);

        userAdminService.setActive(adminA, student, false);

        assertEquals(java.util.Optional.empty(), authService.refresh(session));
    }

    @Test
    void youCannotDeactivateYourOwnAccount() {
        UserRuleException exception = assertThrows(UserRuleException.class,
                () -> userAdminService.setActive(adminA, adminA, false));

        assertEquals("You cannot deactivate your own account.",
                exception.getMessage());
    }

    @Test
    void theLastActiveAdministratorCannotBeDeactivated() throws SQLException {
        setDemoAdminActive(false);
        TestUsers.setActive(adminA, false);

        assertThrows(UserRuleException.class,
                () -> userAdminService.setActive(student, adminB, false));
        assertEquals(1, activeAdministrators());
    }

    // --------------------------------------------------------- passwords

    @Test
    void resettingAPasswordReplacesTheOldOne() throws SQLException {
        userAdminService.resetPassword(student, "Brand#New55");

        assertThrows(AuthenticationException.class,
                () -> authService.login(TestUsers.email("somestudent"), TestUsers.PASSWORD));

        assertEquals(student, authService.login(
                TestUsers.email("somestudent"), "Brand#New55").id());
    }

    @Test
    void aWeakNewPasswordIsRefusedAndTheOldOneStillWorks() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> userAdminService.resetPassword(student, "weak"));

        assertEquals(student, authService.login(
                TestUsers.email("somestudent"), TestUsers.PASSWORD).id());
    }

    @Test
    void theListNeverContainsPasswordHashes() throws SQLException {
        for (UserAccount account : userAdminService.listUsers()) {
            assertFalse(account.toString().contains("pbkdf2"));
        }
    }

    // ------------------------------------------------------- concurrency

    @Test
    void twoAdministratorsDemotingEachOtherNeverLeaveNoAdministrator()
            throws Exception {

        setDemoAdminActive(false);

        for (int round = 0; round < 8; round++) {
            TestUsers.setRole(adminA, "ADMIN");
            TestUsers.setRole(adminB, "ADMIN");

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger succeeded = new AtomicInteger();
            AtomicInteger refused = new AtomicInteger();

            Future<?> first = executor.submit(() -> demote(
                    adminA, adminB, ready, go, succeeded, refused));
            Future<?> second = executor.submit(() -> demote(
                    adminB, adminA, ready, go, succeeded, refused));

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            go.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertEquals(1, succeeded.get(), "round " + (round + 1));
            assertEquals(1, refused.get(), "round " + (round + 1));
            assertEquals(1, activeAdministrators(), "round " + (round + 1));
        }
    }

    private void demote(int actor, int target, CountDownLatch ready,
                        CountDownLatch go, AtomicInteger succeeded,
                        AtomicInteger refused) {
        ready.countDown();
        try {
            go.await();
            userAdminService.changeRole(actor, target, Role.STUDENT);
            succeeded.incrementAndGet();
        } catch (UserRuleException e) {
            refused.incrementAndGet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------- helpers

    private String message(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(IllegalArgumentException.class, action).getMessage();
    }

    private Role roleOf(int id) throws SQLException {
        return userAdminService.listUsers().stream()
                .filter(u -> u.id() == id).findFirst().orElseThrow().role();
    }

    private int activeAdministrators() throws SQLException {
        return (int) userAdminService.listUsers().stream()
                .filter(u -> u.role() == Role.ADMIN && u.active()).count();
    }
}
