package com.vishesh.foodcatalog.catalog.api;

import java.time.Instant;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/** The small, safe view of a restaurant chain that the API sends back. */
public record RestaurantChainResponse(Long id,
        String businessCode,
        String displayName,
        ChainStatus status,
        Instant createdAt,
        Instant updatedAt) {

}
