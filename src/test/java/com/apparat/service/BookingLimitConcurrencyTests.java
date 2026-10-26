package com.apparat.service;

import com.apparat.config.DatabaseConnection;
import com.apparat.dao.BookingCountDao;
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

/**
 * One student tries to book eight different instruments at the very same
 * moment. The limit is three active bookings, so exactly three may win.
 * Different instruments do not share a lock, so only the user lock inside
 * createBooking can make this come out right.
 */
class BookingLimitConcurrencyTests {

    private static final int INSTRUMENTS = 8;
    private static final int ROUNDS = 6;

    private final BookingService bookingService = new BookingService();
    private final ResourceAdminService adminService =
            new ResourceAdminService();
    private final BookingCountDao bookingCountDao = new BookingCountDao();

    private int studentId;
    private final List<Integer> instrumentIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        cleanUp();

        studentId = TestUsers.create("racestudent", "STUDENT", true);

        for (int i = 0; i < INSTRUMENTS; i++) {
            Resource instrument =
                    adminService.addResource("ZZ-TEST-Race" + i, "Lab 4");
            instrumentIds.add(instrument.getId());
        }
    }

    @AfterEach
    void cleanUp() throws SQLException {
        TestUsers.cleanUp();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM resources WHERE name LIKE 'ZZ-TEST-Race%'")) {
            statement.executeUpdate();
        }
    }

    @Test
    void exactlyThreeOfEightSimultaneousBookingsSucceed() throws Exception {

        for (int round = 0; round < ROUNDS; round++) {
            LocalDate day = LocalDate.of(2030, 6, 3).plusDays(round);

            AtomicInteger successes = new AtomicInteger();
            Map<String, Integer> failures = new ConcurrentHashMap<>();

            ExecutorService executor =
                    Executors.newFixedThreadPool(INSTRUMENTS);
            CountDownLatch ready = new CountDownLatch(INSTRUMENTS);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int instrumentId : instrumentIds) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                        bookingService.createBooking(
                                studentId, instrumentId,
                                day.atTime(9, 0), day.atTime(9, 30));
                        successes.incrementAndGet();
                    } catch (Exception e) {
                        failures.merge(e.getClass().getSimpleName(), 1,
                                Integer::sum);
                    }
                }));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS));
            go.countDown();

            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }

            executor.shutdownNow();

            int active;

            try (Connection connection = DatabaseConnection.getConnection()) {
                active = bookingCountDao.countActiveByUser(
                        connection, studentId, LocalDateTime.of(2030, 1, 1, 0, 0));
            }

            System.out.println("Round " + (round + 1) + ": successes="
                    + successes.get() + ", failures=" + failures
                    + ", active in database=" + active);

            assertEquals(3, successes.get(), "round " + (round + 1));
            assertEquals(3, active, "round " + (round + 1));
            assertEquals(Map.of("BookingLimitException", INSTRUMENTS - 3),
                    failures, "round " + (round + 1));

            // Start the next round with no bookings left.
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement slots = connection.prepareStatement(
                         "DELETE FROM booking_slots WHERE booking_id IN "
                                 + "(SELECT id FROM bookings WHERE user_id = ?)");
                 PreparedStatement bookings = connection.prepareStatement(
                         "DELETE FROM bookings WHERE user_id = ?")) {

                slots.setInt(1, studentId);
                slots.executeUpdate();
                bookings.setInt(1, studentId);
                bookings.executeUpdate();
            }
        }
    }
}
