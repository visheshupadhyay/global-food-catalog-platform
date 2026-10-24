package com.vishesh.foodcatalog.catalog.effective.domain;

import java.util.List;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

public record EffectiveMenuSourceData(
        Long outletId,
        ChainStatus outletStatus,
        List<BaseMenuItemSnapshot> baseItems,
        List<OutletOverrideSnapshot> overrides,
        List<OutletLocalMenuItemSnapshot> localItems) {

    public EffectiveMenuSourceData {
        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }
        if (outletStatus == null) {
            throw new IllegalArgumentException("Outlet status must not be null");
        }
        if (baseItems == null || overrides == null || localItems == null) {
            throw new IllegalArgumentException("Menu source lists must not be null");
        }

        // Take our own unchangeable copies. The provider may reuse or change its lists later,
        // but this menu snapshot must keep the same ingredients it was created with.
        baseItems = List.copyOf(baseItems);
        overrides = List.copyOf(overrides);
        localItems = List.copyOf(localItems);
    }

}
