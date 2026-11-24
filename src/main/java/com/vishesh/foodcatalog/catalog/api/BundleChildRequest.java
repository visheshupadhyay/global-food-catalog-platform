package com.vishesh.foodcatalog.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/** One child relationship in the desired, complete bundle structure. */
public record BundleChildRequest(
        @Positive Long childMenuItemId,
        @Positive int quantity,
        @Min(0) int displayOrder) {
}
