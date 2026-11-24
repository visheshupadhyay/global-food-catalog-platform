package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;

/** Immutable option snapshot carried inside a modifier-configuration event. */
public record EventModifierOption(
        Long optionId,
        String optionName,
        BigDecimal priceDelta,
        int displayOrder) {

    public EventModifierOption {
        if (optionId == null || optionId <= 0) {
            throw new IllegalArgumentException("Modifier option ID must be positive");
        }
        if (optionName == null || optionName.isBlank()) {
            throw new IllegalArgumentException("Modifier option name cannot be blank");
        }
        if (priceDelta == null || priceDelta.signum() < 0) {
            throw new IllegalArgumentException("Modifier option price delta cannot be negative or missing");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Modifier option display order cannot be negative");
        }
        optionName = optionName.trim();
    }
}
