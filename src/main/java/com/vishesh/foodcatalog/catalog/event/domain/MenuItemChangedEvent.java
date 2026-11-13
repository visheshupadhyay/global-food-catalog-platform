package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Map;

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
                price, availability, visibility, occurredAt, correlationId, status, itemName, false, null, null, Map.of());
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
                && !Boolean.TRUE.equals(scheduleChanged)) {
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

    /** Stable schedule scope identifier: one base or one outlet override per item. */
    public String getScheduleId() { return scheduleId; }

    public String getScheduleTimezone() { return scheduleTimezone; }

    public Map<String, List<EventScheduleWindow>> getScheduleWindows() { return scheduleWindows; }
    public Map<String, List<EventScheduleWindow>> getBaseScheduleWindows() { return baseScheduleWindows; }
    public Map<String, List<EventScheduleWindow>> getOutletScheduleWindows() { return outletScheduleWindows; }

}
