package com.vishesh.foodcatalog.catalog.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.ModifierConfigurationService;
import com.vishesh.foodcatalog.catalog.application.ModifierGroupConfiguration;
import com.vishesh.foodcatalog.catalog.application.ModifierOptionConfiguration;
import com.vishesh.foodcatalog.catalog.application.ReplaceModifierConfigurationCommand;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;

import jakarta.validation.Valid;

/**
 * HTTP entry point for replacing every modifier group and option of one item.
 */
@RestController
@RequestMapping("/api/v1/restaurant-chains/{restaurantChainId}/menu-items/{menuItemId}/modifier-configuration")
public class ModifierConfigurationController {
	private final ModifierConfigurationService service;

	public ModifierConfigurationController(ModifierConfigurationService service) {
		this.service = service;
	}

	@PutMapping
	public ResponseEntity<ModifierConfigurationResponse> replace(@PathVariable Long restaurantChainId,
			@PathVariable Long menuItemId, @Valid @RequestBody ReplaceModifierConfigurationRequest request) {

		ReplaceModifierConfigurationCommand command = new ReplaceModifierConfigurationCommand(menuItemId,
				restaurantChainId, request.expectedVersion(),
				request.groups().stream().map(this::toGroupConfiguration).toList());

		MenuItem savedMenuItem = service.replaceConfiguration(command);
		return ResponseEntity.ok(new ModifierConfigurationResponse(savedMenuItem.getId(), savedMenuItem.getVersion()));
	}

	private ModifierGroupConfiguration toGroupConfiguration(ModifierGroupRequest group) {
		return new ModifierGroupConfiguration(group.groupId(), group.groupName(), group.minimumSelections(),
				group.maximumSelections(), group.displayOrder(),
				group.options().stream().map(option -> new ModifierOptionConfiguration(option.optionId(),
						option.optionName(), option.priceDelta(), option.displayOrder())).toList());
	}
}
