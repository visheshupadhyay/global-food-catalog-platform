package com.vishesh.foodcatalog.catalog.api;

/** Schedule replacement request carrying the expected version for stale-write protection. */
public record UpdateScheduleRequest(Long outletId, Long expectedVersion,
        CreateScheduleRequest request) { }
