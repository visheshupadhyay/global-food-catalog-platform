package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

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
}
