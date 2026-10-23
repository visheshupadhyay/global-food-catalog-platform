package com.vishesh.foodcatalog.catalog.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuItemEntity;

public class MenuItemMapperTest {
    @Test
    void mapsPersistedDomainMenuGroupToEntity() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;

        MenuItem menuItem = new MenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);

        MenuItemMapper mapper = new MenuItemMapper();
        MenuItemEntity menuItemEntity = mapper.toEntity(menuItem);

        assertNotNull(menuItemEntity);
        assertEquals(restaurantChainId, menuItemEntity.getRestaurantChainId());
        assertEquals(itemName, menuItemEntity.getItemName());
        assertEquals(MenuItemStatus.DRAFT, menuItemEntity.getStatus());
        assertNotNull(menuItemEntity.getCreatedAt());
        assertNotNull(menuItemEntity.getUpdatedAt());
        assertNotNull(menuItemEntity.getCreatedAt());
        assertNotNull(menuItemEntity.getUpdatedAt());
        assertNull(menuItemEntity.getId());
    }

    @Test
    void mapsPersistedEntityToDomain() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;
        Long id = 501L;
        MenuItemStatus status = MenuItemStatus.DRAFT;
        boolean visibility = true;
        boolean availability = true;
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuItemEntity menuItemEntity = new MenuItemEntity(
                id,
                itemName,
                itemPrice,
                createdAt,
                menuGroupId,
                menuCategoryId,
                outletId,
                businessCode,
                status,
                visibility,
                availability,
                0L,
                restaurantChainId,
                updatedAt);

        MenuItemMapper mapper = new MenuItemMapper();
        MenuItem menuItem = mapper.toDomain(menuItemEntity);

        assertNotNull(menuItem);
        assertEquals(itemName, menuItem.getItemName());
        assertEquals(MenuItemStatus.DRAFT, menuItem.getStatus());
        assertNotNull(menuItem.getCreatedAt());
        assertNotNull(menuItem.getUpdatedAt());
        assertEquals(createdAt, menuItem.getCreatedAt());
        assertEquals(updatedAt, menuItem.getUpdatedAt());
        assertEquals(id, menuItem.getId());
    }

    @Test
    void mapsNewDomainMenuGroupToEntityWithGeneratedIdUnset() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;

        MenuItem menuItem = new MenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);
        MenuItemMapper mapper = new MenuItemMapper();
        MenuItemEntity entity = mapper.toEntity(menuItem);

        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals(MenuItemStatus.DRAFT, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0L, entity.getVersion());
    }

    @Test
    void rejectsEntityWithoutIdWhenConvertingToDomain() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;
        Long id = 501L;
        MenuItemStatus status = MenuItemStatus.DRAFT;
        boolean visibility = true;
        boolean availability = true;
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuItemMapper mapper = new MenuItemMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(new MenuItemEntity(
                null,
                itemName,
                itemPrice,
                createdAt,
                menuGroupId,
                menuCategoryId,
                outletId,
                businessCode,
                status,
                visibility,
                availability,
                0L,
                restaurantChainId,
                updatedAt)));
    }

    @Test
    void nullInputTests() {
        MenuItemMapper mapper = new MenuItemMapper();
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(null));

        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomain(null));
    }

    @Test
    void mapsEntityVersionToDomainVersion() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;
        Long id = 501L;
        MenuItemStatus status = MenuItemStatus.DRAFT;
        boolean visibility = true;
        boolean availability = true;
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuItemEntity entity = new MenuItemEntity(
                id,
                itemName,
                itemPrice,
                createdAt,
                menuGroupId,
                menuCategoryId,
                outletId,
                businessCode,
                status,
                visibility,
                availability,
                0L,
                restaurantChainId,
                updatedAt);
        MenuItemMapper mapper = new MenuItemMapper();
        MenuItem domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getVersion(), domain.getVersion());
        assertEquals(entity.getRestaurantChainId(), domain.getRestaurantChainId());
        assertEquals(0L, domain.getVersion());
    }

    @Test
    void mapsDomainVersionToEntityVersion() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;

        MenuItem domain = new MenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);

        MenuItemMapper mapper = new MenuItemMapper();
        MenuItemEntity entity = mapper.toEntity(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getVersion(), domain.getVersion());
        assertEquals(entity.getRestaurantChainId(), domain.getRestaurantChainId());
        assertEquals(0L, entity.getVersion());
    }

    @Test
    void newMenuItemEntityStartsWithVersionZero() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;
        MenuItemEntity entity = new MenuItemEntity(businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);
        assertEquals(null, entity.getId());
        assertEquals(0L, entity.getVersion());
        assertEquals(MenuItemStatus.DRAFT, entity.getStatus());
    }

    @Test
    void newDomainMenuGroupStartsWithVersionZero() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;

        MenuItem domain = new MenuItem(businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);

        assertNull(domain.getId());
        assertEquals(0L, domain.getVersion());
        assertEquals(MenuItemStatus.DRAFT, domain.getStatus());
    }
}
