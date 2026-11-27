package com.vishesh.foodcatalog.catalog.event.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;

/** Creates a pending outbox row from a serialized Catalog event. */
@Component
public class OutboxEventFactory {
	public OutboxEvent create(MenuItemChangedEvent event, String payload) {
		return new OutboxEvent(null, event.getEventId(), event.getOriginalEventId(),
				// Events are applied to one outlet snapshot. Using that outlet as
				// the Kafka key keeps its initialization and later item changes
				// in the same Kafka partition, preserving their order.
				event.getEventType(), String.valueOf(event.getOutletId()), payload, OutboxEventStatus.PENDING, 0L, null,
				null, Instant.now(), null, null, event.getItemId(), event.getAggregateVersion());
	}

	/**
	 * Creates the durable delivery row for one complete outlet onboarding event.
	 */
	public OutboxEvent create(OutletMenuInitializedEvent event, String payload) {
		return new OutboxEvent(null, event.getEventId(), null, event.getEventType(),
				String.valueOf(event.getOutletId()), payload, OutboxEventStatus.PENDING, 0L, null, null, Instant.now(),
				null, null, event.getOutletId(), event.getOutletVersion());
	}
}
