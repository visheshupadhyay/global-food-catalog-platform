package com.vishesh.foodcatalog.catalog.event.domain;

/** Tells us where an outbox event is in its delivery journey. */
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DLQ
}
