package com.vishesh.foodcatalog.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.RestaurantChainService;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;

import jakarta.validation.Valid;

/** Receives HTTP requests about restaurant chains and calls the service layer. */
@RequestMapping("/api/v1/restaurant-chains")
@RestController
public class RestaurantChainController {
    private final RestaurantChainService service;

    public RestaurantChainController(RestaurantChainService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RestaurantChainCreatedResponse> createRestaurantChain(
            @Valid @RequestBody CreateRestaurantChainRequest request) {

        // The controller is the HTTP front door; business rules stay in the service.
        RestaurantChain restaurantChain = service.createChain(request.getBusinessCode(), request.getDisplayName());
        RestaurantChainCreatedResponse response = new RestaurantChainCreatedResponse(restaurantChain.getId(),
                restaurantChain.getStatus());

        // Tell the caller where the new chain can be found later.
        URI location = URI.create("/api/v1/restaurant-chains/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantChainResponse> getRestaurantChainById(@PathVariable Long id) {

        // The controller is the HTTP front door; business rules stay in the service.
        RestaurantChain restaurantChain = service.findChainById(id);
        RestaurantChainResponse response = new RestaurantChainResponse(restaurantChain.getId(),
                restaurantChain.getBusinessCode(),
                restaurantChain.getDisplayName(),
                restaurantChain.getStatus(),
                restaurantChain.getCreatedAt(),
                restaurantChain.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-business-code/{businessCode}")
    public ResponseEntity<RestaurantChainResponse> getRestaurantChainByBusinessCode(
            @PathVariable String businessCode) {

        // The controller is the HTTP front door; business rules stay in the service.
        RestaurantChain restaurantChain = service.findChainByBusinessCode(businessCode);
        RestaurantChainResponse response = new RestaurantChainResponse(restaurantChain.getId(),
                restaurantChain.getBusinessCode(),
                restaurantChain.getDisplayName(),
                restaurantChain.getStatus(),
                restaurantChain.getCreatedAt(),
                restaurantChain.getUpdatedAt());

        return ResponseEntity.ok(response);
    }
}
