package com.vishesh.foodcatalog.catalog.persistence.adapter;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;
import com.vishesh.foodcatalog.catalog.persistence.mapper.MenuGroupMapper;
import com.vishesh.foodcatalog.catalog.persistence.repository.MenuGroupJpaRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;

/** Converts menu groups and sends them to the menu-group database repository. */
@Repository 
public class MenuGroupPersistenceAdapter implements MenuGroupRepository {
    private final MenuGroupJpaRepository repository;
    private final MenuGroupMapper mapper;

    public MenuGroupPersistenceAdapter(MenuGroupJpaRepository repository,
            MenuGroupMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public MenuGroup save(MenuGroup chain) {
        // JPA saves the entity and may add the generated database ID.
        MenuGroupEntity entity = mapper.toEntity(chain);

        MenuGroupEntity savedEntity = repository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsNonArchivedByRestaurantChainIdAndDisplayName(Long restaurantChainId, String displayName) {
        String normalizedDisplayName = displayName.trim().toLowerCase(Locale.ROOT);

        List<ChainStatus> workingStatuses = List.of(ChainStatus.DRAFT, ChainStatus.ACTIVE);

        return repository.existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                restaurantChainId,
                normalizedDisplayName,
                workingStatuses);
    }

    @Override
    public Optional<MenuGroup> findByIdAndRestaurantChainId(Long menuGroupId, Long restaurantChainId) {
        return repository.findByIdAndRestaurantChainId(menuGroupId,restaurantChainId).map(mapper::toDomain);
    }
}
