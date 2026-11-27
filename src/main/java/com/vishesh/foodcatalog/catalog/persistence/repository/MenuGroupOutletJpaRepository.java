package com.vishesh.foodcatalog.catalog.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;

public interface MenuGroupOutletJpaRepository extends Repository<MenuGroupEntity, Long> {

	@Query(value = "select outlet_id from menu_group_outlet where menu_group_id = :menuGroupId", nativeQuery = true)
	List<Long> findOutletIdsByMenuGroupId(@Param("menuGroupId") Long menuGroupId);

	@Query(value = "select menu_group_id from menu_group_outlet where outlet_id = :outletId", nativeQuery = true)
	Optional<Long> findMenuGroupIdByOutletId(@Param("outletId") Long outletId);

	@Modifying
	@Query(value = "insert into menu_group_outlet (menu_group_id, outlet_id) values (:menuGroupId, :outletId)", nativeQuery = true)
	void assignMenuGroupToOutlet(@Param("menuGroupId") Long menuGroupId, @Param("outletId") Long outletId);
}
