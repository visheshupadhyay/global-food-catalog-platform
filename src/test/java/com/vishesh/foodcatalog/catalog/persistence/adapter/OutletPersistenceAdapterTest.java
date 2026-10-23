package com.vishesh.foodcatalog.catalog.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.persistence.repository.OutletJpaRepository;

@ExtendWith(MockitoExtension.class)
class OutletPersistenceAdapterTest {
    @Mock
    private OutletJpaRepository repository;

    @Test
    void existsByIdAndRestaurantChainId_forwardsBothIds() {
        when(repository.existsByIdAndRestaurantChainId(7L, 42L)).thenReturn(true);

        boolean result = new OutletPersistenceAdapter(repository)
                .existsByIdAndRestaurantChainId(7L, 42L);

        assertTrue(result);
        verify(repository).existsByIdAndRestaurantChainId(7L, 42L);
    }

    @Test
    void existsByIdAndRestaurantChainId_returnsFalseWhenJpaReturnsFalse() {
        when(repository.existsByIdAndRestaurantChainId(7L, 42L)).thenReturn(false);

        boolean result = new OutletPersistenceAdapter(repository)
                .existsByIdAndRestaurantChainId(7L, 42L);

        assertFalse(result);
    }
}
