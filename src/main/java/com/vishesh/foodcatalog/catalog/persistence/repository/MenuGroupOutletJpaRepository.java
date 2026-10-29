package com.vishesh.foodcatalog.catalog.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;

public interface MenuGroupOutletJpaRepository extends Repository<MenuGroupEntity, Long> {

    @Query(value = "select outlet_id from menu_group_outlet where menu_group_id = :menuGroupId", nativeQuery = true)
    List<Long> findOutletIdsByMenuGroupId(@Param("menuGroupId") Long menuGroupId);
}
