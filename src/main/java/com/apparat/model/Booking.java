package com.apparat.model;

import java.time.LocalDateTime;

public class Booking {
    private int id;
    private int userId;
    private int resourceId;
    private LocalDateTime start;
    private LocalDateTime end;
    private BookingStatus status;

    public Booking(int id, int userId, int resourceId, LocalDateTime start, LocalDateTime end, BookingStatus status) {

        if (start == null) {
            throw new IllegalArgumentException("Start cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End cannot be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "Start must be before end"
            );
        }

        if (status == null ) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.id = id;
        this.userId = userId;
        this.resourceId = resourceId;
        this.start = start;
        this.end = end;
        this.status = status;
    }

    public int getId() {
        return id;
    }
    public int getUserId() {
        return userId;
    }
    public int getResourceId() {
        return resourceId;
    }
    public LocalDateTime getStart() {
        return start;
    }
    public LocalDateTime getEnd() {
        return end;
    }
    public BookingStatus getStatus() {
        return status;
    }
}

