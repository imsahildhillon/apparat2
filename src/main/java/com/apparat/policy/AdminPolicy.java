package com.apparat.policy;

public class AdminPolicy extends BookingPolicy {

    public AdminPolicy() {
        super("Administrators", "an administrator");
    }

    @Override
    public int maxMinutesPerBooking() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxActiveBookings() {
        return Integer.MAX_VALUE;
    }
}
