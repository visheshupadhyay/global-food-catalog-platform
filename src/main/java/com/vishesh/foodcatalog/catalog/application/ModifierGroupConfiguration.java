package com.vishesh.foodcatalog.catalog.application;

import java.util.List;

/** One group in a complete modifier-configuration replacement. */
public record ModifierGroupConfiguration(Long groupId, String groupName, int minimumSelections,
        int maximumSelections, int displayOrder, List<ModifierOptionConfiguration> options) {

}
