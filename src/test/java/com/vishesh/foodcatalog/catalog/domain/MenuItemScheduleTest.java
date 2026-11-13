package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MenuItemScheduleTest {

    @Test
    void acceptsBaseScheduleWithMultipleNonOverlappingWindows() {
        MenuItemSchedule schedule = new MenuItemSchedule(101L, null, Map.of(
                Weekday.MONDAY, List.of(
                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                        new ScheduleWindow(LocalTime.of(17, 0), LocalTime.of(22, 0)))));

        assertEquals(101L, schedule.getMenuItemId());
        assertEquals(List.of(Weekday.MONDAY), schedule.getWindowsByDay().keySet().stream().toList());
        assertEquals(2, schedule.getWindows(Weekday.MONDAY).size());
    }

    @Test
    void acceptsOutletOverride() {
        MenuItemSchedule schedule = new MenuItemSchedule(101L, 501L, Map.of());

        assertEquals(501L, schedule.getOutletId());
    }

    @Test
    void rejectsInvalidIdsAndMissingWindows() {
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItemSchedule(0L, null, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItemSchedule(101L, 0L, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItemSchedule(101L, null, null));
    }

    @Test
    void rejectsOverlappingAndTouchingWindows() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItemSchedule(101L, null, Map.of(
                Weekday.MONDAY, List.of(
                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                        new ScheduleWindow(LocalTime.of(13, 0), LocalTime.of(16, 0))))));

        assertThrows(IllegalArgumentException.class, () -> new MenuItemSchedule(101L, null, Map.of(
                Weekday.MONDAY, List.of(
                        new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(14, 0)),
                        new ScheduleWindow(LocalTime.of(14, 0), LocalTime.of(16, 0))))));
    }

    @Test
    void rejectsWindowWithEndBeforeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduleWindow(LocalTime.of(16, 0), LocalTime.of(9, 0)));
    }
}
