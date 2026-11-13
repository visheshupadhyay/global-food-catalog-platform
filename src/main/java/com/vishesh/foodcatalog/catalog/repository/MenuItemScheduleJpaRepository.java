package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemScheduleJpaRepository extends JpaRepository<MenuItemScheduleEntity, Long> {
    List<MenuItemScheduleEntity> findByMenuItemIdAndOutletId(Long menuItemId, Long outletId);

    List<MenuItemScheduleEntity> findByMenuItemIdAndOutletIdIsNull(Long menuItemId);

    void deleteByMenuItemIdAndOutletId(Long menuItemId, Long outletId);

    void deleteByMenuItemIdAndOutletIdIsNull(Long menuItemId);

    @Query("select coalesce(max(s.scheduleVersion), -1) from MenuItemScheduleEntity s "
            + "where s.menuItemId = :menuItemId and "
            + "((:outletId is null and s.outletId is null) or s.outletId = :outletId)")
    Long findMaxVersion(@Param("menuItemId") Long menuItemId, @Param("outletId") Long outletId);
}
