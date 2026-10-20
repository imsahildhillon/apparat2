package com.apparat.dao;

import com.apparat.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class BookingSlotDaoScratch {

    public static void main(String[] args) throws SQLException {
        BookingSlotDao bookingSlotDao = new BookingSlotDao();

        int resourceId = 1; // Replace with an existing resource ID
        int bookingId = 1;  // Replace with an existing booking ID

        LocalDateTime slotStart =
                LocalDateTime.of(2026, 10, 21, 10, 0);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                System.out.println("Inserting booking slot...");

                bookingSlotDao.insert(
                        connection,
                        resourceId,
                        slotStart,
                        bookingId
                );

                System.out.println("Booking slot inserted successfully.");

                System.out.println("\nDeleting slots for booking ID "
                        + bookingId + "...");

                int deletedRows = bookingSlotDao.deleteByBookingId(
                        connection,
                        bookingId
                );

                System.out.println("Slots deleted: " + deletedRows);

                // Roll back so this scratch test leaves the database unchanged.
                connection.rollback();
                System.out.println("\nTransaction rolled back. Database unchanged.");

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}

