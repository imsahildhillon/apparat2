package com.apparat.service;

import com.apparat.dao.ReportDao;
import com.apparat.dto.UsageCount;
import com.apparat.dto.UtilizationRow;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportService {

    public static final int MAX_RANGE_DAYS = 366;

    private final ReportDao reportDao = new ReportDao();

    /**
     * Utilization per instrument from one date to another (both inclusive).
     * Utilization is booked hours divided by the hours the lab is open.
     */
    public List<UtilizationRow> utilization(LocalDate from, LocalDate to)
            throws SQLException {

        // 1. Validate the date range.
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Both dates are required."
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "The start date must not be after the end date."
            );
        }

        long days = ChronoUnit.DAYS.between(from, to) + 1;

        if (days > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Please choose a range of at most "
                            + MAX_RANGE_DAYS + " days."
            );
        }

        // 2. Count booked slots in the range. The end date is inclusive,
        //    so the query stops at the start of the following day.
        List<UsageCount> counts = reportDao.findUsageCounts(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );

        // 3. Turn slot counts into hours and percentages.
        double openHoursPerDay = Duration.between(
                BookingService.OPEN, BookingService.CLOSE
        ).toMinutes() / 60.0;

        double capacityHours = days * openHoursPerDay;

        return counts.stream()
                .map(count -> {
                    double hours = count.slots()
                            * BookingService.SLOT_MINUTES / 60.0;

                    double percent = capacityHours > 0
                            ? hours / capacityHours * 100
                            : 0;

                    return new UtilizationRow(
                            count.resourceId(),
                            count.resourceName(),
                            count.bookings(),
                            round(hours, 2),
                            round(percent, 1)
                    );
                })
                .toList();
    }

    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
