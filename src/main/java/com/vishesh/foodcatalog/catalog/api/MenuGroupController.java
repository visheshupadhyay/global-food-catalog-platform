package com.vishesh.foodcatalog.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.MenuGroupService;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;

import jakarta.validation.Valid;

/** Receives HTTP requests about shared menu groups. */
@RequestMapping("/api/v1/restaurant-chains/{restaurantChainId}/menu-groups")
@RestController
public class MenuGroupController {
	private final MenuGroupService service;

	public MenuGroupController(MenuGroupService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<MenuGroupCreatedResponse> createMenuGroup(@PathVariable Long restaurantChainId,
			@Valid @RequestBody CreateMenuGroupRequest request) {

		// The controller is the HTTP front door; business rules stay in the service.
		MenuGroup menuGroup = service.createMenuGroup(restaurantChainId, request.getDisplayName());
		MenuGroupCreatedResponse response = toResponse(menuGroup);

		// Tell the caller where the new menu group can be found later.
		URI location = URI.create("/api/v1/restaurant-chains/" + restaurantChainId + "/menu-groups/" + response.id());

		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MenuGroupCreatedResponse> getMenuGroup(
			@PathVariable("restaurantChainId") Long restaurantChainId, @PathVariable("id") Long id) {

		// The controller is the HTTP front door; business rules stay in the service.
		MenuGroup menuGroup = service.findMenuGroupById(restaurantChainId, id);
		return ResponseEntity.ok(toResponse(menuGroup));
	}

	@PatchMapping("/{menuGroupId}")
	public ResponseEntity<MenuGroupCreatedResponse> updateMenuGroup(@PathVariable("menuGroupId") Long menuGroupId,
			@PathVariable("restaurantChainId") Long restaurantChainId,
			@Valid @RequestBody MenuGroupUpdateRequest request) {

		// The controller is the HTTP front door; business rules stay in the service.
		MenuGroup menuGroup = service.updateMenuGroup(restaurantChainId, menuGroupId, request);
		return ResponseEntity.ok(toResponse(menuGroup));
	}

	/** Maps the domain model to the stable JSON shape exposed by this API. */
	private MenuGroupCreatedResponse toResponse(MenuGroup menuGroup) {
		return new MenuGroupCreatedResponse(menuGroup.getId(), menuGroup.getRestaurantChainId(),
				menuGroup.getDisplayName(), menuGroup.getStatus(), menuGroup.getVersion());
	}

}
