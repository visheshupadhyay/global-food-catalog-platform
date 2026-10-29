package com.vishesh.foodcatalog.catalog.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.vishesh.foodcatalog.catalog.persistence.repository.MenuGroupOutletJpaRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupOutletRepository;

@Repository
public class MenuGroupOutletPersistenceAdapter implements MenuGroupOutletRepository {

    private final MenuGroupOutletJpaRepository repository;

    public MenuGroupOutletPersistenceAdapter(MenuGroupOutletJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Long> findOutletIdsByMenuGroupId(Long menuGroupId) {
        if (menuGroupId == null || menuGroupId <= 0) {
            throw new IllegalArgumentException("Menu group ID must be positive");
        }
        return repository.findOutletIdsByMenuGroupId(menuGroupId);
    }
    
}
