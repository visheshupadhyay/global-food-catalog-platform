package com.vishesh.foodcatalog.catalog.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.ModifierGroup;
import com.vishesh.foodcatalog.catalog.domain.ModifierOption;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierGroup;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierOption;
import com.vishesh.foodcatalog.catalog.repository.ModifierGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.ModifierOptionRepository;

/** Builds the complete modifier snapshot shared by all Catalog event paths. */
@Component
public class ModifierConfigurationSnapshotAssembler {
	private final ModifierGroupRepository modifierGroups;
	private final ModifierOptionRepository modifierOptions;

	public ModifierConfigurationSnapshotAssembler(ModifierGroupRepository modifierGroups,
			ModifierOptionRepository modifierOptions) {
		this.modifierGroups = modifierGroups;
		this.modifierOptions = modifierOptions;
	}

	public List<EventModifierGroup> assemble(Long ownerMenuItemId) {
		if (ownerMenuItemId == null || ownerMenuItemId <= 0) {
			throw new IllegalArgumentException("Owner menu item ID must be positive");
		}
		return modifierGroups.findByOwnerMenuItemId(ownerMenuItemId).stream()
				.sorted(Comparator.comparingInt(ModifierGroup::getDisplayOrder))
				.map(group -> new EventModifierGroup(group.getId(), group.getGroupName(), group.getMinimumSelections(),
						group.getMaximumSelections(), group.getDisplayOrder(),
						modifierOptions.findByModifierGroupId(group.getId()).stream()
								.sorted(Comparator.comparingInt(ModifierOption::getDisplayOrder))
								.map(option -> new EventModifierOption(option.getId(), option.getOptionName(),
										option.getPriceDelta(), option.getDisplayOrder()))
								.toList()))
				.toList();
	}
}
