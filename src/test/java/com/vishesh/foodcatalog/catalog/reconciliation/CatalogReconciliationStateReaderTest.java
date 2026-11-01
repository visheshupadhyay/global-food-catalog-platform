package com.vishesh.foodcatalog.catalog.reconciliation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class CatalogReconciliationStateReaderTest {
    @Mock
    MenuItemRepository repository;
    @Mock
    EffectiveMenuStateClient client;

    private CatalogReconciliationStateReader reader;

    @BeforeEach
    void setUp() {
        reader = new CatalogReconciliationStateReader(repository, client);
    }

    @Test
    void find_sharedItem_buildsCatalogStateAndUsesRequestedOutlet() {
        MenuItem item = item(101L, null);
        when(repository.findById(101L)).thenReturn(Optional.of(item));
        when(client.findSnapshotState(101L, 201L)).thenReturn(Optional.of(snapshot(101L, 201L)));

        ReconciliationStates result = reader.find(101L, 201L).orElseThrow();

        assertEquals(101L, result.catalogState().getMenuItemId());
        assertEquals(201L, result.catalogState().getOutletId());
        assertEquals(101L, result.snapshotState().getMenuItemId());
        verify(repository).findById(101L);
        verify(client).findSnapshotState(101L, 201L);
    }

    @Test
    void find_missingCatalogItem_returnsEmpty() {
        when(repository.findById(101L)).thenReturn(Optional.empty());

        assertTrue(reader.find(101L, 201L).isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    void find_localItemOwnedByAnotherOutlet_returnsEmpty() {
        when(repository.findById(101L)).thenReturn(Optional.of(item(101L, 202L)));

        assertTrue(reader.find(101L, 201L).isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    void find_missingSnapshot_keepsCatalogStateAndUsesNullSnapshot() {
        when(repository.findById(101L)).thenReturn(Optional.of(item(101L, null)));
        when(client.findSnapshotState(101L, 201L)).thenReturn(Optional.empty());

        ReconciliationStates result = reader.find(101L, 201L).orElseThrow();

        assertNotNull(result.catalogState());
        assertNull(result.snapshotState());
    }

    @Test
    void find_rejectsInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> reader.find(0L, 201L));
        assertThrows(IllegalArgumentException.class, () -> reader.find(101L, null));
        verifyNoInteractions(repository, client);
    }

    private MenuItem item(Long id, Long outletId) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Long menuGroupId = outletId == null ? 10L : null;
        return new MenuItem(id, "Burger", BigDecimal.TEN, now, menuGroupId, 20L, outletId,
                "BURGER-101", MenuItemStatus.ACTIVE, true, true, 3L, 1L, now);
    }

    private EffectiveMenuSnapshotState snapshot(Long itemId, Long outletId) {
        return new EffectiveMenuSnapshotState(itemId, outletId, BigDecimal.TEN, true, true, 3L);
    }
}
