package com.apparat.dto;

/**
 * Raw numbers for one instrument in a report period.
 */
public record UsageCount(
        int resourceId,
        String resourceName,
        int bookings,
        int slots
) {}
