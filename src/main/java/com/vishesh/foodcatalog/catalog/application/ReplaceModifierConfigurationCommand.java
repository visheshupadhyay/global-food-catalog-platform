package com.vishesh.foodcatalog.catalog.application;

import java.util.List;

/** Complete desired modifier configuration for one menu item. */
public record ReplaceModifierConfigurationCommand(Long ownerMenuItemId, Long restaurantChainId,
        Long expectedVersion, List<ModifierGroupConfiguration> groups) {

}
