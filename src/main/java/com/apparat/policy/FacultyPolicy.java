package com.apparat.policy;

public class FacultyPolicy extends BookingPolicy {

    public static final int MAX_MINUTES = 240;
    public static final int MAX_ACTIVE = 6;

    public FacultyPolicy() {
        super("Faculty members", "faculty");
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
