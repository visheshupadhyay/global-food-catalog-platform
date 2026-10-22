package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;

import com.vishesh.foodcatalog.catalog.domain.MenuGroup;

/** Business-facing storage operations for menu groups. */
public interface MenuGroupRepository {
    MenuGroup save(MenuGroup menuGroup);

    Optional<MenuGroup> findByIdAndRestaurantChainId(Long menuGroupId, Long restaurantChainId);

    boolean existsNonArchivedByRestaurantChainIdAndDisplayName(Long restaurantChainId, String displayName);
}
