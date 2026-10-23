package com.vishesh.foodcatalog.catalog.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuItemEntity;
import com.vishesh.foodcatalog.catalog.persistence.mapper.MenuItemMapper;
import com.vishesh.foodcatalog.catalog.persistence.repository.MenuItemJpaRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

/** Connects the application repository contract to the JPA repository. */
@Repository 
public class MenuItemPersistenceAdapter implements MenuItemRepository {

    private final MenuItemJpaRepository repository;
    private final MenuItemMapper mapper;

    public MenuItemPersistenceAdapter(MenuItemJpaRepository repository,
            MenuItemMapper mapper) {

        this.mapper = mapper;
        this.repository = repository;
    }

    @Override
    public MenuItem save(MenuItem menuItem) {
        MenuItemEntity entity = mapper.toEntity(menuItem);
        MenuItemEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MenuItem> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MenuItem> findByIdAndRestaurantChainId(Long id, Long restaurantChainId) {
        return repository.findByIdAndRestaurantChainId(id, restaurantChainId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<MenuItem> findByBusinessCodeAndRestaurantChainId(String businessCode, Long restaurantChainId) {
        return repository.findByBusinessCodeAndRestaurantChainId(businessCode.trim(), restaurantChainId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByBusinessCodeAndRestaurantChainId(String businessCode, Long restaurantChainId) {
        return repository.existsByBusinessCodeAndRestaurantChainId(businessCode.trim(), restaurantChainId);
    }

}
