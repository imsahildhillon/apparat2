package com.apparat.policy;

import com.apparat.model.Role;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingPolicyTests {

    private LocalDateTime start() {
        return LocalDateTime.of(2030, 3, 12, 9, 0);
    }

    private LocalDateTime endAfter(int minutes) {
        return start().plusMinutes(minutes);
    }

    @Test
    void eachRoleGetsItsOwnPolicyClass() {
        assertInstanceOf(StudentPolicy.class, BookingPolicy.forRole(Role.STUDENT));
        assertInstanceOf(FacultyPolicy.class, BookingPolicy.forRole(Role.FACULTY));
        assertInstanceOf(TechnicianPolicy.class, BookingPolicy.forRole(Role.TECHNICIAN));
        assertInstanceOf(AdminPolicy.class, BookingPolicy.forRole(Role.ADMIN));
    }

    @Test
    void theSameCallGivesADifferentAnswerPerRole() {
        // One method, four behaviours: polymorphism.
        assertEquals(120, BookingPolicy.forRole(Role.STUDENT).maxMinutesPerBooking());
        assertEquals(240, BookingPolicy.forRole(Role.FACULTY).maxMinutesPerBooking());
        assertEquals(360, BookingPolicy.forRole(Role.TECHNICIAN).maxMinutesPerBooking());
        assertEquals(Integer.MAX_VALUE,
                BookingPolicy.forRole(Role.ADMIN).maxMinutesPerBooking());

        assertEquals(3, BookingPolicy.forRole(Role.STUDENT).maxActiveBookings());
        assertEquals(6, BookingPolicy.forRole(Role.FACULTY).maxActiveBookings());
        assertEquals(10, BookingPolicy.forRole(Role.TECHNICIAN).maxActiveBookings());
    }

    @Test
    void limitsGrowFromStudentToTechnician() {
        for (Role[] pair : new Role[][]{
                {Role.STUDENT, Role.FACULTY}, {Role.FACULTY, Role.TECHNICIAN}}) {

            BookingPolicy smaller = BookingPolicy.forRole(pair[0]);
            BookingPolicy larger = BookingPolicy.forRole(pair[1]);

            assertTrue(smaller.maxMinutesPerBooking() < larger.maxMinutesPerBooking());
            assertTrue(smaller.maxActiveBookings() < larger.maxActiveBookings());
        }
    }

    @Test
    void aBookingExactlyAtTheLimitIsAllowed() {
        BookingPolicy student = new StudentPolicy();

        assertEquals(Optional.empty(), student.check(start(), endAfter(120), 0));
        assertEquals(Optional.empty(), student.check(start(), endAfter(30), 2));
    }

    @Test
    void aTooLongBookingIsExplained() {
        BookingPolicy student = new StudentPolicy();

        assertEquals(
                "Students can book at most 2 hours at a time. "
                        + "Please shorten your booking.",
                student.check(start(), endAfter(150), 0).orElseThrow());

        assertEquals(
                "Faculty members can book at most 4 hours at a time. "
                        + "Please shorten your booking.",
                new FacultyPolicy().check(start(), endAfter(300), 0).orElseThrow());
    }

    @Test
    void tooManyActiveBookingsIsExplained() {
        assertEquals(
                "You already have 3 active bookings, which is the most "
                        + "students can hold at once (3). Cancel one, "
                        + "or wait until one finishes.",
                new StudentPolicy().check(start(), endAfter(30), 3).orElseThrow());
    }

    @Test
    void theLengthIsCheckedBeforeTheCount() {
        // Both limits broken: the user hears about the booking they are making.
        String message = new StudentPolicy()
                .check(start(), endAfter(180), 5).orElseThrow();

        assertTrue(message.contains("at most 2 hours"));
    }

    @Test
    void administratorsHaveNoLimits() {
        BookingPolicy admin = new AdminPolicy();

        assertTrue(admin.isUnlimited());
        assertEquals(Optional.empty(), admin.check(start(), endAfter(540), 1000));
        assertEquals("As an administrator you have no booking limits.",
                admin.describe());
    }

    @Test
    void otherRolesAreNotUnlimited() {
        assertFalse(new StudentPolicy().isUnlimited());
        assertFalse(new FacultyPolicy().isUnlimited());
        assertFalse(new TechnicianPolicy().isUnlimited());
    }

    @Test
    void theDescriptionStatesTheLimitsInWords() {
        assertEquals(
                "As a student you can book up to 2 hours at a time "
                        + "and hold up to 3 active bookings.",
                new StudentPolicy().describe());

        assertEquals(
                "As faculty you can book up to 4 hours at a time "
                        + "and hold up to 6 active bookings.",
                new FacultyPolicy().describe());
    }

    @Test
    void minutesAreWrittenInPlainWords() {
        assertEquals("30 minutes", BookingPolicy.describeMinutes(30));
        assertEquals("1 hour", BookingPolicy.describeMinutes(60));
        assertEquals("1 hour 30 minutes", BookingPolicy.describeMinutes(90));
        assertEquals("2 hours", BookingPolicy.describeMinutes(120));
    }
}
