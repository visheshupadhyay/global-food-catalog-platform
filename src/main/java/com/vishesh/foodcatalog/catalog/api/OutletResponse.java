package com.vishesh.foodcatalog.catalog.api;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/** The public representation of an outlet created by the Catalog API. */
public record OutletResponse(
        Long id,
        Long restaurantChainId,
        String displayName,
        String outletCode,
        String currencyCode,
        String timezone,
        ChainStatus status,
        Long version) {
}
