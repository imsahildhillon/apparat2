package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.MaintenanceView;
import com.apparat.dto.SlotView;
import com.apparat.model.Booking;
import com.apparat.model.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceServiceDbTests {

    private static final String NAME = "ZZ-TEST-Maintenance";
    private static final LocalDate DAY = LocalDate.of(2030, 3, 12);
    private static final int USER_ID = 1;

    private final MaintenanceService maintenanceService =
            new MaintenanceService();
    private final BookingService bookingService = new BookingService();
    private final ResourceAdminService adminService =
            new ResourceAdminService();
    private final AvailabilityService availabilityService =
            new AvailabilityService();

    private int resourceId;

    @BeforeEach
    void createInstrument() throws SQLException {
        cleanUp();
        Resource resource = adminService.addResource(NAME, "Lab 1");
        resourceId = resource.getId();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            run(connection, """
                    DELETE FROM booking_slots WHERE resource_id IN
                    (SELECT id FROM resources WHERE name = ?)
                    """);
            run(connection, """
                    DELETE FROM bookings WHERE resource_id IN
                    (SELECT id FROM resources WHERE name = ?)
                    """);
            run(connection, """
                    DELETE FROM maintenance_windows WHERE resource_id IN
                    (SELECT id FROM resources WHERE name = ?)
                    """);
            run(connection, "DELETE FROM resources WHERE name = ?");
        }
    }

    @Test
    void addedWindowIsSavedAndListed() throws SQLException {
        maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "  Lamp change  ", USER_ID);

        List<MaintenanceView> upcoming = maintenanceService.listUpcoming();

        MaintenanceView view = upcoming.stream()
                .filter(w -> w.resourceName().equals(NAME))
                .findFirst()
                .orElseThrow();

        assertEquals("Lamp change", view.reason());
        assertEquals(at(10, 0), view.startTime());
        assertEquals(at(11, 0), view.endTime());
    }

    @Test
    void bookingInsideAWindowIsRejectedAndNothingIsSaved()
            throws SQLException {

        maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "Calibration", USER_ID);

        assertThrows(MaintenanceConflictException.class,
                () -> bookingService.createBooking(
                        USER_ID, resourceId, at(10, 30), at(11, 30)));

        assertEquals(0, countBookings());
        assertEquals(0, countSlots());
    }

    @Test
    void bookingOverlappingTheWholeWindowIsRejected() throws SQLException {
        maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "Calibration", USER_ID);

        assertThrows(MaintenanceConflictException.class,
                () -> bookingService.createBooking(
                        USER_ID, resourceId, at(9, 30), at(11, 30)));
    }

    @Test
    void bookingsTouchingTheWindowAreAllowed() throws SQLException {
        maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "Calibration", USER_ID);

        // Ends exactly when the window starts, and starts exactly when it ends.
        Booking before = bookingService.createBooking(
                USER_ID, resourceId, at(9, 0), at(10, 0));
        Booking after = bookingService.createBooking(
                USER_ID, resourceId, at(11, 0), at(12, 0));

        assertTrue(before.getId() > 0);
        assertTrue(after.getId() > 0);
    }

    @Test
    void windowOverAConfirmedBookingIsRejected() throws SQLException {
        bookingService.createBooking(
                USER_ID, resourceId, at(10, 0), at(11, 0));

        MaintenanceConflictException exception = assertThrows(
                MaintenanceConflictException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 30), at(11, 30), "Cleaning",
                        USER_ID));

        assertTrue(exception.getMessage().startsWith("1 confirmed"));
    }

    @Test
    void cancelledBookingsDoNotBlockAWindow() throws SQLException {
        Booking booking = bookingService.createBooking(
                USER_ID, resourceId, at(10, 0), at(11, 0));

        bookingService.cancelBooking(booking.getId(), USER_ID);

        int id = maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "Cleaning", USER_ID);

        assertTrue(id > 0);
    }

    @Test
    void overlappingWindowsAreRejectedButAdjacentOnesAreAllowed()
            throws SQLException {

        maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "First", USER_ID);

        assertThrows(MaintenanceConflictException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 30), at(11, 30), "Overlap",
                        USER_ID));

        int adjacent = maintenanceService.addWindow(
                resourceId, at(11, 0), at(12, 0), "Next", USER_ID);

        assertTrue(adjacent > 0);
    }

    @Test
    void invalidWindowsAreRejected() {
        // Off the half hour, outside opening hours, reversed, other day.
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 10), at(10, 40), "x", USER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(7, 0), at(8, 0), "x", USER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(11, 0), at(10, 0), "x", USER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 0),
                        at(10, 0).plusDays(1), "x", USER_ID));
    }

    @Test
    void reasonIsRequiredAndLimited() {
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 0), at(11, 0), "   ", USER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 0), at(11, 0), null, USER_ID));
        assertThrows(IllegalArgumentException.class,
                () -> maintenanceService.addWindow(
                        resourceId, at(10, 0), at(11, 0),
                        "x".repeat(201), USER_ID));
    }

    @Test
    void unknownInstrumentIsRejected() {
        assertThrows(ResourceNotFoundException.class,
                () -> maintenanceService.addWindow(
                        999_999, at(10, 0), at(11, 0), "x", USER_ID));
    }

    @Test
    void removingAWindowReopensTheTime() throws SQLException {
        int id = maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "Cleaning", USER_ID);

        assertThrows(MaintenanceConflictException.class,
                () -> bookingService.createBooking(
                        USER_ID, resourceId, at(10, 0), at(11, 0)));

        maintenanceService.deleteWindow(id);

        Booking booking = bookingService.createBooking(
                USER_ID, resourceId, at(10, 0), at(11, 0));

        assertTrue(booking.getId() > 0);
    }

    @Test
    void removingAnUnknownWindowFails() {
        assertThrows(MaintenanceNotFoundException.class,
                () -> maintenanceService.deleteWindow(999_999));
    }

    @Test
    void availabilityShowsMaintenanceBookedAndFreeSlots()
            throws SQLException {

        maintenanceService.addWindow(
                resourceId, at(10, 0), at(11, 0), "Cleaning", USER_ID);
        bookingService.createBooking(
                USER_ID, resourceId, at(14, 0), at(15, 0));

        List<SlotView> grid =
                availabilityService.getDayGrid(resourceId, DAY);

        assertEquals(18, grid.size());

        assertFalse(slot(grid, 9, 30).maintenance());
        assertTrue(slot(grid, 10, 0).maintenance());
        assertTrue(slot(grid, 10, 30).maintenance());
        assertFalse(slot(grid, 11, 0).maintenance());

        assertTrue(slot(grid, 14, 0).booked());
        assertFalse(slot(grid, 14, 0).maintenance());
        assertFalse(slot(grid, 12, 0).booked());
    }

    private SlotView slot(List<SlotView> grid, int hour, int minute) {
        return grid.stream()
                .filter(s -> s.start().equals(at(hour, minute)))
                .findFirst()
                .orElseThrow();
    }

    private LocalDateTime at(int hour, int minute) {
        return DAY.atTime(hour, minute);
    }

    private int countBookings() throws SQLException {
        return count("SELECT COUNT(*) FROM bookings WHERE resource_id = ?");
    }

    private int countSlots() throws SQLException {
        return count("SELECT COUNT(*) FROM booking_slots WHERE resource_id = ?");
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, resourceId);

            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void run(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, NAME);
            statement.executeUpdate();
        }
    }
}
