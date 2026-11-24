package com.vishesh.foodcatalog.catalog.persistence.adapter;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<Long> findMenuGroupIdByOutletId(Long outletId) {
        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be positive");
        }
        return repository.findMenuGroupIdByOutletId(outletId);
    }

    @Override
    public void assignMenuGroupToOutlet(Long menuGroupId, Long outletId) {
        if (menuGroupId == null || menuGroupId <= 0) {
            throw new IllegalArgumentException("Menu group ID must be positive");
        }
        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be positive");
        }
        repository.assignMenuGroupToOutlet(menuGroupId, outletId);
    }
    
}
