package com.vishesh.foodcatalog.catalog.effective.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

class EffectiveMenuSourceDataTest {

    @Test
    void validSourceData_keepsAllSuppliedValues() {
        EffectiveMenuSourceData data = new EffectiveMenuSourceData(
                201L,
                ChainStatus.ACTIVE,
                List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
                List.of(new OutletOverrideSnapshot(101L, 201L, new BigDecimal("12.50"), false, null)),
                List.of(localItem(301L, 201L, "Berlin Special", true, true, MenuItemStatus.ACTIVE)));

        assertEquals(201L, data.outletId());
        assertEquals(ChainStatus.ACTIVE, data.outletStatus());
        assertEquals(1, data.baseItems().size());
        assertEquals(1, data.overrides().size());
        assertEquals(1, data.localItems().size());
    }

    @Test
    void nullOutletId_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectiveMenuSourceData(null, ChainStatus.ACTIVE, baseItems(), overrides(), localItems()));
    }

    @Test
    void zeroOutletId_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectiveMenuSourceData(0L, ChainStatus.ACTIVE, baseItems(), overrides(), localItems()));
    }

    @Test
    void nullOutletStatus_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectiveMenuSourceData(101L, null, baseItems(), overrides(), localItems()));
    }

    @Test
    void nullBaseItems_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectiveMenuSourceData(101L, ChainStatus.ACTIVE, null, overrides(), localItems()));
    }

    @Test
    void nullOverrides_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectiveMenuSourceData(101L, ChainStatus.ACTIVE, baseItems(), null, localItems()));
    }

    @Test
    void nullLocalItems_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectiveMenuSourceData(101L, ChainStatus.ACTIVE, baseItems(), overrides(), null));
    }

    @Test
    void emptyOverridesAndLocalItems_areAllowed() {
        EffectiveMenuSourceData data = new EffectiveMenuSourceData(
                201L,
                ChainStatus.ACTIVE,
                List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE)),
                List.of(),
                List.of());
        assertEquals(List.of(), data.overrides());
        assertEquals(List.of(), data.localItems());
    }

    @Test
    void constructorCopiesListsAndExposesUnmodifiableLists() {
        List<BaseMenuItemSnapshot> suppliedBaseItems = new ArrayList<>(baseItems());
        EffectiveMenuSourceData data = new EffectiveMenuSourceData(
                201L, ChainStatus.ACTIVE, suppliedBaseItems, overrides(), localItems());

        suppliedBaseItems.clear();

        assertEquals(1, data.baseItems().size());
        assertThrows(UnsupportedOperationException.class, () -> data.baseItems().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.overrides().clear());
        assertThrows(UnsupportedOperationException.class, () -> data.localItems().clear());
    }

    private List<BaseMenuItemSnapshot> baseItems() {
        return List.of(baseItem(101L, "Burger", true, true, MenuItemStatus.ACTIVE));
    }

    private List<OutletOverrideSnapshot> overrides() {
        return List.of(new OutletOverrideSnapshot(101L, 201L, new BigDecimal("12.50"), false, null));
    }

    private List<OutletLocalMenuItemSnapshot> localItems() {
        return List.of(localItem(301L, 201L, "Berlin Special", true, true, MenuItemStatus.ACTIVE));
    }

    private BaseMenuItemSnapshot baseItem(Long id, String name, boolean available, boolean visible,
            MenuItemStatus status) {
        return new BaseMenuItemSnapshot(id, name, new BigDecimal("10.00"), available, visible, status);
    }

    private OutletLocalMenuItemSnapshot localItem(
            Long id, Long outletId, String name, boolean available, boolean visible, MenuItemStatus status) {
        return new OutletLocalMenuItemSnapshot(id, outletId, name, new BigDecimal("9.00"), available, visible, status);
    }
}
