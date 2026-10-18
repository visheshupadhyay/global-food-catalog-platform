package com.vishesh.foodcatalog.catalog.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.persistence.entity.RestaurantChainEntity;
import com.vishesh.foodcatalog.catalog.persistence.mapper.RestaurantChainMapper;
import com.vishesh.foodcatalog.catalog.persistence.repository.RestaurantChainJpaRepository;

public class RestaurantChainPersistenceAdapterTest {
    @Mock
    private RestaurantChainJpaRepository repository;

    @Mock
    private RestaurantChainMapper mapper;

    @InjectMocks
    private RestaurantChainPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_returnsMappedSavedDomain() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        RestaurantChain inputRestaurantChain = new RestaurantChain(
                "BURGER-KING-001",
                "BURGER-KING",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                null);

        RestaurantChainEntity entity = new RestaurantChainEntity(
                null,
                "BURGER-KING-001",
                "BURGER-KING",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt);

        RestaurantChainEntity savedEntity = new RestaurantChainEntity(
                101L,
                "BURGER-KING-001",
                "BURGER-KING",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt);

        RestaurantChain expectedRestaurantChain = new RestaurantChain(
                "BURGER-KING-001",
                "BURGER-KING",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                101L);

        

        when(mapper.toEntity(inputRestaurantChain)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(expectedRestaurantChain);

        RestaurantChain actualRestaurantChain = adapter.save(inputRestaurantChain);

        assertEquals(expectedRestaurantChain.getId(), actualRestaurantChain.getId());
        assertEquals(expectedRestaurantChain.getBusinessCode(), actualRestaurantChain.getBusinessCode());
        assertEquals(expectedRestaurantChain.getDisplayName(), actualRestaurantChain.getDisplayName());
        assertEquals(expectedRestaurantChain.getStatus(), actualRestaurantChain.getStatus());
        assertEquals(expectedRestaurantChain.getCreatedAt(), actualRestaurantChain.getCreatedAt());
        assertEquals(expectedRestaurantChain.getUpdatedAt(), actualRestaurantChain.getUpdatedAt());
        verify(mapper).toEntity(inputRestaurantChain);
        verify(repository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findById_whenFound_returnsMappedDomain() {
        Long id = 101L;
        RestaurantChainEntity entity = org.mockito.Mockito.mock(RestaurantChainEntity.class);
        RestaurantChain expected = org.mockito.Mockito.mock(RestaurantChain.class);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<RestaurantChain> actual = adapter.findById(id);

        assertEquals(Optional.of(expected), actual);
        verify(repository).findById(id);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        Long id = 999L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<RestaurantChain> actual = adapter.findById(id);

        assertEquals(Optional.empty(), actual);
        verify(repository).findById(id);
        verifyNoInteractions(mapper);
    }

    @Test
    void findByBusinessCode_whenFound_returnsMappedDomain() {
        String businessCode = "DH-GE";
        RestaurantChainEntity entity = org.mockito.Mockito.mock(RestaurantChainEntity.class);
        RestaurantChain expected = org.mockito.Mockito.mock(RestaurantChain.class);

        when(repository.findByBusinessCode(businessCode))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<RestaurantChain> actual = adapter.findByBusinessCode(businessCode);

        assertEquals(Optional.of(expected), actual);
        verify(repository).findByBusinessCode(businessCode);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findByBusinessCode_whenMissing_returnsEmpty() {
        String businessCode = "UNKNOWN";

        when(repository.findByBusinessCode(businessCode))
                .thenReturn(Optional.empty());

        Optional<RestaurantChain> actual = adapter.findByBusinessCode(businessCode);

        assertEquals(Optional.empty(), actual);
        verify(repository).findByBusinessCode(businessCode);
        verifyNoInteractions(mapper);
    }

    @Test
    void existsByBusinessCode_whenPresent_returnsTrue() {
        String businessCode = "DH-GE";

        when(repository.existsByBusinessCode(businessCode))
                .thenReturn(true);

        boolean actual = adapter.existsByBusinessCode(businessCode);

        assertTrue(actual);
        verify(repository).existsByBusinessCode(businessCode);
        verifyNoInteractions(mapper);
    }

    @Test
    void existsByBusinessCode_whenAbsent_returnsFalse() {
        String businessCode = "UNKNOWN";

        when(repository.existsByBusinessCode(businessCode))
                .thenReturn(false);

        boolean actual = adapter.existsByBusinessCode(businessCode);

        assertFalse(actual);
        verify(repository).existsByBusinessCode(businessCode);
        verifyNoInteractions(mapper);
    }
}
