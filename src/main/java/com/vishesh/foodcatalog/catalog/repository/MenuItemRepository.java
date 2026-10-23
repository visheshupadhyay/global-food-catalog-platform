package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;

public interface MenuItemRepository {
    MenuItem save(MenuItem menuItem);

    Optional<MenuItem> findById(Long id);

    Optional<MenuItem> findByIdAndRestaurantChainId(
            Long id,
            Long restaurantChainId);

    Optional<MenuItem> findByBusinessCodeAndRestaurantChainId(
            String businessCode,
            Long restaurantChainId);

    boolean existsByBusinessCodeAndRestaurantChainId(
            String businessCode,
            Long restaurantChainId);
}
