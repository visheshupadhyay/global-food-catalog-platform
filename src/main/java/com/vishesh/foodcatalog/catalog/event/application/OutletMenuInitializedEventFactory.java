package com.vishesh.foodcatalog.catalog.event.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuItemSnapshot;

/**
 * Creates the one complete event emitted when a draft outlet becomes active.
 */
@Component
public class OutletMenuInitializedEventFactory {

	public OutletMenuInitializedEvent create(Long outletId, Long restaurantChainId, Long menuGroupId,
			Long outletVersion, String outletTimezone, String correlationId, List<OutletMenuItemSnapshot> menuItems) {
		return new OutletMenuInitializedEvent(UUID.randomUUID(), outletId, restaurantChainId, menuGroupId,
				outletVersion, outletTimezone, Instant.now(), correlationId, menuItems);
	}
}
