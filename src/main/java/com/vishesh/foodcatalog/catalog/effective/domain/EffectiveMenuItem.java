package com.vishesh.foodcatalog.catalog.effective.domain;

import java.math.BigDecimal;

public class EffectiveMenuItem {
    private final Long menuItemId;
    private final Long outletId;
    private final String itemName;
    private final BigDecimal price;
    private final boolean available;
    private final boolean visible;

    public EffectiveMenuItem(Long menuItemId, Long outletId, String itemName, BigDecimal price, boolean available,
            boolean visible) {

        if (menuItemId == null || menuItemId <= 0) {
            throw new IllegalArgumentException("Menu item ID must be greater than zero");
        }

        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or blank");
        }

        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Item price must be zero or greater");
        }

        this.menuItemId = menuItemId;
        this.outletId = outletId;
        this.itemName = itemName.trim();
        this.price = price;
        this.available = available;
        this.visible = visible;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isVisible() {
        return visible;
    }

}
