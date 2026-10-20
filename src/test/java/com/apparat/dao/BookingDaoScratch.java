package com.apparat.dao;

import com.apparat.config.DatabaseConnection;
import com.apparat.model.Booking;
import com.apparat.model.BookingStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class BookingDaoScratch {
    public static void main(String[] args) throws SQLException {
        BookingDao bookingDao = new BookingDao();

        Booking booking = new Booking(
                0,
                1,  // Replace with an existing user ID
                1,  // Replace with an existing resource ID
                LocalDateTime.of(2026, 10, 21, 10, 0),
                LocalDateTime.of(2026, 10, 21, 11, 0),
                BookingStatus.CONFIRMED
        );

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Booking savedBooking = bookingDao.insert(connection, booking);

                System.out.println("Booking inserted successfully!");
                System.out.println("Generated booking ID: "
                        + savedBooking.getId());

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}