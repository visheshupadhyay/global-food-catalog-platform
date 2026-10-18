package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;

import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;

/**
 * Describes the storage jobs needed by the application.
 * The service does not need to know whether storage uses JPA, SQL, or another tool.
 */
public interface RestaurantChainRepository {
    RestaurantChain save(RestaurantChain chain);

    Optional<RestaurantChain> findById(Long id);

    Optional<RestaurantChain> findByBusinessCode(String businessCode);

    boolean existsByBusinessCode(String businessCode);
}
