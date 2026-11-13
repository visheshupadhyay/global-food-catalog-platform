package com.vishesh.foodcatalog.catalog.event.application;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.vishesh.foodcatalog.catalog.event.domain.*;

/** Creates a pending outbox row from a serialized Catalog event. */
@Component
public class OutboxEventFactory {
    public OutboxEvent create(MenuItemChangedEvent event, String payload) {
        return new OutboxEvent(null, event.getEventId(), event.getOriginalEventId(),
                event.getEventType(), String.valueOf(event.getItemId()), payload,
                OutboxEventStatus.PENDING, 0L, null, null, Instant.now(), null, null,
                event.getItemId(), event.getAggregateVersion());
    }
}
