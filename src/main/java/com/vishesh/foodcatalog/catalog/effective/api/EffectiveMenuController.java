package com.vishesh.foodcatalog.catalog.effective.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vishesh.foodcatalog.catalog.api.EffectiveMenuItemResponse;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuService;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;

@RestController
@RequestMapping("/api/v1/outlets")
/** Exposes the menu that a customer sees at one outlet. */
public class EffectiveMenuController {
    private final EffectiveMenuService service;

    public EffectiveMenuController(EffectiveMenuService service) {
        this.service = service;
    }

    @GetMapping("/{outletId}/menu")
    public ResponseEntity<List<EffectiveMenuItemResponse>> getMenu( @PathVariable("outletId")Long outletId) {
        // The service performs validation, filtering, and inheritance resolution.
        List<EffectiveMenuItem> effectiveMenuItems = service.getEffectiveMenu(outletId);
        List<EffectiveMenuItemResponse> effectiveMenuItemResponses = new ArrayList<>();
        for (EffectiveMenuItem item: effectiveMenuItems) {
            effectiveMenuItemResponses.add(new EffectiveMenuItemResponse(
                item.getMenuItemId(), 
                item.getOutletId(), 
                item.getItemName(), 
                item.getPrice(), 
                item.isAvailable(),
                item.isVisible()));
        }
        return ResponseEntity.ok(effectiveMenuItemResponses);
    }
}
