package com.vishesh.foodcatalog.catalog.application;

import java.math.BigDecimal;

/** One atomic option in a complete modifier-configuration replacement. */
public record ModifierOptionConfiguration(Long optionId, String optionName, BigDecimal priceDelta, int displayOrder) {

}
