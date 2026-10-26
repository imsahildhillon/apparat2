package com.apparat.policy;

public class StudentPolicy extends BookingPolicy {

    public static final int MAX_MINUTES = 120;
    public static final int MAX_ACTIVE = 3;

    public StudentPolicy() {
        super("Students", "a student");
    }

    @Override
    public int maxMinutesPerBooking() {
        return MAX_MINUTES;
    }

    @Override
    public int maxActiveBookings() {
        return MAX_ACTIVE;
    }
}
