package com.apparat.servlet;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeServletTests {

    @Test
    void theGreetingFollowsTheTimeOfDay() {
        assertEquals("Good morning", HomeServlet.greetingFor(LocalTime.of(0, 0)));
        assertEquals("Good morning", HomeServlet.greetingFor(LocalTime.of(11, 59)));
        assertEquals("Good afternoon", HomeServlet.greetingFor(LocalTime.of(12, 0)));
        assertEquals("Good afternoon", HomeServlet.greetingFor(LocalTime.of(16, 59)));
        assertEquals("Good evening", HomeServlet.greetingFor(LocalTime.of(17, 0)));
        assertEquals("Good evening", HomeServlet.greetingFor(LocalTime.of(23, 59)));
    }

    @Test
    void aBookingTimeIsWrittenInPlainWords() {
        LocalDateTime start = LocalDateTime.of(2030, 3, 12, 10, 0);

        assertEquals("Tue 12 Mar, 10:00 - 11:30",
                HomeServlet.describeTime(start, start.plusMinutes(90)));
    }
}
