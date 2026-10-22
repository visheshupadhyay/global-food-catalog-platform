package com.vishesh.foodcatalog.catalog.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;
import com.vishesh.foodcatalog.catalog.persistence.mapper.MenuGroupMapper;
import com.vishesh.foodcatalog.catalog.persistence.repository.MenuGroupJpaRepository;

public class MenuGroupPersistenceAdapterTest {
    @Mock
    private MenuGroupJpaRepository repository;

    @Mock
    private MenuGroupMapper mapper;

    @InjectMocks
    private MenuGroupPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_returnsMappedSavedDomain() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        // inputMenuGroup
        // ↓ mapper.toEntity(...)
        // entity // unsaved entity, id = null
        // ↓ repository.save(entity)
        // savedEntity // saved entity, id = 201
        // ↓ mapper.toDomain(savedEntity)
        // expectedMenuGroup // persisted domain object, id = 201
        MenuGroup inputMenuGroup = new MenuGroup(
                101L,
                "BURGER-KING");

        MenuGroupEntity entity = new MenuGroupEntity(
                101L,
                "BURGER-KING");

        MenuGroupEntity savedEntity = new MenuGroupEntity(
                101L,
                "BURGER-KING",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                201L);

        MenuGroup expectedMenuGroup = new MenuGroup(
                101L,
                "BURGER-KING",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                3L,
                201L);

        when(mapper.toEntity(inputMenuGroup)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(expectedMenuGroup);

        MenuGroup actualMenuGroup = adapter.save(inputMenuGroup);

        assertEquals(expectedMenuGroup.getId(), actualMenuGroup.getId());
        assertEquals(expectedMenuGroup.getDisplayName(), actualMenuGroup.getDisplayName());
        assertEquals(expectedMenuGroup.getStatus(), actualMenuGroup.getStatus());
        assertEquals(expectedMenuGroup.getCreatedAt(), actualMenuGroup.getCreatedAt());
        assertEquals(expectedMenuGroup.getUpdatedAt(), actualMenuGroup.getUpdatedAt());
        verify(mapper).toEntity(inputMenuGroup);
        verify(repository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findByIdAndRestaurantChainId_whenFound_forwardsBothIdsAndMapsDomain() {
        Long menuGroupId = 42L;
        Long restaurantChainId = 101L;
        MenuGroupEntity entity = mock(MenuGroupEntity.class);
        MenuGroup expected = mock(MenuGroup.class);

        when(repository.findByIdAndRestaurantChainId(menuGroupId, restaurantChainId))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<MenuGroup> actual = adapter.findByIdAndRestaurantChainId(
                menuGroupId, restaurantChainId);

        assertEquals(Optional.of(expected), actual);
        verify(repository).findByIdAndRestaurantChainId(menuGroupId, restaurantChainId);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findByIdAndRestaurantChainId_whenMissing_returnsEmpty() {
        Long menuGroupId = 999L;
        Long restaurantChainId = 101L;

        when(repository.findByIdAndRestaurantChainId(menuGroupId, restaurantChainId))
                .thenReturn(Optional.empty());

        Optional<MenuGroup> actual = adapter.findByIdAndRestaurantChainId(
                menuGroupId, restaurantChainId);

        assertEquals(Optional.empty(), actual);
        verify(repository).findByIdAndRestaurantChainId(menuGroupId, restaurantChainId);
        verifyNoInteractions(mapper);
    }

    @Test
    void existsNonArchived_whenRepositoryReturnsTrue_returnsTrue() {
        when(repository.existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), anyList())).thenReturn(true);

        boolean actual = adapter.existsNonArchivedByRestaurantChainIdAndDisplayName(101L, "BURGERS");

        assertTrue(actual);
        verify(repository).existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), anyList());
        verifyNoInteractions(mapper);
    }

    @Test
    void existsNonArchived_whenRepositoryReturnsFalse_returnsFalse() {
        when(repository.existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), anyList())).thenReturn(false);

        boolean actual = adapter.existsNonArchivedByRestaurantChainIdAndDisplayName(101L, "BURGERS");

        assertFalse(actual);
        verify(repository).existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), anyList());
        verifyNoInteractions(mapper);
    }

    @Test
    void existsNonArchived_trimsAndLowercasesName() {
        when(repository.existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), anyList())).thenReturn(false);

        adapter.existsNonArchivedByRestaurantChainIdAndDisplayName(101L, "  BURGERS  ");

        verify(repository).existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), anyList());
    }

    @Test
    void existsNonArchived_checksOnlyDraftAndActiveStatuses() {
        when(repository.existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), argThat(statuses -> statuses.size() == 2
                        && statuses.contains(ChainStatus.DRAFT)
                        && statuses.contains(ChainStatus.ACTIVE)
                        && !statuses.contains(ChainStatus.ARCHIVED))))
                .thenReturn(false);

        adapter.existsNonArchivedByRestaurantChainIdAndDisplayName(101L, "burgers");

        verify(repository).existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                eq(101L), eq("burgers"), argThat(statuses -> statuses.size() == 2
                        && statuses.contains(ChainStatus.DRAFT)
                        && statuses.contains(ChainStatus.ACTIVE)
                        && !statuses.contains(ChainStatus.ARCHIVED)));
    }
}
