package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.domain.ModifierOption;

/** JPA adapter for persisting atomic modifier options. */
@Repository
public class ModifierOptionPersistenceAdapter implements ModifierOptionRepository {
    private final ModifierOptionJpaRepository repository;

    public ModifierOptionPersistenceAdapter(ModifierOptionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ModifierOption save(ModifierOption option) {
        ModifierOptionEntity entity;
        if (option.getId() == null) {
            entity = repository.save(new ModifierOptionEntity(option.getModifierGroupId(), option.getOptionName(),
                    option.getPriceDelta(), option.getDisplayOrder()));
        } else {
            entity = repository.findById(option.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Modifier option was not found"));
            if (!entity.getModifierGroupId().equals(option.getModifierGroupId())) {
                throw new IllegalArgumentException("Modifier option group cannot be changed");
            }
            entity.update(option.getOptionName(), option.getPriceDelta(), option.getDisplayOrder());
        }
        return toDomain(entity);
    }

    @Override
    public Optional<ModifierOption> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ModifierOption> findByModifierGroupId(Long modifierGroupId) {
        return repository.findByModifierGroupIdOrderByDisplayOrder(modifierGroupId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long optionId) {
        repository.deleteById(optionId);
    }

    private ModifierOption toDomain(ModifierOptionEntity entity) {
        return new ModifierOption(entity.getId(), entity.getModifierGroupId(), entity.getOptionName(),
                entity.getPriceDelta(), entity.getDisplayOrder());
    }
}
