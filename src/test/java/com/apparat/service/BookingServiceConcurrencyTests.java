package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.BookingSlotDao;
import com.apparat.model.Booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceConcurrencyTests {
    private static final int THREAD_COUNT = 50;
    private final BookingService bookingService = new BookingService();

    private final List<Integer> createdBookingIds =
            new ArrayList<>();

    private final Object cleanupLock = new Object();

    private final int userId = 1;
    private final int resourceId = 1;

    private final LocalDateTime start =
            LocalDateTime.of(2026, 10, 26, 10, 0);

    private final LocalDateTime end =
            LocalDateTime.of(2026, 10, 26, 11, 0);

    @Test
    void onlyOneConcurrentBookingShouldSucceed() throws Exception {
        int bookingsBefore = countRows("bookings");
        int slotsBefore = countRows("booking_slots");

        ExecutorService executor =
                Executors.newFixedThreadPool(THREAD_COUNT);

        CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger();

        Map<String, Integer> failures = new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    readyGate.countDown();

                    try {
                        if (!startGate.await(15, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Timed out waiting for start gate."
                            );
                        }

                        Booking booking = bookingService.createBooking(
                                userId, resourceId, start, end
                        );

                        synchronized (cleanupLock) {
                            createdBookingIds.add(booking.getId());
                        }

                        successes.incrementAndGet();

                    } catch (Exception e) {
                        failures.merge(
                                e.getClass().getSimpleName(),
                                1,
                                Integer::sum
                        );
                    }
                }));
            }

            assertTrue(
                    readyGate.await(15, TimeUnit.SECONDS),
                    "Not all worker threads became ready."
            );

            // Release all workers as close to simultaneously as possible.
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

        System.out.println("Successful bookings: " + successes.get());
        System.out.println("Failure types: " + failures);

        assertEquals(1, successes.get(),
                "Exactly one concurrent booking should succeed.");

        assertEquals(THREAD_COUNT - 1, failures.values().stream()
                        .mapToInt(Integer::intValue)
                        .sum(),
                "All remaining attempts should fail.");

        assertEquals(
                Map.of("SlotConflictException", THREAD_COUNT - 1),
                failures,
                "Every losing request should fail with SlotConflictException."
        );

        assertEquals(bookingsBefore + 1, countRows("bookings"));
        assertEquals(slotsBefore + 2, countRows("booking_slots"));
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

    private int countRows(String tableName) throws SQLException {
        // Only fixed internal table names should be passed here.
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}