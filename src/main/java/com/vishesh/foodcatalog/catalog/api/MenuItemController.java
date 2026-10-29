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
    public ResponseEntity<CreateSharedMenuItemResponse> createSharedMenuItem(
            @PathVariable Long restaurantChainId,
            @Valid @RequestBody CreateSharedMenuItemRequest request) {
        // Create an item inherited by outlets through a menu group.

        // The controller is the HTTP front door; business rules stay in the service.
        MenuItem menuItem = service.createMenuItem(request.getBusinessCode(),
                request.getItemName(),
                request.getItemPrice(),
                restaurantChainId,
                request.getMenuGroupId(),
                request.getMenuCategoryId(),
                null);

        CreateSharedMenuItemResponse response = new CreateSharedMenuItemResponse(
                menuItem.getId(),
                menuItem.getBusinessCode(),
                menuItem.getItemName(),
                menuItem.getItemPrice(),
                menuItem.getRestaurantChainId(),
                menuItem.getStatus(),
                menuItem.getVersion(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt(),
                menuItem.getOutletId(),
                menuItem.getMenuGroupId(),
                menuItem.getMenuCategoryId());

        // Tell the caller where the new item can be found later.
        URI location = URI.create("/api/v1/restaurant-chains/" + restaurantChainId
                + "/menu-items/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/outlets/{outletId}/menu-items")
    public ResponseEntity<CreateOutletMenuItemResponse> createOutletMenuItem(
            @PathVariable Long restaurantChainId,
            @PathVariable Long outletId,
            @Valid @RequestBody CreateOutletMenuItemRequest request) {
        // Create an item that belongs only to this outlet.

        MenuItem menuItem = service.createMenuItem(request.getBusinessCode(),
                request.getItemName(),
                request.getItemPrice(),
                restaurantChainId,
                null,
                request.getMenuCategoryId(),
                outletId);

        CreateOutletMenuItemResponse response = new CreateOutletMenuItemResponse(
                menuItem.getId(),
                menuItem.getBusinessCode(),
                menuItem.getItemName(),
                menuItem.getItemPrice(),
                menuItem.getRestaurantChainId(),
                menuItem.getStatus(),
                menuItem.getVersion(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt(),
                menuItem.getOutletId(),
                menuItem.getMenuCategoryId());

        URI location = URI.create("/api/v1/restaurant-chains/" + restaurantChainId
                + "/outlets/" + outletId + "/menu-items/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/menu-items/{menuItemId}")
    public ResponseEntity<CreateSharedMenuItemResponse> updateMenuItem(
            @PathVariable Long restaurantChainId,
            @PathVariable Long menuItemId,
            @Valid @RequestBody MenuItemUpdateRequest request) {

        MenuItem menuItem = service.updateMenuItem(menuItemId, restaurantChainId, request);
        CreateSharedMenuItemResponse response = new CreateSharedMenuItemResponse(
                menuItem.getId(),
                menuItem.getBusinessCode(),
                menuItem.getItemName(),
                menuItem.getItemPrice(),
                menuItem.getRestaurantChainId(),
                menuItem.getStatus(),
                menuItem.getVersion(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt(),
                menuItem.getOutletId(),
                menuItem.getMenuGroupId(),
                menuItem.getMenuCategoryId());

        return ResponseEntity.ok(response);
    }
}
