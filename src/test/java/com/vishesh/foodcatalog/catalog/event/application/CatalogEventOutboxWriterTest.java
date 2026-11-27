package com.vishesh.foodcatalog.catalog.event.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;

import tools.jackson.databind.ObjectMapper;

/** Verifies the shared technical boundary used by every Catalog write flow. */
class CatalogEventOutboxWriterTest {

	@Test
	void save_menuItemEvent_serializesThenPersistsItsOutboxRow() throws Exception {
		OutboxEventFactory outboxFactory = mock(OutboxEventFactory.class);
		OutboxEventRepository outboxRepository = mock(OutboxEventRepository.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		MenuItemChangedEvent event = mock(MenuItemChangedEvent.class);
		OutboxEvent outboxEvent = mock(OutboxEvent.class);

		when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventType\":\"MENU_ITEM_CHANGED\"}");
		when(outboxFactory.create(event, "{\"eventType\":\"MENU_ITEM_CHANGED\"}")).thenReturn(outboxEvent);

		new CatalogEventOutboxWriter(outboxFactory, outboxRepository, objectMapper).save(event);

		verify(outboxFactory).create(event, "{\"eventType\":\"MENU_ITEM_CHANGED\"}");
		verify(outboxRepository).save(outboxEvent);
	}

	@Test
	void save_outletInitializationEvent_usesItsDedicatedOutboxFactoryMethod() throws Exception {
		OutboxEventFactory outboxFactory = mock(OutboxEventFactory.class);
		OutboxEventRepository outboxRepository = mock(OutboxEventRepository.class);
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		OutletMenuInitializedEvent event = mock(OutletMenuInitializedEvent.class);
		OutboxEvent outboxEvent = mock(OutboxEvent.class);

		when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventType\":\"OUTLET_MENU_INITIALIZED\"}");
		when(outboxFactory.create(eq(event), anyString())).thenReturn(outboxEvent);

		new CatalogEventOutboxWriter(outboxFactory, outboxRepository, objectMapper).save(event);

		verify(outboxFactory).create(event, "{\"eventType\":\"OUTLET_MENU_INITIALIZED\"}");
		verify(outboxRepository).save(outboxEvent);
	}
}
