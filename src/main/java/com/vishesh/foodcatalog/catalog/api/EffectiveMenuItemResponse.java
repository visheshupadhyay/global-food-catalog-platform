package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;

public record EffectiveMenuItemResponse(
        Long menuItemId,
        Long outletId,
        String itemName,
        BigDecimal itemPrice,
        Boolean available,
        Boolean visible) {

}
