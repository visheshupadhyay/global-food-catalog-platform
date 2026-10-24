package com.vishesh.foodcatalog.catalog.effective.application;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;

@Component
/** Combines one base item with one outlet override without changing the base item. */
public class DefaultEffectiveMenuResolver implements EffectiveMenuResolver {

    @Override
    public EffectiveMenuItem resolve(BaseMenuItemSnapshot baseItem, Long outletId, OutletOverrideSnapshot override) {

        // Start with the chain value. A non-null outlet value replaces only that field.

        if (baseItem == null) {
            throw new IllegalArgumentException("Base menu item must be present");
        }

        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }

        if (override != null && !override.getMenuItemId().equals(baseItem.getMenuItemId())) {
            throw new IllegalArgumentException("Override must belong to the same menu item as the base item");
        }

        if (override != null && !override.getOutletId().equals(outletId)) {
            throw new IllegalArgumentException("Override must belong to the requested outlet");
        }

        BigDecimal price;
        boolean available;
        boolean visible;

        if (override != null && override.getPriceOverride() != null) {
            price = override.getPriceOverride();
        } else {
            price = baseItem.getPrice();
        }

        if (override != null && override.getAvailableOverride() != null) {
            available = override.getAvailableOverride();
        } else {
            available = baseItem.isAvailable();
        }

        if (!baseItem.isVisible()) {
            visible = false;
        } else if (override != null && override.getVisibleOverride() != null) {
            visible = override.getVisibleOverride();
        } else {
            visible = baseItem.isVisible();
        }

        return new EffectiveMenuItem(baseItem.getMenuItemId(),
                outletId,
                baseItem.getItemName(),
                price,
                available,
                visible);

    }

}
