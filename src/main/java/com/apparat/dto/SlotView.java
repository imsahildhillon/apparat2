package com.apparat.dto;

import java.time.LocalDateTime;


 //One 30-minute cell of the availability

public record SlotView(
        LocalDateTime start,
        String label,
        boolean booked,
        boolean maintenance
) {}
