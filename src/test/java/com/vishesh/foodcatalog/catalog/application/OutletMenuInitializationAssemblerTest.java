package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.Outlet;
import com.vishesh.foodcatalog.catalog.domain.ScheduleWindow;
import com.vishesh.foodcatalog.catalog.domain.Weekday;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuDataProvider;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuResolver;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierGroup;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierOption;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemScheduleRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemSeasonalAvailabilityRepository;

@ExtendWith(MockitoExtension.class)
class OutletMenuInitializationAssemblerTest {
	@Mock
	EffectiveMenuDataProvider sourceData;
	@Mock
	EffectiveMenuResolver resolver;
	@Mock
	MenuItemRepository menuItems;
	@Mock
	MenuItemScheduleRepository schedules;
	@Mock
	MenuItemSeasonalAvailabilityRepository seasonalAvailability;
	@Mock
	ModifierConfigurationSnapshotAssembler modifierSnapshots;
	@Mock
	BundleSnapshotAssembler bundleSnapshots;

	@Test
	void assemble_usesExistingSparseOverrideResolverAndKeepsDraftItemsInternally() {
		OutletMenuInitializationAssembler assembler = new OutletMenuInitializationAssembler(sourceData, resolver,
				menuItems, schedules, seasonalAvailability, modifierSnapshots, bundleSnapshots);
		Outlet outlet = outlet();
		MenuGroup menuGroup = activeMenuGroup();
		BaseMenuItemSnapshot burger = new BaseMenuItemSnapshot(101L, "Burger", BigDecimal.valueOf(199), true, true,
				MenuItemStatus.DRAFT);
		OutletOverrideSnapshot override = new OutletOverrideSnapshot(101L, 30L, BigDecimal.valueOf(219), false, null);

		when(sourceData.findByOutletId(30L)).thenReturn(Optional.of(
				new EffectiveMenuSourceData(30L, ChainStatus.DRAFT, List.of(burger), List.of(override), List.of())));
		when(menuItems.findByIdAndRestaurantChainId(101L, 10L)).thenReturn(Optional.of(menuItem()));
		when(resolver.resolve(burger, 30L, override))
				.thenReturn(new EffectiveMenuItem(101L, 30L, "Burger", BigDecimal.valueOf(219), false, true));
		when(schedules.find(101L, null)).thenReturn(Optional.empty());
		when(schedules.find(101L, 30L)).thenReturn(Optional.empty());
		when(seasonalAvailability.find(101L, null)).thenReturn(Optional.empty());
		when(seasonalAvailability.find(101L, 30L)).thenReturn(Optional.empty());
		when(modifierSnapshots.assemble(101L)).thenReturn(List.of());

		var snapshots = assembler.assemble(outlet, menuGroup);

		assertEquals(1, snapshots.size());
		assertEquals(MenuItemStatus.DRAFT, snapshots.getFirst().status());
		assertEquals(BigDecimal.valueOf(219), snapshots.getFirst().price());
		assertEquals(false, snapshots.getFirst().availability());
		assertEquals(true, snapshots.getFirst().visibility());
	}

	@Test
	void assemble_includesSchedulesSeasonModifiersAndBundleSnapshot() {
		OutletMenuInitializationAssembler assembler = new OutletMenuInitializationAssembler(sourceData, resolver,
				menuItems, schedules, seasonalAvailability, modifierSnapshots, bundleSnapshots);
		BaseMenuItemSnapshot meal = new BaseMenuItemSnapshot(101L, "Burger Meal", BigDecimal.valueOf(299), true, true,
				MenuItemStatus.ACTIVE);
		MenuItemSchedule baseSchedule = new MenuItemSchedule(101L, null,
				Map.of(Weekday.MONDAY, List.of(new ScheduleWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)))));
		MenuItemSchedule outletSchedule = new MenuItemSchedule(101L, 30L,
				Map.of(Weekday.TUESDAY, List.of(new ScheduleWindow(LocalTime.of(14, 0), LocalTime.of(18, 0)))));
		MenuItemSeasonalAvailability baseSeason = new MenuItemSeasonalAvailability(101L, null,
				LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1), 0L);
		MenuItemSeasonalAvailability outletSeason = new MenuItemSeasonalAvailability(101L, 30L,
				LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20), 0L);
		EventModifierGroup modifierGroup = new EventModifierGroup(5L, "Choose extras", 0, 1, 0,
				List.of(new EventModifierOption(7L, "Cheese", BigDecimal.valueOf(20), 0)));
		EventBundleSnapshot bundle = new EventBundleSnapshot(101L, "Burger Meal", BigDecimal.valueOf(299), List.of());

		when(sourceData.findByOutletId(30L)).thenReturn(
				Optional.of(new EffectiveMenuSourceData(30L, ChainStatus.DRAFT, List.of(meal), List.of(), List.of())));
		when(menuItems.findByIdAndRestaurantChainId(101L, 10L)).thenReturn(Optional.of(bundleMenuItem()));
		when(resolver.resolve(meal, 30L, null))
				.thenReturn(new EffectiveMenuItem(101L, 30L, "Burger Meal", BigDecimal.valueOf(299), true, true));
		when(schedules.find(101L, null)).thenReturn(Optional.of(baseSchedule));
		when(schedules.find(101L, 30L)).thenReturn(Optional.of(outletSchedule));
		when(seasonalAvailability.find(101L, null)).thenReturn(Optional.of(baseSeason));
		when(seasonalAvailability.find(101L, 30L)).thenReturn(Optional.of(outletSeason));
		when(modifierSnapshots.assemble(101L)).thenReturn(List.of(modifierGroup));
		when(bundleSnapshots.assemble(101L, 10L)).thenReturn(bundle);

		var snapshot = assembler.assemble(outlet(), activeMenuGroup()).getFirst();

		assertEquals(LocalTime.of(9, 0), snapshot.baseScheduleWindows().get("MONDAY").getFirst().startTime());
		assertEquals(LocalTime.of(14, 0), snapshot.outletScheduleWindows().get("TUESDAY").getFirst().startTime());
		assertEquals(8L, snapshot.menuItemVersion());
		assertEquals(LocalDate.of(2026, 11, 10), snapshot.seasonalStartDate());
		assertEquals(LocalDate.of(2026, 11, 20), snapshot.seasonalEndDate());
		assertEquals(5L, snapshot.modifierGroups().getFirst().groupId());
		assertEquals(bundle, snapshot.bundleSnapshot());
	}

	private static Outlet outlet() {
		Instant now = Instant.parse("2026-11-23T10:00:00Z");
		return new Outlet(30L, 10L, "Indiranagar", "BLR-IND-01", Currency.getInstance("INR"), ChainStatus.DRAFT, now,
				now, "Asia/Kolkata", 0L);
	}

	private static MenuGroup activeMenuGroup() {
		Instant now = Instant.parse("2026-11-23T10:00:00Z");
		return new MenuGroup(10L, "Bangalore Menu", ChainStatus.ACTIVE, now, now, 2L, 20L);
	}

	private static MenuItem menuItem() {
		Instant now = Instant.parse("2026-11-23T10:00:00Z");
		return new MenuItem(101L, "Burger", BigDecimal.valueOf(199), now, 20L, null, null, "BURGER",
				MenuItemStatus.DRAFT, true, true, 1L, 10L, now, MenuItemKind.PRODUCT);
	}

	private static MenuItem bundleMenuItem() {
		Instant now = Instant.parse("2026-11-23T10:00:00Z");
		return new MenuItem(101L, "Burger Meal", BigDecimal.valueOf(299), now, 20L, null, null, "BURGER-MEAL",
				MenuItemStatus.ACTIVE, true, true, 8L, 10L, now, MenuItemKind.BUNDLE);
	}
}
