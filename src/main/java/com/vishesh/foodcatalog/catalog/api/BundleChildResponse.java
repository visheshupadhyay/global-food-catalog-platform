package com.vishesh.foodcatalog.catalog.api;

/** One stored child relationship returned after a successful replacement. */
public record BundleChildResponse(
        Long childMenuItemId,
        int quantity,
        int displayOrder) {
}
