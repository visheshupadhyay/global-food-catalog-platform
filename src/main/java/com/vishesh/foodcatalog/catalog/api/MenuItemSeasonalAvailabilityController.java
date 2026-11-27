package com.vishesh.foodcatalog.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.vishesh.foodcatalog.catalog.application.MenuItemSeasonalAvailabilityService;

/**
 * HTTP API for replacing and reading a menu item's base or outlet seasonal
 * rule.
 */
@RestController
@RequestMapping("/api/v1/menu-items/{menuItemId}/seasonal-availability")
public class MenuItemSeasonalAvailabilityController {
	private final MenuItemSeasonalAvailabilityService service;

	public MenuItemSeasonalAvailabilityController(MenuItemSeasonalAvailabilityService service) {
		this.service = service;
	}

	@PutMapping
	public Object replace(@PathVariable Long menuItemId, @RequestBody SeasonalAvailabilityRequest request) {
		return service.save(menuItemId, request);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clear(@PathVariable Long menuItemId, @RequestParam(required = false) Long outletId,
			@RequestParam(required = false) Long expectedVersion) {
		service.save(menuItemId, new SeasonalAvailabilityRequest(outletId, null, null, expectedVersion));
	}

	@GetMapping
	public Object get(@PathVariable Long menuItemId, @RequestParam(required = false) Long outletId) {
		return service.find(menuItemId, outletId)
				.orElseThrow(() -> new IllegalArgumentException("Seasonal availability was not found"));
	}
}
