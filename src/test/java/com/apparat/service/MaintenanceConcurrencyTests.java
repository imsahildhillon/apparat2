package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.MaintenanceDao;
import com.apparat.model.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One thread adds a maintenance window while many threads try to book
 * overlapping times for the same instrument. Whatever the timing, a
 * booking and a window must never both exist for the same time.
 */
class MaintenanceConcurrencyTests {

    private static final String NAME = "ZZ-TEST-MaintRace";
    private static final int ROUNDS = 8;
    private static final int BOOKING_THREADS = 29;
    private static final int USER_ID = 1;

    private final MaintenanceService maintenanceService =
            new MaintenanceService();
    private final BookingService bookingService = new BookingService();
    private final MaintenanceDao maintenanceDao = new MaintenanceDao();
    private final ResourceAdminService adminService =
            new ResourceAdminService();

    private int resourceId;

    @BeforeEach
    void createInstrument() throws SQLException {
        cleanUp();
        Resource resource = adminService.addResource(NAME, "Lab 2");
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
    void aWindowAndABookingNeverCoverTheSameTime() throws Exception {

        int windowWon = 0;

        for (int round = 0; round < ROUNDS; round++) {
            LocalDate day = LocalDate.of(2030, 4, 1).plusDays(round);

            Map<String, Integer> failures = new ConcurrentHashMap<>();
            AtomicBoolean windowAdded = new AtomicBoolean(false);
            AtomicInteger bookingsSaved = new AtomicInteger();

            ExecutorService executor =
                    Executors.newFixedThreadPool(BOOKING_THREADS + 1);
            CountDownLatch ready = new CountDownLatch(BOOKING_THREADS + 1);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            // Window thread: 10:00 to 11:00.
            futures.add(executor.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    maintenanceService.addWindow(
                            resourceId, day.atTime(10, 0), day.atTime(11, 0),
                            "Race round", USER_ID);
                    windowAdded.set(true);
                } catch (Exception e) {
                    failures.merge("window:" + e.getClass().getSimpleName(),
                            1, Integer::sum);
                }
            }));

            // Booking threads: three different overlapping windows.
            for (int i = 0; i < BOOKING_THREADS; i++) {
                final int index = i;

                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();

                        LocalDateTime start;
                        LocalDateTime end;

                        switch (index % 3) {
                            case 0 -> {
                                start = day.atTime(10, 0);
                                end = day.atTime(11, 0);
                            }
                            case 1 -> {
                                start = day.atTime(10, 30);
                                end = day.atTime(11, 30);
                            }
                            default -> {
                                start = day.atTime(9, 30);
                                end = day.atTime(10, 30);
                            }
                        }

                        bookingService.createBooking(
                                USER_ID, resourceId, start, end);
                        bookingsSaved.incrementAndGet();

                    } catch (Exception e) {
                        failures.merge(e.getClass().getSimpleName(),
                                1, Integer::sum);
                    }
                }));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS));
            go.countDown();

            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }

            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

            // The invariant, read from the database after the round.
            int overlappingBookings;
            boolean windowExists;

            try (Connection connection = DatabaseConnection.getConnection()) {
                overlappingBookings =
                        maintenanceDao.countConfirmedBookingsOverlapping(
                                connection, resourceId,
                                day.atTime(10, 0), day.atTime(11, 0));

                windowExists = maintenanceDao.existsOverlapping(
                        connection, resourceId,
                        day.atTime(10, 0), day.atTime(11, 0));
            }

            System.out.println("Round " + (round + 1)
                    + ": windowAdded=" + windowAdded.get()
                    + ", bookingsSaved=" + bookingsSaved.get()
                    + ", failures=" + failures);

            assertEquals(windowAdded.get(), windowExists,
                    "The window exists exactly when it reported success.");

            assertTrue(!(windowExists && overlappingBookings > 0),
                    "A window and an overlapping booking must never coexist.");

            // Only clean, expected failure types are allowed. No deadlocks.
            for (String type : failures.keySet()) {
                assertTrue(
                        type.equals("MaintenanceConflictException")
                                || type.equals("SlotConflictException")
                                || type.equals("window:MaintenanceConflictException"),
                        "Unexpected failure type: " + type);
            }

            if (windowAdded.get()) {
                windowWon++;
            }

            clearRound();
        }

        System.out.println("Window won " + windowWon + " of " + ROUNDS
                + " rounds; bookings won the rest.");
    }

    private void clearRound() throws SQLException {
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
        }
    }

    private void run(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, NAME);
            statement.executeUpdate();
        }
    }
}
