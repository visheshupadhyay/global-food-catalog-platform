package com.vishesh.foodcatalog.catalog.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.persistence.entity.RestaurantChainEntity;

public class RestaurantChainMapperTest {
    @Test
    void mapsPersistedDomainChainToEntity() {
        String businessCode = "BURGER-KING-001";
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        RestaurantChain restaurantChain = new RestaurantChain(
                businessCode,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                101L);

        RestaurantChainMapper mapper = new RestaurantChainMapper();
        RestaurantChainEntity restaurantChainEntity = mapper.toEntity(restaurantChain);

        assertNotNull(restaurantChainEntity);
        assertEquals(businessCode, restaurantChainEntity.getBusinessCode());
        assertEquals(displayName, restaurantChainEntity.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, restaurantChainEntity.getStatus());
        assertNotNull(restaurantChainEntity.getCreatedAt());
        assertNotNull(restaurantChainEntity.getUpdatedAt());
        assertEquals(createdAt, restaurantChainEntity.getCreatedAt());
        assertEquals(updatedAt, restaurantChainEntity.getUpdatedAt());
        assertEquals(101L, restaurantChainEntity.getId());
    }

    @Test
    void mapsPersistedEntityToDomain() {
        String businessCode = "BURGER-KING-001";
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        RestaurantChainEntity restaurantChainEntity = new RestaurantChainEntity(

                101L, businessCode,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt);

        RestaurantChainMapper mapper = new RestaurantChainMapper();
        RestaurantChain restaurantChain = mapper.toDomain(restaurantChainEntity);

        assertNotNull(restaurantChain);
        assertEquals(businessCode, restaurantChain.getBusinessCode());
        assertEquals(displayName, restaurantChain.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, restaurantChain.getStatus());
        assertNotNull(restaurantChain.getCreatedAt());
        assertNotNull(restaurantChain.getUpdatedAt());
        assertEquals(createdAt, restaurantChain.getCreatedAt());
        assertEquals(updatedAt, restaurantChain.getUpdatedAt());
        assertEquals(101L, restaurantChain.getId());
    }

    @Test
    void mapsNewDomainChainToEntityWithGeneratedIdUnset() {
        String businessCode = "BURGER-KING-001";
        String displayName = "BURGER-KING";
        RestaurantChainMapper mapper = new RestaurantChainMapper();

        RestaurantChain restaurantChain = new RestaurantChain(businessCode, displayName);
        RestaurantChainEntity entity = mapper.toEntity(restaurantChain);

        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals(ChainStatus.DRAFT, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void rejectsEntityWithoutIdWhenConvertingToDomain() {
        String businessCode = "BURGER-KING-001";
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        RestaurantChainMapper mapper = new RestaurantChainMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(new RestaurantChainEntity(
                null,
                businessCode,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt)));
    }

    @Test
    void nullInputTests() {
        RestaurantChainMapper mapper = new RestaurantChainMapper();
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(null));

        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomain(null));
    }
}