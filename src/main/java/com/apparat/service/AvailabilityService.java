package com.apparat.service;

import com.apparat.dao.AvailabilityDao;
import com.apparat.dao.MaintenanceDao;
import com.apparat.dto.SlotView;
import com.apparat.model.MaintenanceWindow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AvailabilityService {

    private final AvailabilityDao availabilityDao = new AvailabilityDao();
    private final MaintenanceDao maintenanceDao = new MaintenanceDao();

    public List<SlotView> getDayGrid(int resourceId, LocalDate date)
            throws SQLException {

        Set<LocalDateTime> bookedStarts =
                availabilityDao.findBookedSlotStarts(resourceId, date);

        List<MaintenanceWindow> windows = maintenanceDao.findOverlapping(
                resourceId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );

        return buildGrid(
                date, bookedStarts, maintenanceSlotStarts(windows, date)
        );
    }

    public List<SlotView> buildGrid(
            LocalDate date,
            Set<LocalDateTime> bookedStarts
    ) {
        return buildGrid(date, bookedStarts, Set.of());
    }

    /**
     * Builds the grid from opening time up to (not including) closing time.
     * It touches no database, so it can be tested on its own.
     */
    public List<SlotView> buildGrid(
            LocalDate date,
            Set<LocalDateTime> bookedStarts,
            Set<LocalDateTime> maintenanceStarts
    ) {

        List<SlotView> grid = new ArrayList<>();

        LocalDateTime current = date.atTime(BookingService.OPEN);
        LocalDateTime closing = date.atTime(BookingService.CLOSE);

        // The closing time is exclusive, like the end of a booking.
        while (current.isBefore(closing)) {
            LocalDateTime slotEnd =
                    current.plusMinutes(BookingService.SLOT_MINUTES);

            String label = current.toLocalTime()
                    + " - " + slotEnd.toLocalTime();

            grid.add(new SlotView(
                    current,
                    label,
                    bookedStarts.contains(current),
                    maintenanceStarts.contains(current)
            ));

            current = slotEnd;
        }

        return grid;
    }

    /**
     * The start time of every 30-minute slot, on the given day,
     * covered by one of the maintenance windows.
     */
    public Set<LocalDateTime> maintenanceSlotStarts(
            List<MaintenanceWindow> windows,
            LocalDate date
    ) {

        Set<LocalDateTime> starts = new HashSet<>();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        for (MaintenanceWindow window : windows) {
            LocalDateTime current = window.getStart();

            while (current.isBefore(window.getEnd())) {
                if (!current.isBefore(dayStart) && current.isBefore(dayEnd)) {
                    starts.add(current);
                }

                current = current.plusMinutes(BookingService.SLOT_MINUTES);
            }
        }

        return starts;
    }
}
