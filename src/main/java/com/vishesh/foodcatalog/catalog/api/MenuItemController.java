package com.vishesh.foodcatalog.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.MenuItemService;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;

import jakarta.validation.Valid;

/** Receives HTTP requests for shared and outlet-local menu items. */
@RequestMapping("/api/v1/restaurant-chains/{restaurantChainId}")
@RestController
public class MenuItemController {
	private final MenuItemService service;

	public MenuItemController(MenuItemService service) {
		this.service = service;
	}

	@PostMapping("/menu-items")
	public ResponseEntity<CreateSharedMenuItemResponse> createSharedMenuItem(@PathVariable Long restaurantChainId,
			@Valid @RequestBody CreateSharedMenuItemRequest request) {
		// Create an item inherited by outlets through a menu group.

		// The controller is the HTTP front door; business rules stay in the service.
		MenuItem menuItem = createMenuItem(request.getBusinessCode(), request.getItemName(), request.getItemPrice(),
				restaurantChainId, request.getMenuGroupId(), request.getMenuCategoryId(), null,
				request.getMenuItemKind());

		CreateSharedMenuItemResponse response = toSharedResponse(menuItem);

		// Tell the caller where the new item can be found later.
		URI location = URI.create("/api/v1/restaurant-chains/" + restaurantChainId + "/menu-items/" + response.id());

		return ResponseEntity.created(location).body(response);
	}

	@PostMapping("/outlets/{outletId}/menu-items")
	public ResponseEntity<CreateOutletMenuItemResponse> createOutletMenuItem(@PathVariable Long restaurantChainId,
			@PathVariable Long outletId, @Valid @RequestBody CreateOutletMenuItemRequest request) {
		// Create an item that belongs only to this outlet.

		MenuItem menuItem = createMenuItem(request.getBusinessCode(), request.getItemName(), request.getItemPrice(),
				restaurantChainId, null, request.getMenuCategoryId(), outletId, request.getMenuItemKind());

		CreateOutletMenuItemResponse response = toOutletResponse(menuItem);

		URI location = URI.create("/api/v1/restaurant-chains/" + restaurantChainId + "/outlets/" + outletId
				+ "/menu-items/" + response.id());

		return ResponseEntity.created(location).body(response);
	}

	@PatchMapping("/menu-items/{menuItemId}")
	public ResponseEntity<CreateSharedMenuItemResponse> updateMenuItem(@PathVariable Long restaurantChainId,
			@PathVariable Long menuItemId, @Valid @RequestBody MenuItemUpdateRequest request) {

		MenuItem menuItem = service.updateMenuItem(menuItemId, restaurantChainId, request);
		return ResponseEntity.ok(toSharedResponse(menuItem));
	}

	/** Maps a shared item to the JSON shape used by shared-item endpoints. */
	private CreateSharedMenuItemResponse toSharedResponse(MenuItem menuItem) {
		return new CreateSharedMenuItemResponse(menuItem.getId(), menuItem.getBusinessCode(), menuItem.getItemName(),
				menuItem.getItemPrice(), menuItem.getRestaurantChainId(), menuItem.getStatus(), menuItem.getVersion(),
				menuItem.getCreatedAt(), menuItem.getUpdatedAt(), menuItem.getOutletId(), menuItem.getMenuGroupId(),
				menuItem.getMenuCategoryId(), menuItem.getMenuItemKind());
	}

	/** Maps an outlet-local item to the JSON shape used by local-item endpoints. */
	private CreateOutletMenuItemResponse toOutletResponse(MenuItem menuItem) {
		return new CreateOutletMenuItemResponse(menuItem.getId(), menuItem.getBusinessCode(), menuItem.getItemName(),
				menuItem.getItemPrice(), menuItem.getRestaurantChainId(), menuItem.getStatus(), menuItem.getVersion(),
				menuItem.getCreatedAt(), menuItem.getUpdatedAt(), menuItem.getOutletId(), menuItem.getMenuCategoryId(),
				menuItem.getMenuItemKind());
	}

	private MenuItem createMenuItem(String businessCode, String itemName, java.math.BigDecimal itemPrice,
			Long restaurantChainId, Long menuGroupId, Long menuCategoryId, Long outletId, MenuItemKind menuItemKind) {

		// PRODUCT was the only kind before bundles existed. Use the original
		// method so older clients and callers retain exactly the same path.
		if (menuItemKind == null || menuItemKind == MenuItemKind.PRODUCT) {
			return service.createMenuItem(businessCode, itemName, itemPrice, restaurantChainId, menuGroupId,
					menuCategoryId, outletId);
		}
		return service.createMenuItem(businessCode, itemName, itemPrice, restaurantChainId, menuGroupId, menuCategoryId,
				outletId, menuItemKind);
	}
}
