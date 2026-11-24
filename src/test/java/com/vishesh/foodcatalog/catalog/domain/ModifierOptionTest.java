package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ModifierOptionTest {

    @Test
    void nullPriceDelta_becomesFreeOption() {
        ModifierOption option = new ModifierOption(10L, "  Extra cheese  ", null, 0);

        assertEquals("Extra cheese", option.getOptionName());
        assertEquals(BigDecimal.ZERO, option.getPriceDelta());
        assertEquals(null, option.getId());
    }

    @Test
    void rejectsNegativePriceDelta() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModifierOption(10L, "Extra cheese", BigDecimal.ONE.negate(), 0));
    }
}
