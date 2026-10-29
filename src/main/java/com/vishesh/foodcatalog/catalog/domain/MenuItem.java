package com.vishesh.foodcatalog.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A food item that belongs either to a shared menu group or one outlet. */
public class MenuItem {
    private Long id;
    private String itemName;
    private BigDecimal itemPrice;
    private Instant createdAt;
    private Long menuGroupId;
    private Long menuCategoryId;
    private Long outletId;
    private String businessCode;
    private MenuItemStatus status;
    private boolean visibility;
    private boolean availability;
    private Long version;
    private Long restaurantChainId;
    private Instant updatedAt;

    public MenuItem(
            String businessCode,
            String itemName,
            BigDecimal itemPrice,
            Long restaurantChainId,
            Long menuGroupId,
            Long menuCategoryId,
            Long outletId) {

        validateCommonFields(businessCode, itemName, itemPrice, restaurantChainId,
                menuGroupId, menuCategoryId, outletId, MenuItemStatus.DRAFT, 0L);

        businessCode = businessCode.trim();
        itemName = itemName.trim();

        this.id = null;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        Instant now = Instant.now();
        this.createdAt = now;
        this.menuGroupId = menuGroupId;
        this.menuCategoryId = menuCategoryId;
        this.outletId = outletId;
        this.businessCode = businessCode;
        this.status = MenuItemStatus.DRAFT;
        this.visibility = true;
        this.availability = true;
        this.version = 0L;
        this.restaurantChainId = restaurantChainId;
        this.updatedAt = now;
    }

    public MenuItem(Long id, String itemName, BigDecimal itemPrice, Instant createdAt, Long menuGroupId,
            Long menuCategoryId, Long outletId, String businessCode, MenuItemStatus status, boolean visibility,
            boolean availability, Long version, Long restaurantChainId, Instant updatedAt) {

        validateCommonFields(businessCode, itemName, itemPrice, restaurantChainId,
                menuGroupId, menuCategoryId, outletId, status, version);
        if (id == null) {
            throw new IllegalArgumentException("Menu item ID cannot be null");
        }

        if (id <= 0) {
            throw new IllegalArgumentException("Menu item ID should be greater than zero");
        }

        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps should be present");
        }

        itemName = itemName.trim();
        businessCode = businessCode.trim();
        this.id = id;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.createdAt = createdAt;
        this.menuGroupId = menuGroupId;
        this.menuCategoryId = menuCategoryId;
        this.outletId = outletId;
        this.businessCode = businessCode;
        this.status = status;
        this.visibility = visibility;
        this.availability = availability;
        this.version = version;
        this.restaurantChainId = restaurantChainId;
        this.updatedAt = updatedAt;
    }

    /**
     * Checks rules that apply to both a newly created and a persisted item.
     * A menu item must belong to exactly one owner: a shared menu group or an
     * outlet.
     */
    private static void validateCommonFields(
            String businessCode,
            String itemName,
            BigDecimal itemPrice,
            Long restaurantChainId,
            Long menuGroupId,
            Long menuCategoryId,
            Long outletId,
            MenuItemStatus status,
            Long version) {

        if (businessCode == null || businessCode.isBlank()) {
            throw new IllegalArgumentException("Business code cannot be null or blank");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or blank");
        }
        if (itemPrice == null || itemPrice.signum() < 0) {
            throw new IllegalArgumentException("Item price must be zero or greater");
        }
        if (restaurantChainId == null || restaurantChainId <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }
        if ((menuGroupId == null) == (outletId == null)) {
            throw new IllegalArgumentException("Exactly one of menu group ID or outlet ID is required");
        }
        if (menuGroupId != null && menuGroupId <= 0) {
            throw new IllegalArgumentException("Menu group ID must be greater than zero");
        }
        if (outletId != null && outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }
        if (menuCategoryId != null && menuCategoryId <= 0) {
            throw new IllegalArgumentException("Menu category ID must be greater than zero");
        }
        if (status == null) {
            throw new IllegalArgumentException("Menu item status must be present");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version must be zero or greater");
        }
    }

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getMenuGroupId() {
        return menuGroupId;
    }

    public Long getMenuCategoryId() {
        return menuCategoryId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public MenuItemStatus getStatus() {
        return status;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public boolean isAvailability() {
        return availability;
    }

    public Long getVersion() {
        return version;
    }

    public Long getRestaurantChainId() {
        return restaurantChainId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updatePrice(BigDecimal updatedPrice) {
        if (updatedPrice == null || updatedPrice.signum() < 0) {
            throw new IllegalArgumentException("Item price must be zero or greater");
        }
        this.itemPrice = updatedPrice;
    }

    /** Changes the lifecycle state only when the transition is allowed by business rules. */
    public void updateStatus(MenuItemStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Menu item status cannot be null");
        }

        if (this.status == newStatus) {
            return;
        }

        boolean allowed = switch (this.status) {
            case DRAFT -> newStatus == MenuItemStatus.ACTIVE;
            case ACTIVE -> newStatus == MenuItemStatus.HIDDEN
                    || newStatus == MenuItemStatus.ARCHIVED;
            case HIDDEN -> newStatus == MenuItemStatus.ACTIVE
                    || newStatus == MenuItemStatus.ARCHIVED;
            case ARCHIVED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Cannot change menu item status from " + this.status + " to " + newStatus);
        }

        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void updateAvailability(boolean updatedAvailability) {

        this.availability = updatedAvailability;
    }

    public void updateVisibility(boolean updatedVisibility) {
        this.visibility = updatedVisibility;
    }

}
