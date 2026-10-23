package com.vishesh.foodcatalog.catalog.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** The database form of a menu item. JPA maps this object to one table row. */
@Entity
@Table(name = "menu_item")
public class MenuItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @Column(name = "item_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal itemPrice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "menu_group_id")
    private Long menuGroupId;

    @Column(name = "menu_category_id")
    private Long menuCategoryId;

    @Column(name = "outlet_id")
    private Long outletId;

    @Column(name = "business_code", length = 100, nullable = false)
    private String businessCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MenuItemStatus status;

    @Column(name = "visibility", nullable = false)
    private boolean visibility;

    @Column(name = "availability", nullable = false)
    private boolean availability;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "restaurant_chain_id", nullable = false)
    private Long restaurantChainId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MenuItemEntity() {
    }

    public MenuItemEntity(String businessCode,
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

    public MenuItemEntity(Long id, String itemName, BigDecimal itemPrice, Instant createdAt, Long menuGroupId,
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

}
