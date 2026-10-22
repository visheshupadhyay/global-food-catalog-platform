package com.vishesh.foodcatalog.catalog.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;

public class MenuGroupMapperTest {
    @Test
    void mapsPersistedDomainMenuGroupToEntity() {
        Long restaurantChainId = 42L;
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuGroup menuGroup = new MenuGroup(
                restaurantChainId,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                101L);

        MenuGroupMapper mapper = new MenuGroupMapper();
        MenuGroupEntity menuGroupEntity = mapper.toEntity(menuGroup);

        assertNotNull(menuGroupEntity);
        assertEquals(restaurantChainId, menuGroupEntity.getRestaurantChainId());
        assertEquals(displayName, menuGroupEntity.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, menuGroupEntity.getStatus());
        assertNotNull(menuGroupEntity.getCreatedAt());
        assertNotNull(menuGroupEntity.getUpdatedAt());
        assertEquals(createdAt, menuGroupEntity.getCreatedAt());
        assertEquals(updatedAt, menuGroupEntity.getUpdatedAt());
        assertEquals(101L, menuGroupEntity.getId());
    }

    @Test
    void mapsPersistedEntityToDomain() {
        Long restaurantChainId = 42L;
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(
                restaurantChainId,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                101L);

        MenuGroupMapper mapper = new MenuGroupMapper();
        MenuGroup menuGroup = mapper.toDomain(menuGroupEntity);

        assertNotNull(menuGroup);
        assertEquals(displayName, menuGroup.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, menuGroup.getStatus());
        assertNotNull(menuGroup.getCreatedAt());
        assertNotNull(menuGroup.getUpdatedAt());
        assertEquals(createdAt, menuGroup.getCreatedAt());
        assertEquals(updatedAt, menuGroup.getUpdatedAt());
        assertEquals(101L, menuGroup.getId());
    }

    @Test
    void mapsNewDomainMenuGroupToEntityWithGeneratedIdUnset() {
        Long restaurantChainId = 42L;
        String displayName = "BURGER-KING";
        MenuGroupMapper mapper = new MenuGroupMapper();

        MenuGroup restaurantChain = new MenuGroup(restaurantChainId, displayName);
        MenuGroupEntity entity = mapper.toEntity(restaurantChain);

        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals(ChainStatus.DRAFT, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0L, entity.getVersion());
    }

    @Test
    void rejectsEntityWithoutIdWhenConvertingToDomain() {
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuGroupMapper mapper = new MenuGroupMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(new MenuGroupEntity(
                null,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                null)));
    }

    @Test
    void nullInputTests() {
        MenuGroupMapper mapper = new MenuGroupMapper();
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(null));

        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomain(null));
    }

    @Test
    void mapsEntityVersionToDomainVersion() {
        Long restaurantChainId = 201L;
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuGroupEntity entity = new MenuGroupEntity(
                restaurantChainId,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                101L);
        MenuGroupMapper mapper = new MenuGroupMapper();
        MenuGroup domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getVersion(), domain.getVersion());
        assertEquals(entity.getRestaurantChainId(), domain.getRestaurantChainId());
        assertEquals(3L, domain.getVersion());
    }

    @Test
    void mapsDomainVersionToEntityVersion() {
        Long restaurantChainId = 201L;
        String displayName = "BURGER-KING";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuGroup domain = new MenuGroup(
                restaurantChainId,
                displayName,
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                101L);
        MenuGroupMapper mapper = new MenuGroupMapper();
        MenuGroupEntity entity = mapper.toEntity(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getVersion(), domain.getVersion());
        assertEquals(entity.getRestaurantChainId(), domain.getRestaurantChainId());
        assertEquals(3L, entity.getVersion());
    }

    @Test
    void newMenuGroupEntityStartsWithVersionZero() {
        Long restaurantChainId = 201L;
        String displayName = "BURGER-KING";
        MenuGroupEntity entity = new MenuGroupEntity(restaurantChainId, displayName);
        assertEquals(null, entity.getId());
        assertEquals(0L, entity.getVersion());
        assertEquals(ChainStatus.DRAFT, entity.getStatus());
    }

    @Test
    void newDomainMenuGroupStartsWithVersionZero() {
        Long restaurantChainId = 201L;
        String displayName = "BURGER-KING";

        MenuGroup domain = new MenuGroup(restaurantChainId, displayName);

        assertNull(domain.getId());
        assertEquals(0L, domain.getVersion());
        assertEquals(ChainStatus.DRAFT, domain.getStatus());
    }
}
