package com.vishesh.foodcatalog.catalog.application;

import java.util.List;

public record CreateModifierGroupCommand(Long ownerMenuItemId,
        Long restaurantChainId,
        String groupName,
        int minimumSelections,
        int maximumSelections,
        int displayOrder,
        Long expectedVersion,
        List<CreateModifierOptionCommand> options) {

}
