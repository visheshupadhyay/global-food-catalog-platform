package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModifierOptionJpaRepository extends JpaRepository<ModifierOptionEntity, Long> {
	List<ModifierOptionEntity> findByModifierGroupIdOrderByDisplayOrder(Long modifierGroupId);
}
