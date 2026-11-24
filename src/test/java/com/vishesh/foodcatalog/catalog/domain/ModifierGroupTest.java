package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModifierGroupTest {

    @Test
    void validGroup_keepsSelectionBounds() {
        ModifierGroup extras = new ModifierGroup(10L, "  Choose extras  ", 1, 2, 0);

        assertEquals("Choose extras", extras.getGroupName());
        assertEquals(1, extras.getMinimumSelections());
        assertEquals(2, extras.getMaximumSelections());
        assertEquals(null, extras.getId());
    }

    @Test
    void rejectsMaximumBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModifierGroup(10L, "Choose extras", 2, 1, 0));
    }
}
