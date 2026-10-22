package com.vishesh.foodcatalog.catalog.persistence.mapper;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;

/** Translates between business objects and database objects for menu groups. */
@Component
public class MenuGroupMapper {
    public MenuGroupEntity toEntity(MenuGroup domain) {
        // Convert a domain chain into a row that JPA can save.
        if (domain == null) {
            throw new IllegalArgumentException("Menu group is null");
        }

        MenuGroupEntity menuGroupEntity;

        if (domain.getId() == null) {
            menuGroupEntity = new MenuGroupEntity(domain.getRestaurantChainId(), domain.getDisplayName());
        } else {
            menuGroupEntity = new MenuGroupEntity(domain.getRestaurantChainId(),
                    domain.getDisplayName(),
                    domain.getStatus(),
                    domain.getCreatedAt(),
                    domain.getUpdatedAt(),
                    domain.getVersion(),
                    domain.getId());
        }

        return menuGroupEntity;
    }

    public MenuGroup toDomain(MenuGroupEntity entity) {
        // Convert a stored row back into a domain chain.
        if (entity == null) {
            throw new IllegalArgumentException("Menu group is null");
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Cannot rehydrate domain without database ID");
        }
        MenuGroup menuGroup = new MenuGroup(entity.getRestaurantChainId(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                entity.getId());
        return menuGroup;
    }
}
