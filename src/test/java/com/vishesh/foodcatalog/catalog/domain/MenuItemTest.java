package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class MenuItemTest {
    @Test
    void newSharedMenuItem_startsWithDefaults() {
        MenuItem menuItem = new MenuItem("bh-ge-101",
            "Mc veg", 
            BigDecimal.valueOf(1.5), 
            101L, 
            201L, 
            301L, 
            null);
        
        assertNull(menuItem.getId());
        assertEquals(0L, menuItem.getVersion());
        assertEquals(MenuItemStatus.DRAFT, menuItem.getStatus());
        assertEquals("Mc veg", menuItem.getItemName());
        assertEquals("bh-ge-101", menuItem.getBusinessCode());
        assertNotNull(menuItem.getCreatedAt());
        assertNotNull(menuItem.getUpdatedAt());
    }

    @Test
    void newOutletLocalItem_hasOutletOwnerAndNoMenuGroupOwner() {
        MenuItem menuItem = new MenuItem("bh-ge-101",
            "Mc veg", 
            BigDecimal.valueOf(1.5), 
            101L, 
            null, 
            301L, 
            401L);
        
        assertNull(menuItem.getId());
        assertEquals(0L, menuItem.getVersion());
        assertNull(menuItem.getMenuGroupId());
        assertNotNull(menuItem.getOutletId());
    }

    @Test
    void newSharedMenuItem_hasMenuGroupOwnerAndNoOutletOwner() {
        MenuItem menuItem = new MenuItem("bh-ge-101",
            "Mc veg", 
            BigDecimal.valueOf(1.5), 
            101L, 
            201L, 
            301L, 
            null);
        
        assertNull(menuItem.getId());
        assertEquals(0L, menuItem.getVersion());
        assertNotNull(menuItem.getMenuGroupId());
        assertNull(menuItem.getOutletId());
    }

    @Test
    void newItem_rejectsMissingOwner() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("bh-ge-101",
            "Mc veg", BigDecimal.valueOf(1.5), 101L, null, 301L, null));
    }

    @Test
    void newItem_rejectsTwoOwners() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("bh-ge-101",
            "Mc veg", BigDecimal.valueOf(1.5), 101L, 201L, 301L, 401L));
    }

    @Test
    void newItem_rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("bh-ge-101",
            "Mc veg", BigDecimal.valueOf(-1), 101L, 201L, 301L, null));
    }

    @Test
    void newItem_trimsTextValues() {
        MenuItem menuItem = new MenuItem("  bh-ge-101  ", "  Mc veg  ",
            BigDecimal.valueOf(1.5), 101L, 201L, 301L, null);

        assertEquals("bh-ge-101", menuItem.getBusinessCode());
        assertEquals("Mc veg", menuItem.getItemName());
    }

    @Test
    void draftItem_canBecomeActive() {
        MenuItem item = persisted(MenuItemStatus.DRAFT);

        item.updateStatus(MenuItemStatus.ACTIVE);

        assertEquals(MenuItemStatus.ACTIVE, item.getStatus());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void activeItem_canBecomeHiddenOrArchived() {
        MenuItem hidden = persisted(MenuItemStatus.ACTIVE);
        hidden.updateStatus(MenuItemStatus.HIDDEN);
        assertEquals(MenuItemStatus.HIDDEN, hidden.getStatus());

        MenuItem archived = persisted(MenuItemStatus.ACTIVE);
        archived.updateStatus(MenuItemStatus.ARCHIVED);
        assertEquals(MenuItemStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void hiddenItem_canBecomeActiveOrArchived() {
        MenuItem active = persisted(MenuItemStatus.HIDDEN);
        active.updateStatus(MenuItemStatus.ACTIVE);
        assertEquals(MenuItemStatus.ACTIVE, active.getStatus());

        MenuItem archived = persisted(MenuItemStatus.HIDDEN);
        archived.updateStatus(MenuItemStatus.ARCHIVED);
        assertEquals(MenuItemStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void archivedItem_cannotChangeStatus() {
        MenuItem item = persisted(MenuItemStatus.ARCHIVED);

        assertThrows(IllegalStateException.class,
                () -> item.updateStatus(MenuItemStatus.ACTIVE));
    }

    @Test
    void nullStatus_isRejected() {
        MenuItem item = persisted(MenuItemStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> item.updateStatus(null));
    }

    private MenuItem persisted(MenuItemStatus status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new MenuItem(701L, "Burger", BigDecimal.TEN, now,
                201L, 301L, null, "BURGER-701", status,
                true, true, 1L, 101L, now);
    }
}
