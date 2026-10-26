package com.apparat.service;

import com.apparat.dto.SlotView;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityServiceTests {

    private final AvailabilityService availabilityService =
            new AvailabilityService();

    private final LocalDate date = LocalDate.of(2026, 11, 10);

    @Test
    void gridCoversOpeningHoursInHalfHourSlots() {
        List<SlotView> grid = availabilityService.buildGrid(date, Set.of());

        // 09:00 to 18:00 is 9 hours, which is 18 half-hour slots.
        assertEquals(18, grid.size());
        assertEquals("09:00 - 09:30", grid.get(0).label());
        assertEquals("17:30 - 18:00", grid.get(17).label());
    }

    @Test
    void emptyDayHasNoBookedSlots() {
        List<SlotView> grid = availabilityService.buildGrid(date, Set.of());

        assertTrue(grid.stream().noneMatch(SlotView::booked));
    }

    @Test
    void onlyTheBookedStartsAreMarked() {
        Set<LocalDateTime> booked = Set.of(
                date.atTime(10, 0),
                date.atTime(10, 30)
        );

        List<SlotView> grid = availabilityService.buildGrid(date, booked);

        assertEquals(2, grid.stream().filter(SlotView::booked).count());
        assertTrue(grid.get(2).booked());   // 10:00
        assertTrue(grid.get(3).booked());   // 10:30
        assertFalse(grid.get(4).booked());  // 11:00
    }

    @Test
    void closingTimeHasNoSlotOfItsOwn() {
        List<SlotView> grid = availabilityService.buildGrid(
                date, Set.of(date.atTime(18, 0))
        );

        assertEquals(18, grid.size());
        assertTrue(grid.stream().noneMatch(SlotView::booked));
    }

    @Test
    void maintenanceSlotsAreMarkedSeparatelyFromBookedOnes() {
        List<SlotView> grid = availabilityService.buildGrid(
                date,
                Set.of(date.atTime(9, 0)),
                Set.of(date.atTime(10, 0), date.atTime(10, 30))
        );

        assertTrue(grid.get(0).booked());
        assertFalse(grid.get(0).maintenance());

        assertTrue(grid.get(2).maintenance());   // 10:00
        assertTrue(grid.get(3).maintenance());   // 10:30
        assertFalse(grid.get(3).booked());
        assertFalse(grid.get(4).maintenance());  // 11:00
    }

    @Test
    void aWindowCoversEverySlotFromItsStartUpToItsEnd() {
        com.apparat.model.MaintenanceWindow window =
                new com.apparat.model.MaintenanceWindow(
                        1, 1, date.atTime(10, 0), date.atTime(11, 30),
                        "Cleaning", 1);

        Set<LocalDateTime> starts = availabilityService
                .maintenanceSlotStarts(List.of(window), date);

        assertEquals(Set.of(
                date.atTime(10, 0), date.atTime(10, 30),
                date.atTime(11, 0)), starts);
    }

    @Test
    void windowsFromOtherDaysDoNotMarkThisDay() {
        com.apparat.model.MaintenanceWindow window =
                new com.apparat.model.MaintenanceWindow(
                        1, 1, date.plusDays(1).atTime(10, 0),
                        date.plusDays(1).atTime(11, 0), "Cleaning", 1);

        assertTrue(availabilityService
                .maintenanceSlotStarts(List.of(window), date).isEmpty());
    }
}
