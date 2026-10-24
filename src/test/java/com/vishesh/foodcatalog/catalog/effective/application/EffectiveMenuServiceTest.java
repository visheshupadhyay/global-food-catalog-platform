package com.vishesh.foodcatalog.catalog.effective.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.application.OutletNotFoundException;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;

@ExtendWith(MockitoExtension.class)
class EffectiveMenuServiceTest {

    private static final Long OUTLET_ID = 201L;

    @Mock
    private EffectiveMenuDataProvider effectiveMenuDataProvider;

    @Mock
    private EffectiveMenuAssembler effectiveMenuAssembler;

    private EffectiveMenuService service;

    @BeforeEach
    void setUp() {
        service = new EffectiveMenuService(effectiveMenuDataProvider, effectiveMenuAssembler);
    }

    @Test
    void getEffectiveMenu_rejectsNullOutletIdBeforeProviderCall() {
        assertThrows(IllegalArgumentException.class, () -> service.getEffectiveMenu(null));

        verifyNoInteractions(effectiveMenuDataProvider, effectiveMenuAssembler);
    }

    @Test
    void getEffectiveMenu_rejectsZeroOutletIdBeforeProviderCall() {
        assertThrows(IllegalArgumentException.class, () -> service.getEffectiveMenu(0L));

        verifyNoInteractions(effectiveMenuDataProvider, effectiveMenuAssembler);
    }

    @Test
    void getEffectiveMenu_whenOutletIsMissing_throwsNotFound() {
        when(effectiveMenuDataProvider.findByOutletId(OUTLET_ID)).thenReturn(Optional.empty());

        assertThrows(OutletNotFoundException.class, () -> service.getEffectiveMenu(OUTLET_ID));

        verify(effectiveMenuDataProvider).findByOutletId(OUTLET_ID);
        verifyNoInteractions(effectiveMenuAssembler);
    }

    @Test
    void getEffectiveMenu_whenOutletIsDraft_throwsNotFoundWithoutAssembling() {
        when(effectiveMenuDataProvider.findByOutletId(OUTLET_ID))
                .thenReturn(Optional.of(sourceData(ChainStatus.DRAFT)));

        assertThrows(OutletNotFoundException.class, () -> service.getEffectiveMenu(OUTLET_ID));

        verify(effectiveMenuDataProvider).findByOutletId(OUTLET_ID);
        verifyNoInteractions(effectiveMenuAssembler);
    }

    @Test
    void getEffectiveMenu_whenOutletIsArchived_throwsNotFoundWithoutAssembling() {
        when(effectiveMenuDataProvider.findByOutletId(OUTLET_ID))
                .thenReturn(Optional.of(sourceData(ChainStatus.ARCHIVED)));

        assertThrows(OutletNotFoundException.class, () -> service.getEffectiveMenu(OUTLET_ID));

        verify(effectiveMenuDataProvider).findByOutletId(OUTLET_ID);
        verifyNoInteractions(effectiveMenuAssembler);
    }

    @Test
    void getEffectiveMenu_whenOutletIsActive_assemblesAndReturnsMenu() {
        EffectiveMenuSourceData sourceData = sourceData(ChainStatus.ACTIVE);
        List<EffectiveMenuItem> expectedMenu = List.of(new EffectiveMenuItem(
                101L, OUTLET_ID, "Burger", new BigDecimal("12.50"), true, true));
        when(effectiveMenuDataProvider.findByOutletId(OUTLET_ID)).thenReturn(Optional.of(sourceData));
        when(effectiveMenuAssembler.assemble(
                OUTLET_ID,
                sourceData.baseItems(),
                sourceData.overrides(),
                sourceData.localItems())).thenReturn(expectedMenu);

        List<EffectiveMenuItem> actualMenu = service.getEffectiveMenu(OUTLET_ID);

        assertEquals(expectedMenu, actualMenu);
        verify(effectiveMenuDataProvider).findByOutletId(OUTLET_ID);
        verify(effectiveMenuAssembler).assemble(
                OUTLET_ID,
                sourceData.baseItems(),
                sourceData.overrides(),
                sourceData.localItems());
    }

    private EffectiveMenuSourceData sourceData(ChainStatus outletStatus) {
        return new EffectiveMenuSourceData(
                OUTLET_ID,
                outletStatus,
                List.of(new BaseMenuItemSnapshot(
                        101L, "Burger", new BigDecimal("10.00"), true, true, MenuItemStatus.ACTIVE)),
                List.of(new OutletOverrideSnapshot(101L, OUTLET_ID, new BigDecimal("12.50"), null, null)),
                List.of(new OutletLocalMenuItemSnapshot(
                        301L, OUTLET_ID, "Berlin Special", new BigDecimal("9.00"), true, true,
                        MenuItemStatus.ACTIVE)));
    }
}
