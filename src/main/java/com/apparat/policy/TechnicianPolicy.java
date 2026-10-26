package com.apparat.policy;

public class TechnicianPolicy extends BookingPolicy {

    public static final int MAX_MINUTES = 360;
    public static final int MAX_ACTIVE = 10;

    public TechnicianPolicy() {
        super("Technicians", "a technician");
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
