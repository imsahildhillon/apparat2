package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.model.Resource;
import com.apparat.model.ResourceStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingInstrumentMessageDbTests {

    private static final String NAME = "ZZ-TEST-Messages";

    private final BookingService bookingService = new BookingService();
    private final ResourceAdminService adminService =
            new ResourceAdminService();

    private final LocalDateTime start = LocalDateTime.of(2030, 3, 12, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2030, 3, 12, 11, 0);

    @BeforeEach
    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM resources WHERE name = ?")) {

            statement.setString(1, NAME);
            statement.executeUpdate();
        }
    }

    @Test
    void unknownInstrumentIsExplained() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, 999_999, start, end));

        assertEquals("That instrument does not exist.",
                exception.getMessage());
    }

    @Test
    void inactiveInstrumentIsExplained() throws SQLException {
        Resource resource = adminService.addResource(NAME, "Lab 1");
        adminService.setStatus(resource.getId(), ResourceStatus.INACTIVE);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(
                        1, resource.getId(), start, end));

        assertEquals("This instrument is not available for booking right now.",
                exception.getMessage());
    }
}
