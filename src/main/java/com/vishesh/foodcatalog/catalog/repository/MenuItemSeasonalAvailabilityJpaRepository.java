package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemSeasonalAvailabilityJpaRepository
        extends JpaRepository<MenuItemSeasonalAvailabilityEntity, Long> {
    Optional<MenuItemSeasonalAvailabilityEntity> findByMenuItemIdAndOutletId(Long menuItemId, Long outletId);
    Optional<MenuItemSeasonalAvailabilityEntity> findByMenuItemIdAndOutletIdIsNull(Long menuItemId);
    void deleteByMenuItemIdAndOutletId(Long menuItemId, Long outletId);
    void deleteByMenuItemIdAndOutletIdIsNull(Long menuItemId);
}
