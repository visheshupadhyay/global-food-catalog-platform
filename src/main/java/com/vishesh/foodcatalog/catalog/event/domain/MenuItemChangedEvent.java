package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

/** Immutable message carrying a menu-item change from Catalog to Kafka. */
public final class MenuItemChangedEvent {
    private final UUID eventId;
    private final UUID originalEventId;
    private final OutboxEventType eventType;
    private final Long itemId;
    private final Long restaurantChainId;
    private final Long outletId;
    private final Long aggregateVersion;
    private final BigDecimal price;
    private final Boolean availability;
    private final Boolean visibility;
    private final Instant occurredAt;
    private final String correlationId;
    private final MenuItemStatus status;
    private final String itemName;
    private final Boolean scheduleChanged;
    private final Long scheduleVersion;
    private final String scheduleId;
    private final String scheduleTimezone;
    private final Map<String, List<EventScheduleWindow>> scheduleWindows;
    private final Map<String, List<EventScheduleWindow>> baseScheduleWindows;
    private final Map<String, List<EventScheduleWindow>> outletScheduleWindows;
    private final Boolean seasonalAvailabilityChanged;
    private final Long seasonalAvailabilityVersion;
    private final LocalDate seasonalStartDate;
    private final LocalDate seasonalEndDate;
    private final String seasonalTimezone;
    private final Boolean modifierConfigurationChanged;
    private final List<EventModifierGroup> modifierGroups;
    private final Boolean bundleConfigurationChanged;
    private final EventBundleSnapshot bundleSnapshot;


    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId,
            OutboxEventType eventType,
            Long itemId,
            Long restaurantChainId,
            Long outletId,
            Long aggregateVersion,
            BigDecimal price,
            Boolean availability,
            Boolean visibility,
            Instant occurredAt,
            String correlationId,
            MenuItemStatus status,
            String itemName) {
        this(eventId, originalEventId, eventType, itemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, occurredAt, correlationId, status, itemName, false, null, null,
                Map.of());
    }

    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId, OutboxEventType eventType, Long itemId, Long restaurantChainId,
            Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId, MenuItemStatus status,
            String itemName, Boolean scheduleChanged, Long scheduleVersion) {
        this(eventId, originalEventId, eventType, itemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, occurredAt, correlationId, status, itemName,
                scheduleChanged, scheduleVersion, null, Map.of());
    }

    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId, OutboxEventType eventType, Long itemId, Long restaurantChainId,
            Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId, MenuItemStatus status,
            String itemName, Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows) {
        this(eventId, originalEventId, eventType, itemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, occurredAt, correlationId, status, itemName,
                scheduleChanged, scheduleVersion, scheduleTimezone, scheduleWindows, Map.of(), Map.of());
    }

    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId, OutboxEventType eventType, Long itemId, Long restaurantChainId,
            Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId, MenuItemStatus status,
            String itemName, Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows,
            Map<String, List<EventScheduleWindow>> baseScheduleWindows,
            Map<String, List<EventScheduleWindow>> outletScheduleWindows) {
        this(eventId, originalEventId, eventType, itemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, occurredAt, correlationId, status, itemName, scheduleChanged,
                scheduleVersion, scheduleTimezone, scheduleWindows, baseScheduleWindows, outletScheduleWindows,
                false, null, null, null, null);
    }

    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId, OutboxEventType eventType, Long itemId, Long restaurantChainId,
            Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId, MenuItemStatus status,
            String itemName, Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows,
            Map<String, List<EventScheduleWindow>> baseScheduleWindows,
            Map<String, List<EventScheduleWindow>> outletScheduleWindows,
            Boolean seasonalAvailabilityChanged, Long seasonalAvailabilityVersion,
            LocalDate seasonalStartDate, LocalDate seasonalEndDate, String seasonalTimezone) {
        this(eventId, originalEventId, eventType, itemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, occurredAt, correlationId, status, itemName, scheduleChanged,
                scheduleVersion, scheduleTimezone, scheduleWindows, baseScheduleWindows, outletScheduleWindows,
                seasonalAvailabilityChanged, seasonalAvailabilityVersion, seasonalStartDate, seasonalEndDate,
                seasonalTimezone, false, List.of());
    }

    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId, OutboxEventType eventType, Long itemId, Long restaurantChainId,
            Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId, MenuItemStatus status,
            String itemName, Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows,
            Map<String, List<EventScheduleWindow>> baseScheduleWindows,
            Map<String, List<EventScheduleWindow>> outletScheduleWindows,
            Boolean seasonalAvailabilityChanged, Long seasonalAvailabilityVersion,
            LocalDate seasonalStartDate, LocalDate seasonalEndDate, String seasonalTimezone,
            Boolean modifierConfigurationChanged, List<EventModifierGroup> modifierGroups) {
        this(eventId, originalEventId, eventType, itemId, restaurantChainId, outletId, aggregateVersion,
                price, availability, visibility, occurredAt, correlationId, status, itemName,
                scheduleChanged, scheduleVersion, scheduleTimezone, scheduleWindows, baseScheduleWindows,
                outletScheduleWindows, seasonalAvailabilityChanged, seasonalAvailabilityVersion,
                seasonalStartDate, seasonalEndDate, seasonalTimezone, modifierConfigurationChanged,
                modifierGroups, false, null);
    }

    public MenuItemChangedEvent(UUID eventId,
            UUID originalEventId, OutboxEventType eventType, Long itemId, Long restaurantChainId,
            Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId, MenuItemStatus status,
            String itemName, Boolean scheduleChanged, Long scheduleVersion, String scheduleTimezone,
            Map<String, List<EventScheduleWindow>> scheduleWindows,
            Map<String, List<EventScheduleWindow>> baseScheduleWindows,
            Map<String, List<EventScheduleWindow>> outletScheduleWindows,
            Boolean seasonalAvailabilityChanged, Long seasonalAvailabilityVersion,
            LocalDate seasonalStartDate, LocalDate seasonalEndDate, String seasonalTimezone,
            Boolean modifierConfigurationChanged, List<EventModifierGroup> modifierGroups,
            Boolean bundleConfigurationChanged, EventBundleSnapshot bundleSnapshot) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event Id cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("Item Id must be positive");
        }
        if (restaurantChainId == null || restaurantChainId <= 0) {
            throw new IllegalArgumentException("Restaurant Chain Id must be positive");
        }
        if (aggregateVersion == null || aggregateVersion < 0) {
            throw new IllegalArgumentException("Aggregate version must be non-negative");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at cannot be null");
        }
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (price == null && availability == null && visibility == null && status == null
                && !Boolean.TRUE.equals(scheduleChanged)
                && !Boolean.TRUE.equals(seasonalAvailabilityChanged)
                && !Boolean.TRUE.equals(modifierConfigurationChanged)
                && !Boolean.TRUE.equals(bundleConfigurationChanged)) {
            throw new IllegalArgumentException("At least one changed field is required");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (itemName != null && itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank when supplied");
        }
        if (Boolean.TRUE.equals(scheduleChanged) && (scheduleVersion == null || scheduleVersion < 0)) {
            throw new IllegalArgumentException("Schedule version must be non-negative when schedule changed");
        }
        if (Boolean.TRUE.equals(scheduleChanged) && (scheduleTimezone == null || scheduleTimezone.isBlank())) {
            throw new IllegalArgumentException("Schedule timezone is required when schedule changed");
        }
        if (Boolean.TRUE.equals(seasonalAvailabilityChanged)
                && (seasonalAvailabilityVersion == null || seasonalAvailabilityVersion < 0)) {
            throw new IllegalArgumentException("Seasonal availability version must be non-negative when changed");
        }
        if (Boolean.TRUE.equals(seasonalAvailabilityChanged)
                && (seasonalTimezone == null || seasonalTimezone.isBlank())) {
            throw new IllegalArgumentException("Seasonal availability timezone is required when changed");
        }
        if ((seasonalStartDate == null) != (seasonalEndDate == null)
                || (seasonalStartDate != null && !seasonalStartDate.isBefore(seasonalEndDate))) {
            throw new IllegalArgumentException("Seasonal availability dates must be an increasing complete range");
        }
        if (Boolean.TRUE.equals(modifierConfigurationChanged) && modifierGroups == null) {
            throw new IllegalArgumentException("Modifier groups are required when modifier configuration changed");
        }
        if (Boolean.TRUE.equals(modifierConfigurationChanged)
                && eventType != OutboxEventType.MODIFIER_CONFIGURATION_CHANGED) {
            throw new IllegalArgumentException(
                    "Modifier configuration changes must use the modifier configuration event type");
        }
        if (eventType == OutboxEventType.MODIFIER_CONFIGURATION_CHANGED
                && !Boolean.TRUE.equals(modifierConfigurationChanged)) {
            throw new IllegalArgumentException(
                    "Modifier configuration event type requires a modifier configuration snapshot");
        }
        if (Boolean.TRUE.equals(modifierConfigurationChanged)) {
            validateModifierGroupDisplayOrders(modifierGroups);
        }
        if (Boolean.TRUE.equals(bundleConfigurationChanged) && bundleSnapshot == null) {
            throw new IllegalArgumentException("Bundle snapshot is required when bundle configuration changed");
        }
        if (!Boolean.TRUE.equals(bundleConfigurationChanged) && bundleSnapshot != null) {
            throw new IllegalArgumentException("Bundle snapshot must be absent when bundle configuration did not change");
        }
        if (bundleSnapshot != null && !itemId.equals(bundleSnapshot.menuItemId())) {
            throw new IllegalArgumentException("Bundle snapshot must belong to the changed menu item");
        }
        if (Boolean.TRUE.equals(bundleConfigurationChanged)
                && eventType != OutboxEventType.BUNDLE_CONFIGURATION_CHANGED) {
            throw new IllegalArgumentException("Bundle configuration changes must use the bundle configuration event type");
        }
        if (eventType == OutboxEventType.BUNDLE_CONFIGURATION_CHANGED
                && !Boolean.TRUE.equals(bundleConfigurationChanged)) {
            throw new IllegalArgumentException("Bundle configuration event type requires a bundle snapshot");
        }
        this.eventId = eventId;
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.itemId = itemId;
        this.restaurantChainId = restaurantChainId;
        this.outletId = outletId;
        this.aggregateVersion = aggregateVersion;
        this.price = price;
        this.availability = availability;
        this.visibility = visibility;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.status = status;
        this.itemName = itemName;
        this.scheduleChanged = scheduleChanged;
        this.scheduleVersion = scheduleVersion;
        this.scheduleId = Boolean.TRUE.equals(scheduleChanged)
                ? itemId + ":" + (outletId == null ? "base" : outletId)
                : null;
        this.scheduleTimezone = scheduleTimezone;
        this.scheduleWindows = scheduleWindows == null ? Map.of() : Map.copyOf(scheduleWindows);
        this.baseScheduleWindows = baseScheduleWindows == null ? Map.of() : Map.copyOf(baseScheduleWindows);
        this.outletScheduleWindows = outletScheduleWindows == null ? Map.of() : Map.copyOf(outletScheduleWindows);
        this.seasonalAvailabilityChanged = seasonalAvailabilityChanged;
        this.seasonalAvailabilityVersion = seasonalAvailabilityVersion;
        this.seasonalStartDate = seasonalStartDate;
        this.seasonalEndDate = seasonalEndDate;
        this.seasonalTimezone = seasonalTimezone;
        this.modifierConfigurationChanged = Boolean.TRUE.equals(modifierConfigurationChanged);
        this.modifierGroups = modifierGroups == null ? List.of() : List.copyOf(modifierGroups);
        this.bundleConfigurationChanged = Boolean.TRUE.equals(bundleConfigurationChanged);
        this.bundleSnapshot = bundleSnapshot;
    }

    private static void validateModifierGroupDisplayOrders(List<EventModifierGroup> modifierGroups) {
        Set<Integer> displayOrders = new HashSet<>();
        for (EventModifierGroup modifierGroup : modifierGroups) {
            if (modifierGroup == null) {
                throw new IllegalArgumentException("Modifier groups cannot contain null values");
            }
            if (!displayOrders.add(modifierGroup.displayOrder())) {
                throw new IllegalArgumentException("Modifier group display orders must be unique");
            }
        }
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOriginalEventId() {
        return originalEventId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getRestaurantChainId() {
        return restaurantChainId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public Boolean getVisibility() {
        return visibility;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public MenuItemStatus getStatus() {
        return status;
    }

    public String getItemName() {
        return itemName;
    }

    public Boolean getScheduleChanged() {
        return scheduleChanged;
    }

    public Long getScheduleVersion() {
        return scheduleVersion;
    }

    /**
     * Stable schedule scope identifier: one base or one outlet override per item.
     */
    public String getScheduleId() {
        return scheduleId;
    }

    public String getScheduleTimezone() {
        return scheduleTimezone;
    }

    public Map<String, List<EventScheduleWindow>> getScheduleWindows() {
        return scheduleWindows;
    }

    public Map<String, List<EventScheduleWindow>> getBaseScheduleWindows() {
        return baseScheduleWindows;
    }

    public Map<String, List<EventScheduleWindow>> getOutletScheduleWindows() {
        return outletScheduleWindows;
    }

    public Boolean getSeasonalAvailabilityChanged() {
        return seasonalAvailabilityChanged;
    }

    public Long getSeasonalAvailabilityVersion() {
        return seasonalAvailabilityVersion;
    }

    public LocalDate getSeasonalStartDate() {
        return seasonalStartDate;
    }

    public LocalDate getSeasonalEndDate() {
        return seasonalEndDate;
    }

    public String getSeasonalTimezone() {
        return seasonalTimezone;
    }

    /** True means modifierGroups is the complete replacement configuration for this item. */
    public Boolean getModifierConfigurationChanged() {
        return modifierConfigurationChanged;
    }

    /** Complete replacement snapshot; an empty list intentionally removes all groups. */
    public List<EventModifierGroup> getModifierGroups() {
        return modifierGroups;
    }

    /** True means bundleSnapshot is the complete replacement structure for this bundle. */
    public Boolean getBundleConfigurationChanged() {
        return bundleConfigurationChanged;
    }

    /** Complete nested replacement snapshot for the root bundle; null when a bundle did not change. */
    public EventBundleSnapshot getBundleSnapshot() {
        return bundleSnapshot;
    }

}
