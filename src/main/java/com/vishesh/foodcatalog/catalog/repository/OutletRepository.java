package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;

public interface OutletRepository {
    boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId);

    Optional<String> findTimezoneById(Long outletId);
}
