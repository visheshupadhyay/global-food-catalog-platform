package com.vishesh.foodcatalog.catalog.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vishesh.foodcatalog.catalog.persistence.entity.OutletEntity;

/** Spring Data derives the ownership query from this method name. */
public interface OutletJpaRepository extends JpaRepository<OutletEntity, Long> {
    boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId);
}
