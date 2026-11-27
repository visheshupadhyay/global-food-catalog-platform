package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.vishesh.foodcatalog.catalog.domain.ModifierGroup;

/** Port for modifier groups owned by a product or bundle. */
public interface ModifierGroupRepository {
	ModifierGroup save(ModifierGroup group);

	Optional<ModifierGroup> findById(Long id);

	List<ModifierGroup> findByOwnerMenuItemId(Long ownerMenuItemId);

	void deleteById(Long groupId);
}
