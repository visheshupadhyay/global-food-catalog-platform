package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.domain.ModifierGroup;

/** JPA adapter for persisting modifier-group configuration. */
@Repository
public class ModifierGroupPersistenceAdapter implements ModifierGroupRepository {
	private final ModifierGroupJpaRepository repository;

	public ModifierGroupPersistenceAdapter(ModifierGroupJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public ModifierGroup save(ModifierGroup group) {
		ModifierGroupEntity entity;
		if (group.getId() == null) {
			entity = repository.save(new ModifierGroupEntity(group.getOwnerMenuItemId(), group.getGroupName(),
					group.getMinimumSelections(), group.getMaximumSelections(), group.getDisplayOrder()));
		} else {
			entity = repository.findById(group.getId())
					.orElseThrow(() -> new IllegalArgumentException("Modifier group was not found"));
			if (!entity.getOwnerMenuItemId().equals(group.getOwnerMenuItemId())) {
				throw new IllegalArgumentException("Modifier group owner cannot be changed");
			}
			entity.update(group.getGroupName(), group.getMinimumSelections(), group.getMaximumSelections(),
					group.getDisplayOrder());
		}
		return toDomain(entity);
	}

	@Override
	public Optional<ModifierGroup> findById(Long id) {
		return repository.findById(id).map(this::toDomain);
	}

	@Override
	public List<ModifierGroup> findByOwnerMenuItemId(Long ownerMenuItemId) {
		return repository.findByOwnerMenuItemIdOrderByDisplayOrder(ownerMenuItemId).stream().map(this::toDomain)
				.toList();
	}

	@Override
	@Transactional
	public void deleteById(Long groupId) {
		repository.deleteById(groupId);
	}

	private ModifierGroup toDomain(ModifierGroupEntity entity) {
		return new ModifierGroup(entity.getId(), entity.getOwnerMenuItemId(), entity.getGroupName(),
				entity.getMinimumSelections(), entity.getMaximumSelections(), entity.getDisplayOrder());
	}

}
