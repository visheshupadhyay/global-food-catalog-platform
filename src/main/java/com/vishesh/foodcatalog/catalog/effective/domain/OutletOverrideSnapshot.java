package com.vishesh.foodcatalog.catalog.effective.domain;

import java.math.BigDecimal;

public class OutletOverrideSnapshot {
    private final Long menuItemId;
    private final Long outletId;
    private final BigDecimal priceOverride;
    private final Boolean availableOverride;
    private final Boolean visibleOverride;

    public OutletOverrideSnapshot(Long menuItemId,
            Long outletId,
            BigDecimal priceOverride,
            Boolean availableOverride,
            Boolean visibleOverride) {

        if (menuItemId == null || menuItemId <= 0) {
            throw new IllegalArgumentException("Menu item ID must be greater than zero");
        }

        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }

        if (priceOverride != null && priceOverride.signum() < 0) {
            throw new IllegalArgumentException("Price override must be zero or greater");
        }
        this.menuItemId = menuItemId;
        this.outletId = outletId;
        this.priceOverride = priceOverride;
        this.availableOverride = availableOverride;
        this.visibleOverride = visibleOverride;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public Boolean getAvailableOverride() {
        return availableOverride;
    }

    public Boolean getVisibleOverride() {
        return visibleOverride;
    }

}
