package com.apparat.dto;

import java.time.LocalDateTime;

/**
 * One maintenance window with the instrument's name, for the staff page.
 */
public record MaintenanceView(
        int id,
        String resourceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason
) {}
