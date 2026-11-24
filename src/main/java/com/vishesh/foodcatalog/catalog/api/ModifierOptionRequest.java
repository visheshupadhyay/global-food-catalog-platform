package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** One atomic option supplied inside a modifier group replacement. */
public record ModifierOptionRequest(
                @Positive Long optionId,
                @NotBlank String optionName,
                @DecimalMin(value = "0.0", inclusive = true) BigDecimal priceDelta,
                @Min(0) int displayOrder) {
}
