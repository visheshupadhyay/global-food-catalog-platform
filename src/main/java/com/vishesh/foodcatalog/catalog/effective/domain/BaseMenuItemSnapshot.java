package com.vishesh.foodcatalog.catalog.effective.domain;

import java.math.BigDecimal;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

public class BaseMenuItemSnapshot {
    private final Long menuItemId;
    private final String itemName;
    private final BigDecimal price;
    private final boolean available;
    private final boolean visible;
    private final MenuItemStatus status;

    public BaseMenuItemSnapshot(Long menuItemId,
            String itemName,
            BigDecimal price,
            boolean available,
            boolean visible,
            MenuItemStatus status) {

        
        validateCommonFields(menuItemId, itemName, price, status);
        this.menuItemId = menuItemId;
        this.itemName = itemName.trim();
        this.price = price;
        this.available = available;
        this.visible = visible;
        this.status = status;
    }

    private static void validateCommonFields(Long menuItemId, String itemName, BigDecimal price, MenuItemStatus status) {
        if (menuItemId == null || menuItemId <= 0) {
            throw new IllegalArgumentException("Menu item ID must be greater than zero");
        }

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or blank");
        }

        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Item price must be zero or greater");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    public Long getMenuItemId() {
        return menuItemId;
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

    public MenuItemStatus getStatus() {
        return status;
    }
}