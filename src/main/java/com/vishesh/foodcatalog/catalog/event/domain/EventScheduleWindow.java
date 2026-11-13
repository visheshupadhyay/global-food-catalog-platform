package com.vishesh.foodcatalog.catalog.event.domain;

import java.time.LocalTime;

/** Time window carried in a schedule-change event. */
public record EventScheduleWindow(LocalTime startTime, LocalTime endTime) {
    public EventScheduleWindow {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Schedule window start must be before end");
        }
    }
}
