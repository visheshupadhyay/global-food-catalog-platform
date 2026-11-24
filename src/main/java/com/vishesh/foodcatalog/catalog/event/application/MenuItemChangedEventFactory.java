package com.vishesh.foodcatalog.catalog.event.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierGroup;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.EventScheduleWindow;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

/** Builds the standard Kafka/outbox message whenever a menu item changes. */
@Component
public class MenuItemChangedEventFactory {

    /**
     * Creates the complete first snapshot that lets Effective Menu introduce an
     * item when its Catalog lifecycle changes from DRAFT to ACTIVE.
     */
    public MenuItemChangedEvent createMenuItemActivated(
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
        if (aggregateVersion == null || aggregateVersion < 0) {
            throw new IllegalArgumentException("Aggregate version must be zero or greater");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must be zero or greater");
        }
        if (availability == null) {
            throw new IllegalArgumentException("Availability is required for item activation");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("Visibility is required for item activation");
        }
        if (status != MenuItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Activated item status must be ACTIVE");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name is required for item activation");
        }

        return new MenuItemChangedEvent(
                UUID.randomUUID(), null, OutboxEventType.MENU_ITEM_ACTIVATED,
                menuItemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, Instant.now(),
                correlationId == null ? null : correlationId.trim(), status, itemName.trim());
    }

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
        return create(menuItemId, restaurantChainId, aggregateVersion, price, availability, visibility,
                outletId, correlationId, status, itemName, false, null);
    }

    public MenuItemChangedEvent create(Long menuItemId, Long restaurantChainId, Long aggregateVersion,
            BigDecimal price, Boolean availability, Boolean visibility, Long outletId,
            String correlationId, MenuItemStatus status, String itemName,
            Boolean scheduleChanged, Long scheduleVersion) {
        return create(menuItemId, restaurantChainId, aggregateVersion, price, availability, visibility,
                outletId, correlationId, status, itemName, scheduleChanged, scheduleVersion, null, Map.of());
    }

    public MenuItemChangedEvent create(Long menuItemId, Long restaurantChainId, Long aggregateVersion,
            BigDecimal price, Boolean availability, Boolean visibility, Long outletId,
            String correlationId, MenuItemStatus status, String itemName,
            Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows) {

        validatePositive(menuItemId, "Menu item id");
        validatePositive(restaurantChainId, "Restaurant chain id");
        validatePositiveWhenPresent(outletId, "Outlet id");

        if (itemName != null && itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank when supplied");
        }
        return new MenuItemChangedEvent(
                UUID.randomUUID(),
                null,
                eventTypeFor(scheduleChanged),
                menuItemId,
                restaurantChainId,
                outletId,
                aggregateVersion,
                price,
                availability,
                visibility,
                Instant.now(),
                correlationId == null ? null : correlationId.trim(),
                status, itemName == null ? null : itemName.trim(), scheduleChanged, scheduleVersion,
                scheduleTimezone, scheduleWindows);
    }

    public MenuItemChangedEvent create(Long menuItemId, Long restaurantChainId, Long aggregateVersion,
            BigDecimal price, Boolean availability, Boolean visibility, Long outletId,
            String correlationId, MenuItemStatus status, String itemName,
            Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows,
            Map<String, List<EventScheduleWindow>> baseScheduleWindows,
            Map<String, List<EventScheduleWindow>> outletScheduleWindows) {
        validatePositive(menuItemId, "Menu item id");
        validatePositive(restaurantChainId, "Restaurant chain id");
        validatePositiveWhenPresent(outletId, "Outlet id");
        if (itemName != null && itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank when supplied");
        }
        return new MenuItemChangedEvent(UUID.randomUUID(), null, eventTypeFor(scheduleChanged),
                menuItemId, restaurantChainId, outletId, aggregateVersion, price, availability, visibility,
                Instant.now(), correlationId == null ? null : correlationId.trim(), status,
                itemName == null ? null : itemName.trim(), scheduleChanged, scheduleVersion,
                scheduleTimezone, scheduleWindows, baseScheduleWindows, outletScheduleWindows);
    }

    /**
     * Creates a complete replacement snapshot after a menu item's modifier
     * configuration changes.
     */
    public MenuItemChangedEvent createModifierConfigurationChanged(
            Long menuItemId,
            Long restaurantChainId,
            Long aggregateVersion,
            Long outletId,
            String correlationId,
            MenuItemStatus status,
            String itemName,
            List<EventModifierGroup> modifierGroups) {

        validatePositive(menuItemId, "Menu item id");
        validatePositive(restaurantChainId, "Restaurant chain id");
        validatePositiveWhenPresent(outletId, "Outlet id");
        if (modifierGroups == null) {
            throw new IllegalArgumentException("Modifier groups are required");
        }
        if (itemName != null && itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank when supplied");
        }

        return new MenuItemChangedEvent(
                UUID.randomUUID(), null, OutboxEventType.MODIFIER_CONFIGURATION_CHANGED,
                menuItemId, restaurantChainId, outletId, aggregateVersion,
                null, null, null, Instant.now(),
                correlationId == null ? null : correlationId.trim(),
                status, itemName == null ? null : itemName.trim(),
                false, null, null, Map.of(), Map.of(), Map.of(),
                false, null, null, null, null,
                true, modifierGroups);
    }

    /** Creates a complete replacement snapshot after a bundle's child structure changes. */
    public MenuItemChangedEvent createBundleConfigurationChanged(
            Long menuItemId,
            Long restaurantChainId,
            Long aggregateVersion,
            Long outletId,
            String correlationId,
            MenuItemStatus status,
            String itemName,
            EventBundleSnapshot bundleSnapshot) {

        validatePositive(menuItemId, "Menu item id");
        validatePositive(restaurantChainId, "Restaurant chain id");
        validatePositiveWhenPresent(outletId, "Outlet id");
        if (bundleSnapshot == null) {
            throw new IllegalArgumentException("Bundle snapshot is required");
        }
        if (!menuItemId.equals(bundleSnapshot.menuItemId())) {
            throw new IllegalArgumentException("Bundle snapshot must belong to the changed menu item");
        }
        if (itemName != null && itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank when supplied");
        }

        return new MenuItemChangedEvent(
                UUID.randomUUID(), null, OutboxEventType.BUNDLE_CONFIGURATION_CHANGED,
                menuItemId, restaurantChainId, outletId, aggregateVersion,
                null, null, null, Instant.now(),
                correlationId == null ? null : correlationId.trim(),
                status, itemName == null ? null : itemName.trim(),
                false, null, null, Map.of(), Map.of(), Map.of(),
                false, null, null, null, null,
                false, List.of(), true, bundleSnapshot);
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

    private static OutboxEventType eventTypeFor(Boolean scheduleChanged) {
        return Boolean.TRUE.equals(scheduleChanged)
                ? OutboxEventType.SCHEDULE_CHANGED
                : OutboxEventType.MENU_ITEM_CHANGED;
    }

    /**
     * Creates the sparse event emitted after a seasonal availability replacement.
     */
    public MenuItemChangedEvent createSeasonalAvailabilityChanged(Long menuItemId, Long restaurantChainId,
            Long aggregateVersion, Long seasonalAvailabilityVersion, Long outletId, String correlationId,
            MenuItemStatus status,
            Boolean availability, String timezone, LocalDate startDate, LocalDate endDate) {
        validatePositive(menuItemId, "Menu item id");
        validatePositive(restaurantChainId, "Restaurant chain id");
        validatePositiveWhenPresent(outletId, "Outlet id");
        return new MenuItemChangedEvent(UUID.randomUUID(), null,
                OutboxEventType.SEASONAL_AVAILABILITY_CHANGED, menuItemId,
                restaurantChainId, outletId, aggregateVersion, null,
                availability, null, Instant.now(),
                correlationId == null ? null : correlationId.trim(),
                status, null, false, null,
                null, Map.of(), Map.of(), Map.of(),
                true, seasonalAvailabilityVersion,
                startDate, endDate, timezone);
    }
}
