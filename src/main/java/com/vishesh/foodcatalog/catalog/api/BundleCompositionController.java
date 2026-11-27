package com.vishesh.foodcatalog.catalog.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.application.BundleCompositionResult;
import com.vishesh.foodcatalog.catalog.application.BundleCompositionService;
import com.vishesh.foodcatalog.catalog.domain.BundleChild;

import jakarta.validation.Valid;

/**
 * HTTP entry point for replacing a bundle's complete direct-child list.
 * Validation of ownership, cycles, persistence, and outbox delivery belongs to
 * the application service, not this HTTP adapter.
 */
@RestController
@RequestMapping("/api/v1/restaurant-chains/{restaurantChainId}/menu-items/{bundleMenuItemId}/bundle-children")
public class BundleCompositionController {
	private final BundleCompositionService service;

	public BundleCompositionController(BundleCompositionService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<BundleCompositionResponse> getChildren(@PathVariable Long restaurantChainId,
			@PathVariable Long bundleMenuItemId) {

		return ResponseEntity.ok(toResponse(service.getComposition(bundleMenuItemId, restaurantChainId)));
	}

	@PutMapping
	public ResponseEntity<BundleCompositionResponse> replaceChildren(@PathVariable Long restaurantChainId,
			@PathVariable Long bundleMenuItemId, @Valid @RequestBody ReplaceBundleChildrenRequest request) {

		BundleCompositionResult result = service.replaceChildrenWithResult(bundleMenuItemId, restaurantChainId,
				request.expectedVersion(), request.children().stream().map(child -> new BundleChild(bundleMenuItemId,
						child.childMenuItemId(), child.quantity(), child.displayOrder())).toList());

		return ResponseEntity.ok(toResponse(result));
	}

	private BundleCompositionResponse toResponse(BundleCompositionResult result) {
		return new BundleCompositionResponse(result.bundleMenuItemId(), result.bundleVersion(),
				result.children().stream().map(child -> new BundleChildResponse(child.getChildMenuItemId(),
						child.getQuantity(), child.getDisplayOrder())).toList());
	}
}
