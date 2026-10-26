package com.apparat.dto;

/**
 * One line of the utilization report.
 */
public record UtilizationRow(
        int resourceId,
        String resourceName,
        int bookings,
        double bookedHours,
        double utilizationPercent
) {}
