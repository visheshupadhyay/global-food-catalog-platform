package com.vishesh.foodcatalog.catalog.persistence.adapter;

import org.springframework.stereotype.Repository;

import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.persistence.repository.OutletJpaRepository;

/** Reads only the outlet ownership fact needed by the catalog command service. */
@Repository
public class OutletPersistenceAdapter implements OutletRepository {

    private final OutletJpaRepository repository;

    public OutletPersistenceAdapter(OutletJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId) {
        return repository.existsByIdAndRestaurantChainId(outletId, restaurantChainId);
    }
}
