package com.vishesh.foodcatalog.catalog.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.CreateOutletCommand;
import com.vishesh.foodcatalog.catalog.application.OutletService;
import com.vishesh.foodcatalog.catalog.domain.Outlet;

import jakarta.validation.Valid;

/** HTTP entry point for creating draft outlets and activating prepared outlets. */
@RestController
public class OutletController {
    private final OutletService service;

    public OutletController(OutletService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/restaurant-chains/{restaurantChainId}/outlets")
    public ResponseEntity<OutletResponse> createDraftOutlet(
            @PathVariable Long restaurantChainId,
            @Valid @RequestBody CreateOutletRequest request) {

        Outlet outlet = service.createDraftOutlet(restaurantChainId,
                new CreateOutletCommand(
                        request.getDisplayName(),
                        request.getOutletCode(),
                        request.getCurrencyCode(),
                        request.getTimezone(),
                        request.getMenuGroupId()));

        OutletResponse response = new OutletResponse(
                outlet.getId(),
                outlet.getRestaurantChainId(),
                outlet.getDisplayName(),
                outlet.getOutletCode(),
                outlet.getCurrency().getCurrencyCode(),
                outlet.getTimezone(),
                outlet.getStatus(),
                outlet.getVersion());

        URI location = URI.create("/api/v1/restaurant-chains/" + restaurantChainId
                + "/outlets/" + outlet.getId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Explicitly publishes a prepared outlet to customers. The service owns
     * the transaction which makes the status change and initialization event
     * durable together.
     */
    @PostMapping("/api/v1/outlets/{outletId}/activation")
    public OutletActivationResponse activateOutlet(
            @PathVariable Long outletId,
            @Valid @RequestBody ActivateOutletRequest request) {

        Outlet outlet = service.activateOutlet(outletId, request.expectedVersion());
        return new OutletActivationResponse(outlet.getId(), outlet.getStatus(), outlet.getVersion());
    }
}
