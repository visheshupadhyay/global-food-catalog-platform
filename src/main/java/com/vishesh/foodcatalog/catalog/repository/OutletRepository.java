package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;

import com.vishesh.foodcatalog.catalog.domain.Outlet;

public interface OutletRepository {
    Outlet save(Outlet outlet);

    Optional<Outlet> findById(Long outletId);

    boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId);

    boolean existsByRestaurantChainIdAndOutletCode(Long restaurantChainId, String outletCode);

    Optional<String> findTimezoneById(Long outletId);
}
