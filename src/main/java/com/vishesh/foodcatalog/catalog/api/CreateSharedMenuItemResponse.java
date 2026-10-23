package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

/**
 * CreateSharedMenuItemResponse
 */
public record CreateSharedMenuItemResponse(Long id,
        String businessCode,
        String itemName,
        BigDecimal itemPrice,
        Long restaurantChainId,
        MenuItemStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        Long outletId,
        Long menuGroupId,
        Long menuCategoryId) {
}
