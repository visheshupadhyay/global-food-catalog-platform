package com.vishesh.foodcatalog.catalog.event.application;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;

/** Sends one saved outbox event to the outside messaging system. */
public interface OutboxEventPublisher {
    /** Publishes the event, or throws when delivery fails. */
    void publish(OutboxEvent event);
}
