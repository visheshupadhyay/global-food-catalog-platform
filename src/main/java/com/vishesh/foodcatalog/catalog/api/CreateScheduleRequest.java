package com.vishesh.foodcatalog.catalog.api;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import com.vishesh.foodcatalog.catalog.domain.Weekday;

/** Request body for creating a base or outlet-specific weekly schedule. */
public record CreateScheduleRequest(Long outletId, Map<Weekday, List<Window>> windows) {
    public record Window(LocalTime startTime, LocalTime endTime) { }
}
