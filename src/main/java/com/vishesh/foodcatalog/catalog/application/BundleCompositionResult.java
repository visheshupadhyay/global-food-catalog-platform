package com.vishesh.foodcatalog.catalog.application;

import java.util.List;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;

/**
 * Result of one complete bundle-child replacement. The returned version is
 * what an administrator must send with their next optimistic-locking update.
 */
public record BundleCompositionResult(
        Long bundleMenuItemId,
        Long bundleVersion,
        List<BundleChild> children) {

    public BundleCompositionResult {
        if (bundleMenuItemId == null || bundleMenuItemId <= 0) {
            throw new IllegalArgumentException("Bundle menu item ID must be greater than zero");
        }
        if (bundleVersion == null || bundleVersion < 0) {
            throw new IllegalArgumentException("Bundle version must be zero or greater");
        }
        children = List.copyOf(children);
    }
}
