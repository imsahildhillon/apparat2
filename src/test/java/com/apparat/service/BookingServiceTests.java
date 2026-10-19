package com.apparat.service;

import java.time.LocalDateTime;

public class BookingServiceTests {
    public static void main(String[] args) {
        BookingService bookingService = new BookingService();

        //1. 10:00-10:29 -expect-error
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 10, 29);

        bookingService.bookingStatus(start,end);

        //2. 10:00-10:30 -expect-pass
        start = LocalDateTime.of(2026, 10, 8, 10, 0);
        end = LocalDateTime.of(2026, 10, 8, 10, 30);

        bookingService.bookingStatus(start,end);

        //2. 9:00-9:30 -expect-pass
        start = LocalDateTime.of(2026, 10, 8, 9, 0);
        end = LocalDateTime.of(2026, 10, 8, 9, 30);

        bookingService.bookingStatus(start,end);

        //3. 10:00-10:00 -expect-error
        start = LocalDateTime.of(2026, 10, 8, 10, 0);
        end = LocalDateTime.of(2026, 10, 8, 10, 0);

        bookingService.bookingStatus(start,end);

        //4. 11:00-10:00 -expect-error
        start = LocalDateTime.of(2026, 10, 8, 11, 0);
        end = LocalDateTime.of(2026, 10, 8, 10, 0);

        bookingService.bookingStatus(start,end);

        //5. 17:00-10:00(tomorrow) -expect-error
        start = LocalDateTime.of(2026, 10, 8, 17, 0);
        end = LocalDateTime.of(2026, 10, 9, 10, 0);

        bookingService.bookingStatus(start,end);

        //6. 17:30-18:00 -expect-pass
        start = LocalDateTime.of(2026, 10, 8, 17, 30);
        end = LocalDateTime.of(2026, 10, 8, 18, 0);

        bookingService.bookingStatus(start,end);

    }
}
