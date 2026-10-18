package com.vishesh.foodcatalog.catalog.persistence.mapper;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.persistence.entity.RestaurantChainEntity;

/**
 * Converts between the business object and the database object.
 * The domain object protects business rules; the entity is shaped for JPA.
 */
@Component 
public class RestaurantChainMapper {

    public RestaurantChainEntity toEntity(RestaurantChain domain) {
        // Convert a domain chain into a row that JPA can save.
        if (domain == null) {
            throw new IllegalArgumentException("Restaurant Chain is null");
        }

        RestaurantChainEntity entity = new RestaurantChainEntity(domain.getId(),
                domain.getBusinessCode(),
                domain.getDisplayName(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
        return entity;
    }

    public RestaurantChain toDomain(RestaurantChainEntity entity) {
        // Convert a stored row back into a domain chain.
        if (entity == null) {
            throw new IllegalArgumentException("Restaurant Chain Entity is null");
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Cannot rehydrate domain without database ID");
        }
        RestaurantChain restaurantChain = new RestaurantChain(entity.getBusinessCode(), entity.getDisplayName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getId());
        return restaurantChain;
    }
}
