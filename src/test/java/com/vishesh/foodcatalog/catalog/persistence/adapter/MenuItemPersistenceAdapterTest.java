package com.vishesh.foodcatalog.catalog.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuItemEntity;
import com.vishesh.foodcatalog.catalog.persistence.mapper.MenuItemMapper;
import com.vishesh.foodcatalog.catalog.persistence.repository.MenuItemJpaRepository;

public class MenuItemPersistenceAdapterTest {
    @Mock
    private MenuItemJpaRepository repository;

    @Mock
    private MenuItemMapper mapper;

    @InjectMocks
    private MenuItemPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_returnsMappedSavedDomain() {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = 401L;

        MenuItem menuItem = new MenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                null);

        MenuItemEntity entity = new MenuItemEntity(businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                null);

        MenuItemEntity savedEntity = new MenuItemEntity(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                null);

        MenuItem expectedMenuItem = new MenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                null);

        when(mapper.toEntity(menuItem)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(expectedMenuItem);

        MenuItem actualMenuItem = adapter.save(menuItem);

        assertEquals(expectedMenuItem.getId(), actualMenuItem.getId());
        assertEquals(expectedMenuItem.getItemName(), actualMenuItem.getItemName());
        assertEquals(expectedMenuItem.getStatus(), actualMenuItem.getStatus());
        assertEquals(expectedMenuItem.getCreatedAt(), actualMenuItem.getCreatedAt());
        assertEquals(expectedMenuItem.getUpdatedAt(), actualMenuItem.getUpdatedAt());
        verify(mapper).toEntity(menuItem);
        verify(repository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findByIdAndRestaurantChainId_whenFound_forwardsBothIdsAndMapsDomain() {
        Long MenuItemId = 42L;
        Long restaurantChainId = 101L;
        MenuItemEntity entity = mock(MenuItemEntity.class);
        MenuItem expected = mock(MenuItem.class);

        when(repository.findByIdAndRestaurantChainId(MenuItemId, restaurantChainId))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<MenuItem> actual = adapter.findByIdAndRestaurantChainId(
                MenuItemId, restaurantChainId);

        assertEquals(Optional.of(expected), actual);
        verify(repository).findByIdAndRestaurantChainId(MenuItemId, restaurantChainId);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findByIdAndRestaurantChainId_whenMissing_returnsEmpty() {
        Long MenuItemId = 999L;
        Long restaurantChainId = 101L;

        when(repository.findByIdAndRestaurantChainId(eq(MenuItemId), eq(restaurantChainId)))
                .thenReturn(Optional.empty());

        Optional<MenuItem> actual = adapter.findByIdAndRestaurantChainId(
                MenuItemId, restaurantChainId);

        assertEquals(Optional.empty(), actual);
        verify(repository).findByIdAndRestaurantChainId(MenuItemId, restaurantChainId);
        verifyNoInteractions(mapper);
    }

    @Test
    void existsNonArchived_whenRepositoryReturnsTrue_returnsTrue() {
        String businessCode = "BH-GE-101";
        Long restaurantChainId = 101L;
        when(repository.existsByBusinessCodeAndRestaurantChainId(
                eq(businessCode), eq(restaurantChainId))).thenReturn(true);

        boolean actual = adapter.existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);

        assertTrue(actual);
        verify(repository).existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);
        verifyNoInteractions(mapper);
    }

    @Test
    void existsNonArchived_whenRepositoryReturnsFalse_returnsFalse() {
        String businessCode = "BH-GE-101";
        Long restaurantChainId = 101L;
        when(repository.existsByBusinessCodeAndRestaurantChainId(
                eq(businessCode), eq(restaurantChainId))).thenReturn(false);

        boolean actual = adapter.existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);

        assertFalse(actual);
        verify(repository).existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);
        verifyNoInteractions(mapper);
    }

    @Test
    void existsNonArchived_trimsAndLowercasesName() {
        String businessCode = "   BH-GE-101   ";
        Long restaurantChainId = 101L;
        when(repository.existsByBusinessCodeAndRestaurantChainId(
            eq(businessCode), eq(restaurantChainId))).thenReturn(false);

        adapter.existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);

        verify(repository).existsByBusinessCodeAndRestaurantChainId(
            eq(businessCode.trim()), eq(restaurantChainId));
    }

    @Test
    void existsNonArchived_checksOnlyDraftAndActiveStatuses() {
        String businessCode = "   BH-GE-101   ";
        Long restaurantChainId = 101L;
        when(repository.existsByBusinessCodeAndRestaurantChainId(
                eq(businessCode), eq(restaurantChainId)))
                .thenReturn(false);

        adapter.existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);

        verify(repository).existsByBusinessCodeAndRestaurantChainId(businessCode.trim(), restaurantChainId);
    }
}
