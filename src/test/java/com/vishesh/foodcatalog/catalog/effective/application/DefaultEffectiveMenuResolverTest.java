package com.vishesh.foodcatalog.catalog.effective.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;

public class DefaultEffectiveMenuResolverTest {

	private static final Long MENU_ITEM_ID = 101L;
	private static final Long BERLIN_OUTLET_ID = 501L;

	private final DefaultEffectiveMenuResolver resolver = new DefaultEffectiveMenuResolver();

	@Test
	void resolve_withoutOverride_usesAllBaseValues() {
		BaseMenuItemSnapshot baseItem = baseItem();

		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, null);

		assertEquals(MENU_ITEM_ID, effectiveItem.getMenuItemId());
		assertEquals(BERLIN_OUTLET_ID, effectiveItem.getOutletId());
		assertEquals("Classic Burger", effectiveItem.getItemName());
		assertEquals(new BigDecimal("10.00"), effectiveItem.getPrice());
		assertTrue(effectiveItem.isAvailable());
		assertTrue(effectiveItem.isVisible());
	}

	@Test
	void resolve_withPriceOverride_changesOnlyPrice() {
		BaseMenuItemSnapshot baseItem = baseItem();
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, new BigDecimal("12.50"), null, null);

		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, override);

		assertEquals(new BigDecimal("12.50"), effectiveItem.getPrice());
		assertTrue(effectiveItem.isAvailable());
		assertTrue(effectiveItem.isVisible());
	}

	@Test
	void resolve_withAvailabilityOverride_changesOnlyAvailability() {
		BaseMenuItemSnapshot baseItem = baseItem();
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, null, false, null);

		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, override);

		assertEquals(new BigDecimal("10.00"), effectiveItem.getPrice());
		assertFalse(effectiveItem.isAvailable());
		assertTrue(effectiveItem.isVisible());
	}

	@Test
	void resolve_withVisibilityOverride_changesOnlyVisibility() {
		BaseMenuItemSnapshot baseItem = baseItem();
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, null, null, false);

		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, override);

		assertEquals(new BigDecimal("10.00"), effectiveItem.getPrice());
		assertTrue(effectiveItem.isAvailable());
		assertFalse(effectiveItem.isVisible());
	}

	@Test
	void resolve_withMultipleOverrides_appliesEachProvidedValue() {
		BaseMenuItemSnapshot baseItem = baseItem();
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, new BigDecimal("11.75"), false, false);

		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, override);

		assertEquals(new BigDecimal("11.75"), effectiveItem.getPrice());
		assertFalse(effectiveItem.isAvailable());
		assertFalse(effectiveItem.isVisible());
	}

	@Test
	void resolve_withEmptyOverride_inheritsAllBaseValues() {
		BaseMenuItemSnapshot baseItem = baseItem();
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, null, null, null);

		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, override);

		assertEquals(new BigDecimal("10.00"), effectiveItem.getPrice());
		assertTrue(effectiveItem.isAvailable());
		assertTrue(effectiveItem.isVisible());
	}

	@Test
	void resolve_whenBaseItemIsHidden_visibilityOverrideCannotMakeItVisible() {
		BaseMenuItemSnapshot hiddenBaseItem = new BaseMenuItemSnapshot(
				MENU_ITEM_ID,
				"Classic Burger",
				new BigDecimal("10.00"),
				true,
				false,
				MenuItemStatus.ACTIVE);
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, null, null, true);

		EffectiveMenuItem effectiveItem = resolver.resolve(hiddenBaseItem, BERLIN_OUTLET_ID, override);

		assertFalse(effectiveItem.isVisible());
	}

	@Test
	void resolve_whenOverrideReferencesAnotherItem_rejectsRequest() {
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				999L, BERLIN_OUTLET_ID, new BigDecimal("12.50"), null, null);

		assertThrows(IllegalArgumentException.class,
				() -> resolver.resolve(baseItem(), BERLIN_OUTLET_ID, override));
	}

	@Test
	void resolve_whenOverrideReferencesAnotherOutlet_rejectsRequest() {
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, 999L, new BigDecimal("12.50"), null, null);

		assertThrows(IllegalArgumentException.class,
				() -> resolver.resolve(baseItem(), BERLIN_OUTLET_ID, override));
	}

	@Test 
	void resolve_afterOverrideIsRemoved_returnsBaseValue(){
		BaseMenuItemSnapshot baseItem = baseItem();
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(
				MENU_ITEM_ID, BERLIN_OUTLET_ID, new BigDecimal("12.50"), null, null);
		EffectiveMenuItem effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, override);
		assertEquals(override.getPriceOverride(), effectiveItem.getPrice());

		effectiveItem = resolver.resolve(baseItem, BERLIN_OUTLET_ID, null);

		assertEquals(baseItem.getPrice(), effectiveItem.getPrice());
	}

	private BaseMenuItemSnapshot baseItem() {
		return new BaseMenuItemSnapshot(
				MENU_ITEM_ID,
				"Classic Burger",
				new BigDecimal("10.00"),
				true,
				true,
				MenuItemStatus.ACTIVE);
	}
}
