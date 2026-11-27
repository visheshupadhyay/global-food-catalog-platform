package com.vishesh.foodcatalog.catalog.event.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;

class MenuItemChangedEventTest {

	@Test
	void normalEvent_defaultsModifierConfigurationToUnchanged() {
		MenuItemChangedEvent event = normalEventBuilder().build();

		assertFalse(event.getModifierConfigurationChanged());
		assertEquals(List.of(), event.getModifierGroups());
	}

	@Test
	void modifierConfigurationEvent_allowsEmptySnapshotToRemoveAllGroups() {
		MenuItemChangedEvent event = modifierEvent(List.of());

		assertEquals(OutboxEventType.MODIFIER_CONFIGURATION_CHANGED, event.getEventType());
		assertEquals(Boolean.TRUE, event.getModifierConfigurationChanged());
		assertEquals(List.of(), event.getModifierGroups());
	}

	@Test
	void modifierConfigurationEvent_rejectsDuplicateGroupDisplayOrders() {
		EventModifierOption cheese = new EventModifierOption(101L, "Extra cheese", BigDecimal.valueOf(20), 0);
		List<EventModifierGroup> groups = List.of(
				new EventModifierGroup(11L, "Choose extras", 0, 1, 0, List.of(cheese)),
				new EventModifierGroup(12L, "Choose a drink", 0, 1, 0,
						List.of(new EventModifierOption(102L, "Cola", BigDecimal.valueOf(30), 0))));

		assertThrows(IllegalArgumentException.class, () -> modifierEvent(groups));
	}

	@Test
	void bundleConfigurationEvent_carriesCompleteNestedReplacementSnapshot() {
		EventBundleChild fries = new EventBundleChild(12L, "Fries", MenuItemKind.PRODUCT, 1, 1, MenuItemStatus.ACTIVE,
				true, true, List.of());
		EventBundleSnapshot meal = new EventBundleSnapshot(10L, "Burger Meal", BigDecimal.valueOf(299),
				List.of(new EventBundleChild(11L, "Burger", MenuItemKind.PRODUCT, 1, 0, MenuItemStatus.ACTIVE, true,
						true, List.of()), fries));

		MenuItemChangedEvent event = MenuItemChangedEvent.builder()
				.identity(UUID.randomUUID(), null, OutboxEventType.BUNDLE_CONFIGURATION_CHANGED)
				.target(10L, 1L, null, 2L).itemChange(null, null, null, MenuItemStatus.ACTIVE, "Burger Meal")
				.metadata(Instant.parse("2026-01-01T00:00:00Z"), "corr-1").bundle(true, meal).build();

		assertEquals(Boolean.TRUE, event.getBundleConfigurationChanged());
		assertEquals("Burger Meal", event.getBundleSnapshot().name());
		assertEquals(2, event.getBundleSnapshot().children().size());
	}

	@Test
	void normalEvent_defaultsBundleConfigurationToUnchanged() {
		MenuItemChangedEvent event = normalEventBuilder().build();

		assertFalse(event.getBundleConfigurationChanged());
		assertNull(event.getBundleSnapshot());
	}

	private MenuItemChangedEvent modifierEvent(List<EventModifierGroup> groups) {
		return MenuItemChangedEvent.builder()
				.identity(UUID.randomUUID(), null, OutboxEventType.MODIFIER_CONFIGURATION_CHANGED)
				.target(10L, 1L, null, 2L).itemChange(null, null, null, MenuItemStatus.ACTIVE, "Burger")
				.metadata(Instant.parse("2026-01-01T00:00:00Z"), "corr-1").modifiers(true, groups).build();
	}

	private MenuItemChangedEventBuilder normalEventBuilder() {
		return MenuItemChangedEvent.builder().identity(UUID.randomUUID(), null, OutboxEventType.MENU_ITEM_CHANGED)
				.target(10L, 1L, null, 2L).itemChange(BigDecimal.TEN, null, null, MenuItemStatus.ACTIVE, "Burger")
				.metadata(Instant.parse("2026-01-01T00:00:00Z"), "corr-1");
	}
}
