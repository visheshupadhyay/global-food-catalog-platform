package com.vishesh.foodcatalog.catalog.persistence.adapter;

import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.persistence.repository.OutletJpaRepository;
import com.vishesh.foodcatalog.catalog.domain.Outlet;
import com.vishesh.foodcatalog.catalog.persistence.mapper.OutletMapper;

/** Reads only the outlet ownership fact needed by the catalog command service. */
@Repository
public class OutletPersistenceAdapter implements OutletRepository {

    private final OutletJpaRepository repository;
    private final OutletMapper mapper;

    public OutletPersistenceAdapter(OutletJpaRepository repository, OutletMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Outlet save(Outlet outlet) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(outlet)));
    }

    @Override
    public Optional<Outlet> findById(Long outletId) {
        return repository.findById(outletId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId) {
        return repository.existsByIdAndRestaurantChainId(outletId, restaurantChainId);
    }

    @Override
    public boolean existsByRestaurantChainIdAndOutletCode(Long restaurantChainId, String outletCode) {
        return repository.existsByRestaurantChainIdAndOutletCode(restaurantChainId, outletCode);
    }

    @Override
    public Optional<String> findTimezoneById(Long outletId) {
        return repository.findById(outletId).map(entity -> entity.getTimezone());
    }
}
