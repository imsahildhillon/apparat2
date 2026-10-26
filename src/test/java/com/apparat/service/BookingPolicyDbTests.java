package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.model.Booking;
import com.apparat.model.Resource;
import com.apparat.support.TestUsers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The role limits, tested through createBooking against the real database.
 */
class BookingPolicyDbTests {

    private static final LocalDate FUTURE = LocalDate.of(2030, 5, 6);

    private final BookingService bookingService = new BookingService();
    private final ResourceAdminService adminService =
            new ResourceAdminService();

    private int instrumentId;
    private int studentId;
    private int facultyId;
    private int technicianId;
    private int adminId;

    @BeforeEach
    void createUsersAndAnInstrument() throws SQLException {
        cleanUp();

        studentId = TestUsers.create("limitstudent", "STUDENT", true);
        facultyId = TestUsers.create("limitfaculty", "FACULTY", true);
        technicianId = TestUsers.create("limittech", "TECHNICIAN", true);
        adminId = TestUsers.create("limitadmin", "ADMIN", true);

        Resource instrument = adminService.addResource("ZZ-TEST-Limits", "Lab 3");
        instrumentId = instrument.getId();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        TestUsers.cleanUp();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM resources WHERE name = 'ZZ-TEST-Limits'")) {
            statement.executeUpdate();
        }
    }

    private LocalDateTime at(int dayOffset, int hour, int minute) {
        return FUTURE.plusDays(dayOffset).atTime(hour, minute);
    }

    private Booking book(int userId, int dayOffset, int startHour, int endHour)
            throws SQLException {
        return bookingService.createBooking(
                userId, instrumentId,
                at(dayOffset, startHour, 0), at(dayOffset, endHour, 0));
    }

    @Test
    void aStudentMayBookTwoHoursButNotMore() throws SQLException {
        assertTrue(book(studentId, 0, 9, 11).getId() > 0);

        BookingLimitException exception = assertThrows(
                BookingLimitException.class,
                () -> book(studentId, 1, 9, 12));

        assertEquals("Students can book at most 2 hours at a time. "
                + "Please shorten your booking.", exception.getMessage());
    }

    @Test
    void aRefusedBookingLeavesNothingBehind() throws SQLException {
        assertThrows(BookingLimitException.class,
                () -> book(studentId, 0, 9, 13));

        assertEquals(0, count("SELECT COUNT(*) FROM bookings WHERE user_id = ?",
                studentId));
        assertEquals(0, count("SELECT COUNT(*) FROM booking_slots "
                + "WHERE resource_id = ?", instrumentId));
    }

    @Test
    void facultyMayBookLongerThanAStudent() throws SQLException {
        assertTrue(book(facultyId, 0, 9, 13).getId() > 0);

        assertThrows(BookingLimitException.class,
                () -> book(facultyId, 1, 9, 14));
    }

    @Test
    void aTechnicianMaySixHours() throws SQLException {
        assertTrue(book(technicianId, 0, 9, 15).getId() > 0);

        assertThrows(BookingLimitException.class,
                () -> book(technicianId, 1, 9, 16));
    }

    @Test
    void anAdministratorMayBookTheWholeDay() throws SQLException {
        assertTrue(book(adminId, 0, 9, 18).getId() > 0);
    }

    @Test
    void aStudentCannotHoldAFourthActiveBooking() throws SQLException {
        book(studentId, 0, 9, 10);
        book(studentId, 1, 9, 10);
        book(studentId, 2, 9, 10);

        BookingLimitException exception = assertThrows(
                BookingLimitException.class,
                () -> book(studentId, 3, 9, 10));

        assertTrue(exception.getMessage().startsWith(
                "You already have 3 active bookings"));
    }

    @Test
    void cancellingABookingFreesAPlace() throws SQLException {
        Booking first = book(studentId, 0, 9, 10);
        book(studentId, 1, 9, 10);
        book(studentId, 2, 9, 10);

        bookingService.cancelBooking(first.getId(), studentId);

        assertTrue(book(studentId, 3, 9, 10).getId() > 0);
    }

    @Test
    void finishedBookingsDoNotCount() throws SQLException {
        // Three bookings in the past, then three in the future.
        LocalDate past = LocalDate.of(2020, 1, 6);

        for (int day = 0; day < 3; day++) {
            bookingService.createBooking(studentId, instrumentId,
                    past.plusDays(day).atTime(9, 0),
                    past.plusDays(day).atTime(10, 0));
        }

        book(studentId, 0, 9, 10);
        book(studentId, 1, 9, 10);

        assertTrue(book(studentId, 2, 9, 10).getId() > 0);
    }

    @Test
    void facultyMayHoldSixButNotSeven() throws SQLException {
        for (int day = 0; day < 6; day++) {
            book(facultyId, day, 9, 10);
        }

        assertThrows(BookingLimitException.class,
                () -> book(facultyId, 6, 9, 10));
    }

    @Test
    void oneUsersLimitDoesNotAffectAnother() throws SQLException {
        book(studentId, 0, 9, 10);
        book(studentId, 1, 9, 10);
        book(studentId, 2, 9, 10);

        assertTrue(book(facultyId, 3, 9, 10).getId() > 0);
    }

    @Test
    void anUnknownUserIsRefused() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> book(999_999, 0, 9, 10));

        assertEquals("That user does not exist.", exception.getMessage());
    }

    private int count(String sql, int value) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, value);

            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
