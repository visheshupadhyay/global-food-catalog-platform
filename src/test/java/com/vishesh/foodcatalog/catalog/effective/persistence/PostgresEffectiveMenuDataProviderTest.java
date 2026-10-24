package com.vishesh.foodcatalog.catalog.effective.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.BaseMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletLocalMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletOverrideProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletStatusProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.repository.EffectiveMenuJpaReadRepository;

@ExtendWith(MockitoExtension.class)
class PostgresEffectiveMenuDataProviderTest {

    private static final Long OUTLET_ID = 201L;

    @Mock
    private EffectiveMenuJpaReadRepository repository;

    @Mock
    private OutletStatusProjection outletStatusProjection;

    @Mock
    private BaseMenuItemProjection baseProjection;

    @Mock
    private OutletOverrideProjection overrideProjection;

    @Mock
    private OutletLocalMenuItemProjection localProjection;

    private PostgresEffectiveMenuDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PostgresEffectiveMenuDataProvider(repository);
    }

    @Test
    void unknownOutlet_returnsEmpty() {
        when(repository.findOutletStatusByOutletId(OUTLET_ID)).thenReturn(Optional.empty());

        assertTrue(provider.findByOutletId(OUTLET_ID).isEmpty());

        verify(repository).findOutletStatusByOutletId(OUTLET_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void existingOutlet_mapsItsStatus() {
        when(repository.findOutletStatusByOutletId(OUTLET_ID)).thenReturn(Optional.of(outletStatusProjection));
        when(outletStatusProjection.getOutletStatus()).thenReturn("ACTIVE");
        stubEmptyMenuData();

        EffectiveMenuSourceData result = provider.findByOutletId(OUTLET_ID).orElseThrow();

        assertEquals(OUTLET_ID, result.outletId());
        assertEquals(ChainStatus.ACTIVE, result.outletStatus());
    }

    @Test
    void baseProjection_becomesBaseMenuItemSnapshot() {
        stubExistingActiveOutlet();
        when(baseProjection.getMenuItemId()).thenReturn(101L);
        when(baseProjection.getItemName()).thenReturn("Classic Burger");
        when(baseProjection.getPrice()).thenReturn(new BigDecimal("10.00"));
        when(baseProjection.getAvailable()).thenReturn(true);
        when(baseProjection.getVisible()).thenReturn(true);
        when(baseProjection.getStatus()).thenReturn("ACTIVE");
        stubEmptyOverridesAndLocalItems();
        when(repository.findBaseItemsByOutletId(OUTLET_ID)).thenReturn(List.of(baseProjection));

        BaseMenuItemSnapshot item = provider.findByOutletId(OUTLET_ID).orElseThrow().baseItems().get(0);

        assertEquals(101L, item.getMenuItemId());
        assertEquals("Classic Burger", item.getItemName());
        assertEquals(new BigDecimal("10.00"), item.getPrice());
        assertEquals(MenuItemStatus.ACTIVE, item.getStatus());
    }

    @Test
    void overrideProjection_receivesRequestedOutletId() {
        stubExistingActiveOutlet();
        when(overrideProjection.getMenuItemId()).thenReturn(101L);
        when(overrideProjection.getPriceOverride()).thenReturn(new BigDecimal("12.00"));
        when(overrideProjection.getAvailableOverride()).thenReturn(null);
        when(overrideProjection.getVisibleOverride()).thenReturn(null);
        stubEmptyBaseAndLocalItems();
        when(repository.findOverridesByOutletId(OUTLET_ID)).thenReturn(List.of(overrideProjection));

        OutletOverrideSnapshot override = provider.findByOutletId(OUTLET_ID).orElseThrow().overrides().get(0);

        assertEquals(OUTLET_ID, override.getOutletId());
        assertEquals(101L, override.getMenuItemId());
    }

    @Test
    void localProjection_becomesOutletLocalMenuItemSnapshot() {
        stubExistingActiveOutlet();
        when(localProjection.getMenuItemId()).thenReturn(301L);
        when(localProjection.getOutletId()).thenReturn(OUTLET_ID);
        when(localProjection.getItemName()).thenReturn("Berlin Special");
        when(localProjection.getPrice()).thenReturn(new BigDecimal("9.00"));
        when(localProjection.getAvailable()).thenReturn(true);
        when(localProjection.getVisible()).thenReturn(true);
        when(localProjection.getStatus()).thenReturn("ACTIVE");
        stubEmptyBaseAndOverrides();
        when(repository.findLocalItemsByOutletId(OUTLET_ID)).thenReturn(List.of(localProjection));

        OutletLocalMenuItemSnapshot item = provider.findByOutletId(OUTLET_ID).orElseThrow().localItems().get(0);

        assertEquals(301L, item.getMenuItemId());
        assertEquals(OUTLET_ID, item.getOutletId());
        assertEquals(MenuItemStatus.ACTIVE, item.getStatus());
    }

    @Test
    void allRepositoryMethods_receiveSameOutletId() {
        stubExistingActiveOutlet();
        stubEmptyMenuData();

        provider.findByOutletId(OUTLET_ID);

        verify(repository).findOutletStatusByOutletId(OUTLET_ID);
        verify(repository).findBaseItemsByOutletId(OUTLET_ID);
        verify(repository).findOverridesByOutletId(OUTLET_ID);
        verify(repository).findLocalItemsByOutletId(OUTLET_ID);
    }

    @Test
    void databaseStatusStrings_becomeCorrectEnums() {
        stubExistingActiveOutlet();
        when(localProjection.getMenuItemId()).thenReturn(301L);
        when(localProjection.getOutletId()).thenReturn(OUTLET_ID);
        when(localProjection.getItemName()).thenReturn("Special");
        when(localProjection.getPrice()).thenReturn(new BigDecimal("5.00"));
        when(localProjection.getAvailable()).thenReturn(true);
        when(localProjection.getVisible()).thenReturn(true);
        when(localProjection.getStatus()).thenReturn("HIDDEN");
        stubEmptyBaseAndOverrides();
        when(repository.findLocalItemsByOutletId(OUTLET_ID)).thenReturn(List.of(localProjection));

        EffectiveMenuSourceData result = provider.findByOutletId(OUTLET_ID).orElseThrow();

        assertEquals(ChainStatus.ACTIVE, result.outletStatus());
        assertEquals(MenuItemStatus.HIDDEN, result.localItems().get(0).getStatus());
    }

    private void stubExistingActiveOutlet() {
        when(repository.findOutletStatusByOutletId(OUTLET_ID)).thenReturn(Optional.of(outletStatusProjection));
        when(outletStatusProjection.getOutletStatus()).thenReturn("ACTIVE");
    }

    private void stubEmptyMenuData() {
        stubEmptyBaseAndOverrides();
        stubEmptyLocalItems();
    }

    private void stubEmptyBaseAndOverrides() {
        when(repository.findBaseItemsByOutletId(OUTLET_ID)).thenReturn(List.of());
        when(repository.findOverridesByOutletId(OUTLET_ID)).thenReturn(List.of());
    }

    private void stubEmptyOverridesAndLocalItems() {
        when(repository.findOverridesByOutletId(OUTLET_ID)).thenReturn(List.of());
        stubEmptyLocalItems();
    }

    private void stubEmptyBaseAndLocalItems() {
        when(repository.findBaseItemsByOutletId(OUTLET_ID)).thenReturn(List.of());
        stubEmptyLocalItems();
    }

    private void stubEmptyLocalItems() {
        when(repository.findLocalItemsByOutletId(OUTLET_ID)).thenReturn(List.of());
    }
}
