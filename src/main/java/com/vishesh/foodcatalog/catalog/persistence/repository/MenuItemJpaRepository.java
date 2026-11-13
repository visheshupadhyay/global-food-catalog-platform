package com.vishesh.foodcatalog.catalog.persistence.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "select mi.outlet_id from menu_item mi where mi.id = :menuItemId and mi.outlet_id is not null "
            + "union "
            + "select mgo.outlet_id from menu_item mi join menu_group_outlet mgo on mgo.menu_group_id = mi.menu_group_id "
            + "where mi.id = :menuItemId and mi.menu_group_id is not null", nativeQuery = true)
    List<Long> findAffectedOutletIds(@Param("menuItemId") Long menuItemId);
}
