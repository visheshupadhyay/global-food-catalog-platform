package com.vishesh.foodcatalog.catalog.application;

import java.math.BigDecimal;

/**
 * CreateModifierOptionCommand
 */
public record CreateModifierOptionCommand(String optionName,
        BigDecimal priceDelta,
        int displayOrder) {

}
