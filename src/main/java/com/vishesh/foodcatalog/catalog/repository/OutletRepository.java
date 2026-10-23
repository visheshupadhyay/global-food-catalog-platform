package com.vishesh.foodcatalog.catalog.repository;

public interface OutletRepository {
    boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId);
}
