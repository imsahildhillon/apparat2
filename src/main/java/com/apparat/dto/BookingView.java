
package com.apparat.dto;

import java.time.LocalDateTime;

public record BookingView(
        int id,
        String resourceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {}

