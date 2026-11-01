package com.vishesh.foodcatalog.catalog.reconciliation;

import java.math.BigDecimal;

public record EffectiveMenuSnapshotResponse(Long menuItemId, Long outletId, BigDecimal price,
        boolean availability, boolean visibility, Long version) {
}
