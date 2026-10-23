package com.vishesh.foodcatalog.catalog.persistence.mapper;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuItemEntity;

/** Converts between business objects and database objects. */
@Component 
public class MenuItemMapper {
    public MenuItemEntity toEntity(MenuItem domain) {
        // Convert a domain menu item into a row that JPA can save.
        if (domain == null) {
            throw new IllegalArgumentException("Menu item is null");
        }

        MenuItemEntity menuItemEntity;

        if (domain.getId() == null) {
            menuItemEntity = new MenuItemEntity(domain.getBusinessCode(),
                    domain.getItemName(),
                    domain.getItemPrice(),
                    domain.getRestaurantChainId(),
                    domain.getMenuGroupId(),
                    domain.getMenuCategoryId(),
                    domain.getOutletId());
        } else {
            menuItemEntity = new MenuItemEntity(domain.getId(),
                    domain.getItemName(),
                    domain.getItemPrice(),
                    domain.getCreatedAt(),
                    domain.getMenuGroupId(),
                    domain.getMenuCategoryId(),
                    domain.getOutletId(),
                    domain.getBusinessCode(),
                    domain.getStatus(),
                    domain.isVisibility(),
                    domain.isAvailability(),
                    domain.getVersion(),
                    domain.getRestaurantChainId(),
                    domain.getUpdatedAt());
        }

        return menuItemEntity;
    }

    public MenuItem toDomain(MenuItemEntity entity) {
        // Convert a stored row back into a domain menu item.
        if (entity == null) {
            throw new IllegalArgumentException("Menu item is null");
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Cannot rehydrate domain without database ID");
        }
        MenuItem menuItem = new MenuItem(
                entity.getId(),
                entity.getItemName(),
                entity.getItemPrice(),
                entity.getCreatedAt(),
                entity.getMenuGroupId(),
                entity.getMenuCategoryId(),
                entity.getOutletId(),
                entity.getBusinessCode(),
                entity.getStatus(),
                entity.isVisibility(),
                entity.isAvailability(),
                entity.getVersion(),
                entity.getRestaurantChainId(),
                entity.getUpdatedAt());
        return menuItem;
    }
}
