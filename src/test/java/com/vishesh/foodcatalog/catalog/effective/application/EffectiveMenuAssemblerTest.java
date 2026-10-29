package com.vishesh.foodcatalog.catalog.effective.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;

class EffectiveMenuAssemblerTest {

	private static final Long BERLIN_OUTLET_ID = 501L;
	private static final Long MUNICH_OUTLET_ID = 502L;

	private final EffectiveMenuAssembler assembler = new EffectiveMenuAssembler(new DefaultEffectiveMenuResolver());

	@Test
	void assemble_includesActiveVisibleBaseItemWithoutOverride() {
		List<EffectiveMenuItem> result = assemble(List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(), List.of());

		assertEquals(1, result.size());
		assertEquals(new BigDecimal("10.00"), result.getFirst().getPrice());
	}

	@Test
	void assemble_appliesMatchingOutletOverrideToBaseItem() {
		List<EffectiveMenuItem> result = assemble(
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(new OutletOverrideSnapshot(101L, BERLIN_OUTLET_ID, new BigDecimal("12.50"), false, null)),
				List.of());

		assertEquals(new BigDecimal("12.50"), result.getFirst().getPrice());
		assertFalse(result.getFirst().isAvailable());
	}

	@Test
	void assemble_excludesNonActiveBaseItems() {
		List<EffectiveMenuItem> result = assemble(
				List.of(baseItem(101L, "Draft", true, true, MenuItemStatus.DRAFT),
						baseItem(102L, "Archived", true, true, MenuItemStatus.ARCHIVED)),
				List.of(), List.of());

		assertTrue(result.isEmpty());
	}

	@Test
	void assemble_excludesHiddenEffectiveItems() {
		List<EffectiveMenuItem> result = assemble(
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(new OutletOverrideSnapshot(101L, BERLIN_OUTLET_ID, null, null, false)), List.of());

		assertTrue(result.isEmpty());
	}

	@Test
	void assemble_keepsVisibleUnavailableItems() {
		List<EffectiveMenuItem> result = assemble(
				List.of(baseItem(101L, "Burger", false, true, MenuItemStatus.ACTIVE)), List.of(), List.of());

		assertEquals(1, result.size());
		assertFalse(result.getFirst().isAvailable());
	}

	@Test
	void assemble_includesActiveVisibleOutletLocalItem() {
		List<EffectiveMenuItem> result = assemble(List.of(), List.of(),
				List.of(localItem(201L, BERLIN_OUTLET_ID, "Berlin Special", true, true, MenuItemStatus.ACTIVE)));

		assertEquals("Berlin Special", result.getFirst().getItemName());
		assertEquals(BERLIN_OUTLET_ID, result.getFirst().getOutletId());
	}

	@Test
	void assemble_forBerlin_includesOnlyBerlinLocalItems() {
		List<EffectiveMenuItem> result = assembler.assemble(
				BERLIN_OUTLET_ID,
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(),
				List.of(localItem(201L, BERLIN_OUTLET_ID, "Berlin Special", true, true, MenuItemStatus.ACTIVE)));

		assertEquals(List.of("Berlin Special", "Burger"),
				result.stream().map(EffectiveMenuItem::getItemName).toList());
		assertTrue(result.stream().anyMatch(item -> item.getMenuItemId().equals(201L)));
	}

	@Test
	void assemble_excludesHiddenOrNonActiveOutletLocalItems() {
		List<EffectiveMenuItem> result = assemble(List.of(), List.of(),
				List.of(localItem(201L, BERLIN_OUTLET_ID, "Hidden", true, false, MenuItemStatus.ACTIVE),
						localItem(202L, BERLIN_OUTLET_ID, "Draft", true, true, MenuItemStatus.DRAFT)));

		assertTrue(result.isEmpty());
	}

	@Test
	void assemble_rejectsLocalItemForAnotherOutlet() {
		assertThrows(IllegalArgumentException.class, () -> assemble(List.of(), List.of(),
				List.of(localItem(201L, MUNICH_OUTLET_ID, "Munich Special", true, true, MenuItemStatus.ACTIVE))));
	}

	@Test
	void assemble_forMunich_rejectsBerlinLocalItemInsteadOfLeakingIt() {
		assertThrows(IllegalArgumentException.class, () -> assembler.assemble(
				MUNICH_OUTLET_ID,
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(),
				List.of(localItem(201L, BERLIN_OUTLET_ID, "Berlin Special", true, true, MenuItemStatus.ACTIVE))));
	}

	@Test
	void assemble_rejectsOverrideForAnotherOutlet() {
		assertThrows(IllegalArgumentException.class, () -> assemble(
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(new OutletOverrideSnapshot(101L, MUNICH_OUTLET_ID, null, null, null)), List.of()));
	}

	@Test
	void assemble_sortsCustomerMenuAlphabetically() {
		List<EffectiveMenuItem> result = assemble(
				List.of(baseItem(101L, "Fries", true, true, MenuItemStatus.ACTIVE),
						baseItem(102L, "burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(),
				List.of(localItem(201L, BERLIN_OUTLET_ID, "Cola", true, true, MenuItemStatus.ACTIVE)));

		assertEquals(List.of("burger", "Cola", "Fries"),
				result.stream().map(EffectiveMenuItem::getItemName).toList());
	}

	@Test
	void assemble_rejectsDuplicateOverridesForTheSameBaseItem() {
		assertThrows(IllegalArgumentException.class, () -> assemble(
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(new OutletOverrideSnapshot(101L, BERLIN_OUTLET_ID, new BigDecimal("11.00"), null, null),
						new OutletOverrideSnapshot(101L, BERLIN_OUTLET_ID, new BigDecimal("12.00"), null, null)),
				List.of()));
	}

	@Test
	void assemble_rejectsOverrideWithoutSuppliedBaseItem() {
		assertThrows(IllegalArgumentException.class, () -> assemble(
				List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
				List.of(new OutletOverrideSnapshot(999L, BERLIN_OUTLET_ID, new BigDecimal("12.50"), false, null)),
				List.of()));
	}

	@Test
	void assemble_afterOverrideIsRemoved_restoresBaseValue() {
		BaseMenuItemSnapshot baseItem = baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE);
		OutletOverrideSnapshot outletOverride = new OutletOverrideSnapshot(101L, BERLIN_OUTLET_ID,
				new BigDecimal("12.50"), null, null);
		List<EffectiveMenuItem> result = assemble(List.of(baseItem), List.of(outletOverride), List.of());

		assertEquals(1, result.size());
		assertEquals(outletOverride.getPriceOverride(), result.get(0).getPrice());

		result = assemble(List.of(baseItem), List.of(), List.of());
		assertEquals(1, result.size());
		assertEquals(baseItem.getPrice(), result.get(0).getPrice());

	}

	@Test
	void unavailableActiveItem_isIncludedButMarkedUnavailable() {
		BaseMenuItemSnapshot baseItem = baseItem(101L, "Burger", false, true, MenuItemStatus.ACTIVE);
		List<EffectiveMenuItem> result = assemble(List.of(baseItem), List.of(), List.of());
		assertEquals(1, result.size());
		assertEquals(baseItem.getMenuItemId(), result.get(0).getMenuItemId());
		assertFalse(result.get(0).isAvailable());
	}

	@Test
	void hiddenItem_isExcludedFromCustomerMenu() {
		BaseMenuItemSnapshot baseItem = baseItem(101L, "Burger", true, false, MenuItemStatus.ACTIVE);
		List<EffectiveMenuItem> result = assemble(List.of(baseItem), List.of(), List.of());
		assertEquals(0, result.size());
	}

	@Test
	void archivedItem_isExcludedFromCustomerMenu() {
		BaseMenuItemSnapshot baseItem = baseItem(101L, "Burger", true, true, MenuItemStatus.ARCHIVED);
		List<EffectiveMenuItem> result = assemble(List.of(baseItem), List.of(), List.of());
		assertEquals(0, result.size());
	}

	@Test
	void availableActiveItem_isShownAsOrderable() {
		BaseMenuItemSnapshot baseItem = baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE);
		List<EffectiveMenuItem> result = assemble(List.of(baseItem), List.of(), List.of());
		assertEquals(1, result.size());
		assertEquals(baseItem.getMenuItemId(), result.get(0).getMenuItemId());
		assertTrue(result.get(0).isAvailable());
	}

	private List<EffectiveMenuItem> assemble(
			List<BaseMenuItemSnapshot> baseItems,
			List<OutletOverrideSnapshot> overrides,
			List<OutletLocalMenuItemSnapshot> localItems) {
		return assembler.assemble(BERLIN_OUTLET_ID, baseItems, overrides, localItems);
	}

	private BaseMenuItemSnapshot baseItem(Long id, String name, boolean available, boolean visible,
			MenuItemStatus status) {
		return new BaseMenuItemSnapshot(id, name, new BigDecimal("10.00"), available, visible, status);
	}

	private OutletLocalMenuItemSnapshot localItem(
			Long id, Long outletId, String name, boolean available, boolean visible, MenuItemStatus status) {
		return new OutletLocalMenuItemSnapshot(id, outletId, name, new BigDecimal("9.00"), available, visible, status);
	}

}
