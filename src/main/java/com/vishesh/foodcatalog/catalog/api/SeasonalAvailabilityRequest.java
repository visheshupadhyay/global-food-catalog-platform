package com.vishesh.foodcatalog.catalog.api;

import java.time.LocalDate;

/**
 * Request for replacing a base or outlet-specific seasonal availability rule.
 */
public record SeasonalAvailabilityRequest(Long outletId, LocalDate startDate, LocalDate endDate,
        Long expectedVersion) {
}
