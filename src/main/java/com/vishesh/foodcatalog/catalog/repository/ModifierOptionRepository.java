package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.vishesh.foodcatalog.catalog.domain.ModifierOption;

/** Port for atomic options belonging to one modifier group. */
public interface ModifierOptionRepository {
	ModifierOption save(ModifierOption option);

	Optional<ModifierOption> findById(Long id);

	List<ModifierOption> findByModifierGroupId(Long modifierGroupId);

	void deleteById(Long optionId);
}
