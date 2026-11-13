package com.vishesh.foodcatalog.catalog.domain;

import java.time.LocalTime;
import java.util.Objects;

/** One half-open recurring time window: start is included and end is excluded. */
public final class ScheduleWindow {
    private final LocalTime startTime;
    private final LocalTime endTime;

    public ScheduleWindow(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Schedule window times are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Schedule window start must be before end");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    boolean overlapsOrTouches(ScheduleWindow other) {
        Objects.requireNonNull(other, "Other schedule window is required");
        return !endTime.isBefore(other.startTime) && !other.endTime.isBefore(startTime);
    }
}
