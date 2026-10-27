package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable message describing the new state of a changed menu item. */
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

    public MenuItemChangedEvent(UUID eventId, UUID originalEventId, OutboxEventType eventType, Long itemId,
            Long restaurantChainId, Long outletId, Long aggregateVersion, BigDecimal price, Boolean availability,
            Boolean visibility, Instant occurredAt, String correlationId) {
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
        if (price == null && availability == null && visibility == null) {
            throw new IllegalArgumentException("At least one changed field is required");
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
    }

    public UUID getEventId() { return eventId; }
    public UUID getOriginalEventId() { return originalEventId; }
    public OutboxEventType getEventType() { return eventType; }
    public Long getItemId() { return itemId; }
    public Long getRestaurantChainId() { return restaurantChainId; }
    public Long getOutletId() { return outletId; }
    public Long getAggregateVersion() { return aggregateVersion; }
    public BigDecimal getPrice() { return price; }
    public Boolean getAvailability() { return availability; }
    public Boolean getVisibility() { return visibility; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getCorrelationId() { return correlationId; }

}
