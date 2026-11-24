package com.vishesh.foodcatalog.catalog.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.Outlet;
import com.vishesh.foodcatalog.catalog.domain.ScheduleWindow;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuDataProvider;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuResolver;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.EventScheduleWindow;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemScheduleRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemSeasonalAvailabilityRepository;

/**
 * Builds the complete, internal item state needed when Effective Menu first
 * learns about an outlet. It reuses the existing sparse-override resolver and
 * the existing bundle/modifier assemblers instead of re-implementing them.
 */
@Component
public class OutletMenuInitializationAssembler {
    private final EffectiveMenuDataProvider sourceData;
    private final EffectiveMenuResolver effectiveMenuResolver;
    private final MenuItemRepository menuItems;
    private final MenuItemScheduleRepository schedules;
    private final MenuItemSeasonalAvailabilityRepository seasonalAvailability;
    private final ModifierConfigurationSnapshotAssembler modifierSnapshots;
    private final BundleSnapshotAssembler bundleSnapshots;

    public OutletMenuInitializationAssembler(
            @Qualifier("postgresEffectiveMenuDataProvider") EffectiveMenuDataProvider sourceData,
            @Qualifier("defaultEffectiveMenuResolver") EffectiveMenuResolver effectiveMenuResolver,
            MenuItemRepository menuItems,
            MenuItemScheduleRepository schedules,
            MenuItemSeasonalAvailabilityRepository seasonalAvailability,
            ModifierConfigurationSnapshotAssembler modifierSnapshots,
            BundleSnapshotAssembler bundleSnapshots) {
        this.sourceData = sourceData;
        this.effectiveMenuResolver = effectiveMenuResolver;
        this.menuItems = menuItems;
        this.schedules = schedules;
        this.seasonalAvailability = seasonalAvailability;
        this.modifierSnapshots = modifierSnapshots;
        this.bundleSnapshots = bundleSnapshots;
    }

    public List<OutletMenuItemSnapshot> assemble(Outlet outlet, MenuGroup menuGroup) {
        if (outlet == null || outlet.getId() == null) {
            throw new IllegalArgumentException("A stored outlet is required");
        }
        if (menuGroup == null || menuGroup.getId() == null) {
            throw new IllegalArgumentException("A stored menu group is required");
        }
        if (!outlet.getRestaurantChainId().equals(menuGroup.getRestaurantChainId())) {
            throw new IllegalArgumentException("Outlet and menu group must belong to the same restaurant chain");
        }

        EffectiveMenuSourceData source = sourceData.findByOutletId(outlet.getId())
                .orElseThrow(() -> new OutletNotFoundException("Outlet was not found"));
        Map<Long, OutletOverrideSnapshot> overrides = indexOverrides(source.overrides());
        List<OutletMenuItemSnapshot> result = new ArrayList<>();

        for (BaseMenuItemSnapshot base : source.baseItems()) {
            MenuItem storedItem = loadItem(base.getMenuItemId(), outlet.getRestaurantChainId());
            if (storedItem.getStatus() == MenuItemStatus.ARCHIVED) {
                continue;
            }
            EffectiveMenuItem effective = effectiveMenuResolver.resolve(base, outlet.getId(),
                    overrides.get(base.getMenuItemId()));
            result.add(toSnapshot(storedItem, effective.getItemName(), effective.getPrice(),
                    effective.isAvailable(), effective.isVisible(), outlet.getId()));
        }

        for (OutletLocalMenuItemSnapshot local : source.localItems()) {
            MenuItem storedItem = loadItem(local.getMenuItemId(), outlet.getRestaurantChainId());
            if (storedItem.getStatus() == MenuItemStatus.ARCHIVED) {
                continue;
            }
            result.add(toSnapshot(storedItem, local.getItemName(), local.getPrice(),
                    local.isAvailable(), local.isVisible(), outlet.getId()));
        }

        result.sort(Comparator.comparing(OutletMenuItemSnapshot::menuItemId));
        return List.copyOf(result);
    }

    private MenuItem loadItem(Long menuItemId, Long restaurantChainId) {
        return menuItems.findByIdAndRestaurantChainId(menuItemId, restaurantChainId)
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item with ID " + menuItemId + " was not found for restaurant chain " + restaurantChainId));
    }

    private OutletMenuItemSnapshot toSnapshot(MenuItem item, String name, java.math.BigDecimal price,
            boolean availability, boolean visibility, Long outletId) {
        MenuItemSchedule baseSchedule = item.getOutletId() == null
                ? schedules.find(item.getId(), null).orElse(null)
                : null;
        MenuItemSchedule outletSchedule = schedules.find(item.getId(), outletId).orElse(null);

        MenuItemSeasonalAvailability baseSeason = item.getOutletId() == null
                ? seasonalAvailability.find(item.getId(), null).orElse(null)
                : null;
        MenuItemSeasonalAvailability outletSeason = seasonalAvailability.find(item.getId(), outletId).orElse(null);
        MenuItemSeasonalAvailability effectiveSeason = outletSeason != null ? outletSeason : baseSeason;

        EventBundleSnapshot bundleSnapshot = item.getMenuItemKind() == MenuItemKind.BUNDLE
                ? bundleSnapshots.assemble(item.getId(), item.getRestaurantChainId())
                : null;

        return new OutletMenuItemSnapshot(
                item.getId(), item.getVersion(), name, price, item.getStatus(), availability, visibility,
                toEventWindows(baseSchedule), toEventWindows(outletSchedule),
                effectiveSeason == null ? null : effectiveSeason.getStartDate(),
                effectiveSeason == null ? null : effectiveSeason.getEndDate(),
                modifierSnapshots.assemble(item.getId()), bundleSnapshot);
    }

    private Map<Long, OutletOverrideSnapshot> indexOverrides(List<OutletOverrideSnapshot> overrides) {
        Map<Long, OutletOverrideSnapshot> result = new HashMap<>();
        for (OutletOverrideSnapshot override : overrides) {
            if (result.putIfAbsent(override.getMenuItemId(), override) != null) {
                throw new IllegalStateException("Multiple overrides exist for menu item " + override.getMenuItemId());
            }
        }
        return result;
    }

    private Map<String, List<EventScheduleWindow>> toEventWindows(MenuItemSchedule schedule) {
        if (schedule == null) {
            return Map.of();
        }
        Map<String, List<EventScheduleWindow>> result = new HashMap<>();
        schedule.getWindowsByDay().forEach((weekday, windows) -> result.put(weekday.name(), windows.stream()
                .map(window -> toEventWindow(window))
                .toList()));
        return result;
    }

    private EventScheduleWindow toEventWindow(ScheduleWindow window) {
        return new EventScheduleWindow(window.getStartTime(), window.getEndTime());
    }
}
