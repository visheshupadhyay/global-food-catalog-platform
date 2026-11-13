package com.vishesh.foodcatalog.catalog.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Weekly recurring schedule for a base menu item or one outlet override. */
public final class MenuItemSchedule {
    private final Long menuItemId;
    private final Long outletId;
    private final Map<Weekday, List<ScheduleWindow>> windowsByDay;
    private final Long version;

    public MenuItemSchedule(Long menuItemId, Long outletId,
            Map<Weekday, List<ScheduleWindow>> windowsByDay) {
        this(menuItemId, outletId, windowsByDay, 0L);
    }

    public MenuItemSchedule(Long menuItemId, Long outletId,
            Map<Weekday, List<ScheduleWindow>> windowsByDay, Long version) {
        if (menuItemId == null || menuItemId <= 0) {
            throw new IllegalArgumentException("Menu item ID must be positive");
        }
        if (outletId != null && outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be positive when supplied");
        }
        if (windowsByDay == null) {
            throw new IllegalArgumentException("Schedule windows are required");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Schedule version must be non-negative");
        }

        EnumMap<Weekday, List<ScheduleWindow>> copy = new EnumMap<>(Weekday.class);
        for (Map.Entry<Weekday, List<ScheduleWindow>> entry : windowsByDay.entrySet()) {
            Weekday weekday = entry.getKey();
            List<ScheduleWindow> windows = entry.getValue();
            if (weekday == null || windows == null) {
                throw new IllegalArgumentException("Schedule weekday and windows are required");
            }
            List<ScheduleWindow> dayWindows = new ArrayList<>(windows);
            for (int index = 0; index < dayWindows.size(); index++) {
                ScheduleWindow current = dayWindows.get(index);
                if (current == null) {
                    throw new IllegalArgumentException("Schedule windows cannot contain null values");
                }
                for (int nextIndex = index + 1; nextIndex < dayWindows.size(); nextIndex++) {
                    if (current.overlapsOrTouches(dayWindows.get(nextIndex))) {
                        throw new IllegalArgumentException(
                                "Schedule windows cannot overlap or touch on " + weekday);
                    }
                }
            }
            copy.put(weekday, List.copyOf(dayWindows));
        }

        this.menuItemId = menuItemId;
        this.outletId = outletId;
        this.windowsByDay = Collections.unmodifiableMap(copy);
        this.version = version;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    /** Null means this is the base schedule; a value identifies an outlet override. */
    public Long getOutletId() {
        return outletId;
    }

    public Map<Weekday, List<ScheduleWindow>> getWindowsByDay() {
        return windowsByDay;
    }

    public List<ScheduleWindow> getWindows(Weekday weekday) {
        return windowsByDay.getOrDefault(weekday, List.of());
    }

    public Long getVersion() {
        return version;
    }
}
