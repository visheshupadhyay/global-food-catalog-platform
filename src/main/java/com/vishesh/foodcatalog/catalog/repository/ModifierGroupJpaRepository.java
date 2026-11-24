package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModifierGroupJpaRepository extends JpaRepository<ModifierGroupEntity, Long> {
    List<ModifierGroupEntity> findByOwnerMenuItemIdOrderByDisplayOrder(Long ownerMenuItemId);
}
