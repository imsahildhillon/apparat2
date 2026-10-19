package com.apparat.service;
import java.time.*;

public class BookingService {
    public static final LocalTime OPEN = LocalTime.of(9, 0);
    public static final LocalTime CLOSE = LocalTime.of(18, 0);


    public boolean isValidRange(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        //1.start time is before end time
        if (start.isAfter(end)) {
            return false;
        }
        //2.booking duration is more than or equal to 30 mins
        if (duration.toMinutes() < 30) {
            return false;
        }
        //3.Same Day
        if (!start.toLocalDate().isEqual(end.toLocalDate())) {
            return false;
        }
        //4. start and end time must be between OPEN and CLOSE;
        if (start.toLocalTime().isAfter(CLOSE) || start.toLocalTime().isBefore(OPEN)) {
            return false;
        }
        if (end.toLocalTime().isBefore(OPEN)
                || end.toLocalTime().isAfter(CLOSE)) {
            return false;
        }

        return true;
    }

    public void bookingStatus(LocalDateTime start, LocalDateTime end) {
        System.out.println("Time:"+start.toLocalTime()+ "-" + end.toLocalTime()+ ":-");
        if (isValidRange(start, end)) {
            System.out.println( "Booking Confirmed!!");
        } else {
            System.out.println("Error 400: Bad Request!!");
        }
    }


}