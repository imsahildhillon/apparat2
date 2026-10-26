package com.apparat.model;

import java.time.LocalDateTime;

public class MaintenanceWindow {
    private int id;
    private int resourceId;
    private LocalDateTime start;
    private LocalDateTime end;
    private String reason;
    private int createdBy;

    public MaintenanceWindow(
            int id,
            int resourceId,
            LocalDateTime start,
            LocalDateTime end,
            String reason,
            int createdBy
    ) {
        if (start == null) {
            throw new IllegalArgumentException("Start cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End cannot be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start must be before end");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Reason cannot be null or blank"
            );
        }

        this.id = id;
        this.resourceId = resourceId;
        this.start = start;
        this.end = end;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    public int getId() {
        return id;
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
    public String getReason() {
        return reason;
    }
    public int getCreatedBy() {
        return createdBy;
    }
}
