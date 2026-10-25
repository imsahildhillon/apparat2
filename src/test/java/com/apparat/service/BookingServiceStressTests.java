
package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.BookingSlotDao;
import com.apparat.model.Booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * PLAN
 *
 * Windows and occupied slot starts:
 * A: 10:00-11:00 -> 10:00, 10:30
 * B: 10:30-11:30 -> 10:30, 11:00
 * C: 11:00-12:00 -> 11:00, 11:30
 * D: 10:00-12:00 -> 10:00, 10:30, 11:00, 11:30
 * E: 11:30-12:30 -> 11:30, 12:00
 * F: 10:30-12:00 -> 10:30, 11:00, 11:30
 *
 * Overlap means sharing at least one slot start:
 * A conflicts with B, D, F.
 * B conflicts with A, D, F.
 * C conflicts with D, E, F.
 * D conflicts with A, B, C, E, F.
 * E conflicts with C, D, F.
 * F conflicts with A, B, C, D, E.
 * A and C can coexist; A and E can coexist; B and E can coexist.
 *
 * Prediction:
 * At most two distinct windows can succeed.
 * Expected failures are mainly SlotConflictException.
 * Deadlocks (MySQL error 1213) should be zero with FOR UPDATE.
 *
 * Invariants:
 * 1. Every successful booking has exactly duration / 30 slots.
 * 2. No booking on the test date has zero slots.
 * 3. No slot points to a missing booking.
 * 4. New slot rows equal the expected slots for successful bookings.
 * 5. No resource-slot pair appears more than once.
 */

class BookingServiceStressTests {

    private static final int THREAD_COUNT = 50;
    private static final int SLOT_MINUTES = 30;

    private static final LocalDate TEST_DATE =
            LocalDate.of(2026, 10, 28);

    private final BookingService bookingService =
            new BookingService();

    private final int userId = 1;
    private final int resourceId = 1;

    private final Object cleanupLock = new Object();

    private final List<BookingResult> createdBookings =
            new ArrayList<>();

    private final List<Integer> createdBookingIds =
            new ArrayList<>();

    private record Window(
            String name,
            LocalDateTime start,
            LocalDateTime end
    ) {}

    private record BookingResult(
            int bookingId,
            LocalDateTime start,
            LocalDateTime end
    ) {}

    private List<Window> windows() {
        return List.of(
                new Window("A", time(10, 0), time(11, 0)),
                new Window("B", time(10, 30), time(11, 30)),
                new Window("C", time(11, 0), time(12, 0)),
                new Window("D", time(10, 0), time(12, 0)),
                new Window("E", time(11, 30), time(12, 30)),
                new Window("F", time(10, 30), time(12, 0))
        );
    }

    private LocalDateTime time(int hour, int minute) {
        return TEST_DATE.atTime(hour, minute);
    }

    @Test
    void overlappingWindowsPreserveDatabaseInvariants()
            throws Exception {

        List<Window> windows = windows();

        int bookingsBefore = countBookingsOnTestDate();
        int slotsBefore = countSlotsOnTestDate();

        ExecutorService executor =
                Executors.newFixedThreadPool(THREAD_COUNT);

        CountDownLatch readyGate =
                new CountDownLatch(THREAD_COUNT);

        CountDownLatch startGate =
                new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger deadlocks = new AtomicInteger();

        Map<String, Integer> failures =
                new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int taskIndex = i;

                futures.add(executor.submit(() -> {
                    readyGate.countDown();

                    try {
                        if (!startGate.await(15, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Timed out waiting for start gate."
                            );
                        }

                        Window window =
                                windows.get(taskIndex % windows.size());

                        Booking booking =
                                bookingService.createBooking(
                                        userId,
                                        resourceId,
                                        window.start(),
                                        window.end()
                                );

                        BookingResult result = new BookingResult(
                                booking.getId(),
                                window.start(),
                                window.end()
                        );

                        synchronized (cleanupLock) {
                            createdBookings.add(result);
                            createdBookingIds.add(booking.getId());
                        }

                        successes.incrementAndGet();

                    } catch (Exception e) {
                        if (isDeadlock(e)) {
                            deadlocks.incrementAndGet();
                        }

                        failures.merge(
                                e.getClass().getSimpleName(),
                                1,
                                Integer::sum
                        );

                        System.err.println(
                                "Worker " + taskIndex
                                        + " failed: "
                                        + e.getClass().getName()
                                        + ": " + e.getMessage()
                        );
                    }
                }));
            }

            assertTrue(
                    readyGate.await(15, TimeUnit.SECONDS),
                    "Not all workers became ready."
            );

            startGate.countDown();

            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }

        } finally {
            startGate.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(15, TimeUnit.SECONDS),
                    "Worker threads did not terminate."
            );
        }

        int successCount = successes.get();
        int deadlockCount = deadlocks.get();

        int failureCount = failures.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        System.out.println("Total attempts: " + THREAD_COUNT);
        System.out.println("Successful bookings: " + successCount);
        System.out.println("Failure types: " + failures);
        System.out.println("Deadlocks (error 1213): " + deadlockCount);
        System.out.println("Successful windows: " + createdBookings);

        // At most two distinct windows can coexist.
        assertTrue(
                successCount <= 2,
                "More than two compatible windows were accepted."
        );

        // At least one booking must succeed.
        assertTrue(
                successCount >= 1,
                "At least one booking should succeed."
        );

        // All workers must succeed or record a failure.
        assertEquals(
                THREAD_COUNT,
                successCount + failureCount,
                "Every worker must succeed or record a failure."
        );

        // FOR UPDATE should prevent deadlocks in this test workload.
        assertEquals(
                0,
                deadlockCount,
                "No MySQL deadlocks (error 1213) should occur."
        );

        // Invariant 1: each successful booking has the expected slots.
        int expectedNewSlots = 0;

        for (BookingResult booking : List.copyOf(createdBookings)) {
            long durationMinutes = Duration.between(
                    booking.start(),
                    booking.end()
            ).toMinutes();

            int expectedSlots =
                    Math.toIntExact(durationMinutes / SLOT_MINUTES);

            expectedNewSlots += expectedSlots;

            assertEquals(
                    expectedSlots,
                    countSlotsForBooking(booking.bookingId()),
                    "Incorrect slot count for booking "
                            + booking.bookingId()
            );
        }

        // Invariant 2: no booking on the test date is left without slots.
        assertEquals(
                0,
                countBookingsWithoutSlotsOnTestDate(),
                "A booking on the test date has no slots."
        );

        // Invariant 3: no test-date slot references a missing booking.
        assertEquals(
                0,
                countSlotsWithMissingBookingOnTestDate(),
                "A slot points to a nonexistent booking."
        );

        // Invariant 4: slot rows added equal the expected sum.
        assertEquals(
                slotsBefore + expectedNewSlots,
                countSlotsOnTestDate(),
                "Unexpected number of slot rows after the run."
        );

        // Invariant 5: no resource-slot pair appears more than once.
        assertEquals(
                0,
                countDuplicateResourceSlots(),
                "A resource has duplicate slot assignments."
        );

        assertEquals(
                bookingsBefore + successCount,
                countBookingsOnTestDate(),
                "Unexpected booking row count after the run."
        );
    }

    /*
     * Detect MySQL deadlock error 1213, including when the SQLException
     * is wrapped inside another exception.
     */
    private boolean isDeadlock(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 1213) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private int countBookingsOnTestDate() throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM bookings
                WHERE start_at >= ? AND start_at < ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(TEST_DATE.atStartOfDay())
            );
            statement.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(
                            TEST_DATE.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countSlotsOnTestDate() throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM booking_slots
                WHERE slot_start >= ? AND slot_start < ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(TEST_DATE.atStartOfDay())
            );
            statement.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(
                            TEST_DATE.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countSlotsForBooking(int bookingId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM booking_slots
                WHERE booking_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, bookingId);

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countBookingsWithoutSlotsOnTestDate()
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM bookings b
                WHERE b.start_at >= ? AND b.start_at < ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM booking_slots s
                      WHERE s.booking_id = b.id
                  )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(TEST_DATE.atStartOfDay())
            );
            statement.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(
                            TEST_DATE.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countSlotsWithMissingBookingOnTestDate()
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM booking_slots s
                LEFT JOIN bookings b ON b.id = s.booking_id
                WHERE s.slot_start >= ? AND s.slot_start < ?
                  AND b.id IS NULL
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setTimestamp(
                    1,
                    java.sql.Timestamp.valueOf(TEST_DATE.atStartOfDay())
            );
            statement.setTimestamp(
                    2,
                    java.sql.Timestamp.valueOf(
                            TEST_DATE.plusDays(1).atStartOfDay())
            );

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countDuplicateResourceSlots() throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT resource_id, slot_start
                    FROM booking_slots
                    GROUP BY resource_id, slot_start
                    HAVING COUNT(*) > 1
                ) duplicates_found
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            rs.next();
            return rs.getInt(1);
        }
    }

    @AfterEach
    void cleanUp() throws SQLException {
        synchronized (cleanupLock) {
            try (Connection connection =
                         DatabaseConnection.getConnection()) {

                connection.setAutoCommit(false);

                try {
                    BookingSlotDao slotDao = new BookingSlotDao();

                    for (int bookingId : createdBookingIds) {
                        slotDao.deleteByBookingId(connection, bookingId);

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             "DELETE FROM bookings WHERE id = ?")) {
                            statement.setInt(1, bookingId);
                            statement.executeUpdate();
                        }
                    }

                    connection.commit();

                    createdBookingIds.clear();
                    createdBookings.clear();

                } catch (SQLException | RuntimeException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackException) {
                        e.addSuppressed(rollbackException);
                    }

                    throw e;
                }
            }
        }
    }
}
