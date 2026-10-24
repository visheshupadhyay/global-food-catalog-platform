package com.vishesh.foodcatalog.catalog.effective.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.vishesh.foodcatalog.catalog.effective.persistence.projection.BaseMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletLocalMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletOverrideProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletStatusProjection;
import com.vishesh.foodcatalog.catalog.persistence.entity.OutletEntity;

/** Read-only queries for the effective-menu source data. */
public interface EffectiveMenuJpaReadRepository extends Repository<OutletEntity, Long> {
    @Query(value = """
            SELECT o.id AS "outletId",
                   o.status AS "outletStatus"
            FROM outlet o
            WHERE o.id = :outletId
            """, nativeQuery = true)
    Optional<OutletStatusProjection> findOutletStatusByOutletId(@Param("outletId") Long outletId);

    @Query(value = """
            SELECT mi.id AS "menuItemId",
                   mi.item_name AS "itemName",
                   mi.item_price AS "price",
                   mi.availability AS "available",
                   mi.visibility AS "visible",
                   mi.status AS "status"
            FROM menu_group_outlet mgo
            JOIN menu_group mg ON mg.id = mgo.menu_group_id
            JOIN menu_item mi ON mi.menu_group_id = mg.id
            WHERE mgo.outlet_id = :outletId
              AND mg.status = 'ACTIVE'
              AND mi.menu_group_id IS NOT NULL
              AND mi.outlet_id IS NULL
            """, nativeQuery = true)
    List<BaseMenuItemProjection> findBaseItemsByOutletId(@Param("outletId") Long outletId);

    @Query(value = """
            SELECT oo.base_menu_item_id AS "menuItemId",
                   oo.override_price AS "priceOverride",
                   oo.override_available AS "availableOverride",
                   oo.override_visible AS "visibleOverride"
            FROM outlet_override oo
            WHERE oo.outlet_id = :outletId
            """, nativeQuery = true)
    List<OutletOverrideProjection> findOverridesByOutletId(@Param("outletId") Long outletId);

    @Query(value = """
            SELECT mi.id AS "menuItemId",
                   mi.outlet_id AS "outletId",
                   mi.item_name AS "itemName",
                   mi.item_price AS "price",
                   mi.availability AS "available",
                   mi.visibility AS "visible",
                   mi.status AS "status"
            FROM menu_item mi
            WHERE mi.outlet_id = :outletId
            """, nativeQuery = true)
    List<OutletLocalMenuItemProjection> findLocalItemsByOutletId(@Param("outletId") Long outletId);
}
