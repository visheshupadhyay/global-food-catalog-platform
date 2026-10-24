package com.vishesh.foodcatalog.catalog.effective.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vishesh.foodcatalog.catalog.application.OutletNotFoundException;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;

@Service
/** Coordinates the customer read: load source data, apply outlet rules, and return the menu. */
public class EffectiveMenuService {
    private final EffectiveMenuDataProvider effectiveMenuDataProvider;
    private final EffectiveMenuAssembler effectiveMenuAssembler;

    public EffectiveMenuService(EffectiveMenuDataProvider effectiveMenuDataProvider,
            EffectiveMenuAssembler effectiveMenuAssembler) {

        this.effectiveMenuDataProvider = effectiveMenuDataProvider;
        this.effectiveMenuAssembler = effectiveMenuAssembler;
    }

    public List<EffectiveMenuItem> getEffectiveMenu(Long outletId) {
        // Invalid IDs stop before any database query is made.
        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }
        Optional<EffectiveMenuSourceData> sourceDataOptional = effectiveMenuDataProvider.findByOutletId(outletId);
        if (sourceDataOptional.isEmpty()) {
            throw new OutletNotFoundException("Outlet not found");
        }

        EffectiveMenuSourceData sourceData = sourceDataOptional.get();
        // Draft and archived outlets are not customer-facing outlets.
        if (sourceData.outletStatus() != ChainStatus.ACTIVE) {
            throw new OutletNotFoundException("Outlet not found");
        }

        return effectiveMenuAssembler.assemble(outletId,
                sourceData.baseItems(),
                sourceData.overrides(),
                sourceData.localItems());

    }
}
