package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dto.UtilizationRow;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportServiceDbTests {

    private static final String NAME = "ZZ-TEST-Report";
    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);

    private final ReportService reportService = new ReportService();
    private final ResourceAdminService adminService =
            new ResourceAdminService();
    private final BookingService bookingService = new BookingService();

    private Resource resource;

    @BeforeEach
    void createInstrumentWithOneBooking() throws SQLException {
        cleanUp();

        resource = adminService.addResource(NAME, "Lab 1");

        // 10:00 to 11:00 is two slots, which is one booked hour.
        bookingService.createBooking(
                1,
                resource.getId(),
                LocalDateTime.of(DAY, java.time.LocalTime.of(10, 0)),
                LocalDateTime.of(DAY, java.time.LocalTime.of(11, 0))
        );
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
            run(connection, "DELETE FROM resources WHERE name = ?");
        }
    }

    @Test
    void oneBookedHourInOneDayIsOneNinth() throws SQLException {
        UtilizationRow row = rowFor(reportService.utilization(DAY, DAY));

        assertEquals(1, row.bookings());
        assertEquals(1.0, row.bookedHours());
        // 1 booked hour out of 9 open hours.
        assertEquals(11.1, row.utilizationPercent());
    }

    @Test
    void longerRangeLowersThePercentage() throws SQLException {
        UtilizationRow row = rowFor(
                reportService.utilization(DAY.minusDays(1), DAY));

        assertEquals(1.0, row.bookedHours());
        // 1 booked hour out of 18 open hours.
        assertEquals(5.6, row.utilizationPercent());
    }

    @Test
    void rangeWithoutBookingsShowsZeros() throws SQLException {
        UtilizationRow row = rowFor(reportService.utilization(
                DAY.plusDays(10), DAY.plusDays(10)));

        assertEquals(0, row.bookings());
        assertEquals(0.0, row.bookedHours());
        assertEquals(0.0, row.utilizationPercent());
    }

    @Test
    void invalidRangesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.utilization(DAY, DAY.minusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> reportService.utilization(null, DAY));
        assertThrows(IllegalArgumentException.class,
                () -> reportService.utilization(DAY, DAY.plusDays(400)));
    }

    private UtilizationRow rowFor(List<UtilizationRow> rows) {
        return rows.stream()
                .filter(row -> row.resourceName().equals(NAME))
                .findFirst()
                .orElseThrow();
    }

    private void run(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, NAME);
            statement.executeUpdate();
        }
    }
}
