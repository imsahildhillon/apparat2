package com.apparat.policy;

import com.apparat.model.Role;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * What one role may book. The checking is written once, here; each role
 * only supplies its own numbers by overriding the two abstract methods.
 */
public abstract class BookingPolicy {

    private final String plural;
    private final String who;

    /**
     * @param plural the role in plural, for messages, such as "Students"
     * @param who    the role as a phrase, such as "a student"
     */
    protected BookingPolicy(String plural, String who) {
        this.plural = plural;
        this.who = who;
    }

    /** The longest single booking, in minutes. */
    public abstract int maxMinutesPerBooking();

    /** How many confirmed bookings that have not finished yet. */
    public abstract int maxActiveBookings();

    public static BookingPolicy forRole(Role role) {
        return switch (role) {
            case STUDENT -> new StudentPolicy();
            case FACULTY -> new FacultyPolicy();
            case TECHNICIAN -> new TechnicianPolicy();
            case ADMIN -> new AdminPolicy();
        };
    }

    public boolean isUnlimited() {
        return maxMinutesPerBooking() == Integer.MAX_VALUE
                && maxActiveBookings() == Integer.MAX_VALUE;
    }

    /**
     * Says which limit a booking would break, or returns empty if it is fine.
     *
     * @param activeBookings how many active bookings the user already has
     */
    public Optional<String> check(
            LocalDateTime start,
            LocalDateTime end,
            int activeBookings
    ) {

        long minutes = Duration.between(start, end).toMinutes();

        if (minutes > maxMinutesPerBooking()) {
            return Optional.of(
                    plural + " can book at most "
                            + describeMinutes(maxMinutesPerBooking())
                            + " at a time. Please shorten your booking."
            );
        }

        if (activeBookings >= maxActiveBookings()) {
            return Optional.of(
                    "You already have " + activeBookings + " active booking"
                            + (activeBookings == 1 ? "" : "s")
                            + ", which is the most "
                            + plural.toLowerCase() + " can hold at once ("
                            + maxActiveBookings() + "). Cancel one, "
                            + "or wait until one finishes."
            );
        }

        return Optional.empty();
    }

    /**
     * One sentence for the booking page.
     */
    public String describe() {

        if (isUnlimited()) {
            return "As " + who + " you have no booking limits.";
        }

        return "As " + who + " you can book up to "
                + describeMinutes(maxMinutesPerBooking())
                + " at a time and hold up to " + maxActiveBookings()
                + " active bookings.";
    }

    // 120 -> "2 hours", 90 -> "1 hour 30 minutes", 30 -> "30 minutes"
    static String describeMinutes(int minutes) {

        int hours = minutes / 60;
        int rest = minutes % 60;

        String hoursText = hours == 0
                ? ""
                : hours + (hours == 1 ? " hour" : " hours");

        String restText = rest == 0 ? "" : rest + " minutes";

        return (hoursText + " " + restText).trim();
    }
}
