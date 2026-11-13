package com.vishesh.foodcatalog.catalog.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.vishesh.foodcatalog.catalog.persistence.entity.OutletEntity;

/** Spring Data derives the ownership query from this method name. */
public interface OutletJpaRepository extends JpaRepository<OutletEntity, Long> {
    boolean existsByIdAndRestaurantChainId(Long outletId, Long restaurantChainId);
    Optional<OutletEntity> findById(Long outletId);
}
