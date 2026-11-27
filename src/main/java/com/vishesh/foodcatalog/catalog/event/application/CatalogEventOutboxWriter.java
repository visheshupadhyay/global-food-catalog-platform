package com.vishesh.foodcatalog.catalog.event.application;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns a Catalog event into a durable pending outbox row.
 *
 * <p>
 * Application services decide <em>which</em> business event to create. This
 * class owns the repeated technical work of JSON serialization and persistence.
 * When it throws, the caller's transaction rolls back both the business change
 * and the outbox write.
 * </p>
 */
@Component
public class CatalogEventOutboxWriter {
	private final OutboxEventFactory outboxEventFactory;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	public CatalogEventOutboxWriter(OutboxEventFactory outboxEventFactory, OutboxEventRepository outboxEventRepository,
			ObjectMapper objectMapper) {
		this.outboxEventFactory = outboxEventFactory;
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
	}

	/** Saves one ordinary menu-item change for later Kafka delivery. */
	public void save(MenuItemChangedEvent event) {
		outboxEventRepository.save(outboxEventFactory.create(event, serialize(event)));
	}

	/** Saves one complete first-menu snapshot for a newly activated outlet. */
	public void save(OutletMenuInitializedEvent event) {
		outboxEventRepository.save(outboxEventFactory.create(event, serialize(event)));
	}

	private String serialize(Object event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize Catalog outbox event", exception);
		}
	}
}
