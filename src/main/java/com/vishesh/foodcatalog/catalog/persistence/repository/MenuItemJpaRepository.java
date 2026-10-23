package com.vishesh.foodcatalog.catalog.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuItemEntity;

public interface MenuItemJpaRepository extends JpaRepository<MenuItemEntity, Long> {

    Optional<MenuItemEntity> findByIdAndRestaurantChainId(
            Long id,
            Long restaurantChainId);

    Optional<MenuItemEntity> findByBusinessCodeAndRestaurantChainId(
            String businessCode,
            Long restaurantChainId);

    boolean existsByBusinessCodeAndRestaurantChainId(
            String businessCode,
            Long restaurantChainId);
}
