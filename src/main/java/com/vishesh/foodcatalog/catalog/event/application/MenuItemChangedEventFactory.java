package com.vishesh.foodcatalog.catalog.event.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

/** Builds the standard Kafka/outbox message whenever a menu item changes. */
@Component
public class MenuItemChangedEventFactory {

    /**
     * Creates a sparse event: null means that particular field did not change.
     * The version must be the version returned by persistence after the update.
     */
    public MenuItemChangedEvent create(
            Long menuItemId,
            Long restaurantChainId,
            Long aggregateVersion,
            BigDecimal price,
            Boolean availability,
            Boolean visibility,
            Long outletId,
            String correlationId,
            MenuItemStatus status,
            String itemName) {

        validatePositive(menuItemId, "Menu item id");
        validatePositive(restaurantChainId, "Restaurant chain id");
        validatePositiveWhenPresent(outletId, "Outlet id");

        if (itemName != null && itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank when supplied");
        }
        return new MenuItemChangedEvent(
                UUID.randomUUID(),
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                menuItemId,
                restaurantChainId,
                outletId,
                aggregateVersion,
                price,
                availability,
                visibility,
                Instant.now(),
                correlationId == null ? null : correlationId.trim(),
                status,
                itemName == null ? null : itemName.trim());
    }

    private static void validatePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void validatePositiveWhenPresent(Long value, String field) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(field + " must be positive when supplied");
        }
    }
}
