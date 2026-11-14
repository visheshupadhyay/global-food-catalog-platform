package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;

/** Port for the one optional seasonal rule in each menu-item scope. */
public interface MenuItemSeasonalAvailabilityRepository {
    MenuItemSeasonalAvailability save(MenuItemSeasonalAvailability season);
    Optional<MenuItemSeasonalAvailability> find(Long menuItemId, Long outletId);
}
