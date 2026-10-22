package com.vishesh.foodcatalog.catalog.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.persistence.entity.RestaurantChainEntity;
import com.vishesh.foodcatalog.catalog.persistence.mapper.RestaurantChainMapper;
import com.vishesh.foodcatalog.catalog.persistence.repository.RestaurantChainJpaRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

/**
 * Connects the business repository to the database repository.
 * It translates domain objects to database objects and translates them back.
 */
/** Converts chain objects and sends them to the chain database repository. */
@Repository 
public class RestaurantChainPersistenceAdapter implements RestaurantChainRepository {
    private final RestaurantChainJpaRepository repository;
    private final RestaurantChainMapper mapper;

    public RestaurantChainPersistenceAdapter(RestaurantChainJpaRepository repository,
            RestaurantChainMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RestaurantChain save(RestaurantChain chain) {
        // JPA saves the entity and may add the generated database ID.
        RestaurantChainEntity entity = mapper.toEntity(chain);

        RestaurantChainEntity savedEntity = repository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<RestaurantChain> findById(Long id) {
        // The mapper turns the database row back into a business object.
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<RestaurantChain> findByBusinessCode(String businessCode) {
        return repository.findByBusinessCode(businessCode).map(mapper::toDomain);
    }

    @Override
    public boolean existsByBusinessCode(String businessCode) {
        return repository.existsByBusinessCode(businessCode);
    }
}
