package com.vishesh.foodcatalog.catalog.event.domain;

/** Tells us where an outbox event is in its delivery journey. */
/** Delivery state used by the outbox worker and operator tools. */
public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DLQ
}
