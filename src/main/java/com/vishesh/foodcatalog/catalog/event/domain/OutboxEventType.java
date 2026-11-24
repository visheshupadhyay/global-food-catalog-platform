package com.vishesh.foodcatalog.catalog.event.domain;

/** Names the kind of business change carried by an outbox event. */
public enum OutboxEventType {
    /** A complete menu snapshot used to create an outlet's first read model. */
    OUTLET_MENU_INITIALIZED,
    /** A complete first snapshot emitted when a draft item becomes active. */
    MENU_ITEM_ACTIVATED,
    MENU_ITEM_CHANGED,
    SCHEDULE_CHANGED,
    SEASONAL_AVAILABILITY_CHANGED,
    MODIFIER_CONFIGURATION_CHANGED,
    BUNDLE_CONFIGURATION_CHANGED
}
