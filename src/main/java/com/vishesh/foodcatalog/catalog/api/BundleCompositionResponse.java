package com.vishesh.foodcatalog.catalog.api;

import java.util.List;

/** Confirms the saved child list and supplies the next optimistic-lock version. */
public record BundleCompositionResponse(
        Long bundleMenuItemId,
        Long version,
        List<BundleChildResponse> children) {
}
