package com.vishesh.foodcatalog.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.RestaurantChainService;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/restaurant-chains")
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
}
