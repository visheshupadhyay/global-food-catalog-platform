package com.vishesh.foodcatalog.catalog.effective.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;

/**
 * Builds the menu that one outlet shows to its customers.
 *
 * The final menu has three parts:
 * shared items from the restaurant chain, changes made by this outlet,
 * and items that belong only to this outlet. This class puts them together.
 */
@Component
public class EffectiveMenuAssembler {

    private final EffectiveMenuResolver effectiveMenuResolver;

    public EffectiveMenuAssembler(EffectiveMenuResolver effectiveMenuResolver) {
        if (effectiveMenuResolver == null) {
            throw new IllegalArgumentException("Effective menu resolver must be present");
        }
        this.effectiveMenuResolver = effectiveMenuResolver;
    }

    public List<EffectiveMenuItem> assemble(
            Long outletId,
            List<BaseMenuItemSnapshot> baseItems,
            List<OutletOverrideSnapshot> overrides,
            List<OutletLocalMenuItemSnapshot> localItems) {

        // First, make sure every piece of data belongs to this outlet.
        validateInputs(outletId, baseItems, overrides, localItems);

        // Make a quick lookup box: base item ID -> this outlet's change for that item.
        // This helps us find a change quickly while reading the shared items.
        Map<Long, OutletOverrideSnapshot> overridesByMenuItemId = buildOverrideLookup(overrides);
        Set<Long> baseItemIds = collectBaseItemIds(baseItems);

        // A change without its shared item makes no sense. Stop instead of hiding
        // a data mistake.
        if (!baseItemIds.containsAll(overridesByMenuItemId.keySet())) {
            throw new IllegalArgumentException("Every override must reference a supplied base menu item");
        }

        List<EffectiveMenuItem> effectiveItems = new ArrayList<>();
        for (BaseMenuItemSnapshot baseItem : baseItems) {
            // Only ACTIVE items are ready for customers to see.
            if (baseItem.getStatus() != MenuItemStatus.ACTIVE) {
                continue;
            }

            // Ask the resolver for the final values. It uses the shared value unless
            // this outlet changed that value.
            EffectiveMenuItem effectiveItem = effectiveMenuResolver.resolve(
                    baseItem, outletId, overridesByMenuItemId.get(baseItem.getMenuItemId()));

            // Invisible items do not appear at all. Unavailable items do appear,
            // so customers know that the item exists but cannot be ordered now.
            if (effectiveItem.isVisible()) {
                effectiveItems.add(effectiveItem);
            }
        }

        for (OutletLocalMenuItemSnapshot localItem : localItems) {
            // A local item already belongs to this outlet. It has no shared item
            // to copy from, so we can add it directly.
            if (localItem.getStatus() == MenuItemStatus.ACTIVE && localItem.isVisible()) {
                effectiveItems.add(new EffectiveMenuItem(
                        localItem.getMenuItemId(), outletId, localItem.getItemName(), localItem.getPrice(),
                        localItem.isAvailable(), true));
            }
        }

        // Put items in A-to-Z order. The ID gives a steady order when two items
        // have the same name.
        effectiveItems.sort(Comparator
                .comparing(EffectiveMenuItem::getItemName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EffectiveMenuItem::getMenuItemId));

        // Give back a list that other code cannot change by accident.
        return List.copyOf(effectiveItems);
    }

    private static void validateInputs(
            Long outletId,
            List<BaseMenuItemSnapshot> baseItems,
            List<OutletOverrideSnapshot> overrides,
            List<OutletLocalMenuItemSnapshot> localItems) {
        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }
        if (baseItems == null || overrides == null || localItems == null) {
            throw new IllegalArgumentException("Menu item lists must be present");
        }
        if (baseItems.stream().anyMatch(item -> item == null)
                || overrides.stream().anyMatch(item -> item == null)
                || localItems.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException("Menu item lists cannot contain null values");
        }
        // This stops Berlin data from being mixed into a Munich menu, or the
        // other way around.
        if (overrides.stream().anyMatch(override -> !outletId.equals(override.getOutletId()))) {
            throw new IllegalArgumentException("Every override must belong to the requested outlet");
        }
        if (localItems.stream().anyMatch(item -> !outletId.equals(item.getOutletId()))) {
            throw new IllegalArgumentException("Every local item must belong to the requested outlet");
        }
    }

    private static Map<Long, OutletOverrideSnapshot> buildOverrideLookup(List<OutletOverrideSnapshot> overrides) {
        Map<Long, OutletOverrideSnapshot> overridesByMenuItemId = new HashMap<>();
        for (OutletOverrideSnapshot override : overrides) {
            OutletOverrideSnapshot previous = overridesByMenuItemId.putIfAbsent(
                    override.getMenuItemId(), override);
            if (previous != null) {
                // Two changes for one item would leave us unsure which value to use.
                // One outlet may have only one change for one shared item.
                throw new IllegalArgumentException("Only one override is allowed for an outlet and base menu item");
            }
        }
        return overridesByMenuItemId;
    }

    private static Set<Long> collectBaseItemIds(List<BaseMenuItemSnapshot> baseItems) {
        Set<Long> baseItemIds = new HashSet<>();
        for (BaseMenuItemSnapshot baseItem : baseItems) {
            if (!baseItemIds.add(baseItem.getMenuItemId())) {
                // One shared item should appear once. Otherwise the customer could
                // see the same item twice.
                throw new IllegalArgumentException("Base menu item IDs must be unique");
            }
        }
        return baseItemIds;
    }
}
