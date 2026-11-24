package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;
import java.util.List;

/** Immutable complete replacement snapshot of one bundle and its resolved children. */
public record EventBundleSnapshot(
        Long menuItemId,
        String name,
        BigDecimal price,
        List<EventBundleChild> children) {

    public EventBundleSnapshot {
        if (menuItemId == null || menuItemId <= 0) {
            throw new IllegalArgumentException("Bundle menu item ID must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Bundle name cannot be blank");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Bundle price must be zero or greater");
        }
        if (children == null) {
            throw new IllegalArgumentException("Bundle children are required");
        }
        if (children.stream().anyMatch(child -> child == null)) {
            throw new IllegalArgumentException("Bundle children cannot contain null values");
        }

        name = name.trim();
        children = List.copyOf(children);
    }
}
