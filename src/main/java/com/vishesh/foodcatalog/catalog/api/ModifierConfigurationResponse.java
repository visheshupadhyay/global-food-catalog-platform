package com.vishesh.foodcatalog.catalog.api;

/** Small acknowledgement returned for both changed and no-op replacements. */
public record ModifierConfigurationResponse(Long menuItemId, Long version) {
}
