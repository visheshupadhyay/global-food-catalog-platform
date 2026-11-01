package com.vishesh.foodcatalog.catalog.reconciliation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reconciliation/menu-items")
/** Exposes a diagnostic endpoint for checking one item at one outlet. */
public class ReconciliationEndpoint {
    private final ReconciliationStateReader stateReader;
    private final ReconciliationService reconciliationService;

    public ReconciliationEndpoint(ReconciliationStateReader stateReader,
            ReconciliationService reconciliationService) {
        this.stateReader = stateReader;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/{menuItemId}/outlets/{outletId}")
    public ResponseEntity<ReconciliationResult> compare(@PathVariable Long menuItemId,
            @PathVariable Long outletId) {
        if (menuItemId == null || menuItemId <= 0 || outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Menu item and outlet IDs must be positive");
        }
        ReconciliationStates states = stateReader.find(menuItemId, outletId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliation state not found"));
        return ResponseEntity.ok(reconciliationService.compare(states.catalogState(), states.snapshotState()));
    }
}
