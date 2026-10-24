package com.vishesh.foodcatalog.catalog.effective.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuDataProvider;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.BaseMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletLocalMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletOverrideProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletStatusProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.repository.EffectiveMenuJpaReadRepository;

@Component
/** Reads only the small set of rows needed to calculate one outlet's menu. */
public class PostgresEffectiveMenuDataProvider implements EffectiveMenuDataProvider {
    private final EffectiveMenuJpaReadRepository effectiveMenuJpaReadRepository;

    public PostgresEffectiveMenuDataProvider(EffectiveMenuJpaReadRepository effectiveMenuJpaReadRepository) {
        this.effectiveMenuJpaReadRepository = effectiveMenuJpaReadRepository;
    }

    @Override
    public Optional<EffectiveMenuSourceData> findByOutletId(Long outletId) {
        if (outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Outlet not found");
        }

        Optional<OutletStatusProjection> localMenuItemProjections = effectiveMenuJpaReadRepository
                .findOutletStatusByOutletId(outletId);

        if (localMenuItemProjections.isEmpty()) {
            // No outlet row means the outlet does not exist.
            return Optional.empty();
        }

        List<BaseMenuItemProjection> baseMenuProjection = effectiveMenuJpaReadRepository
                .findBaseItemsByOutletId(outletId);

        List<OutletOverrideProjection> localOverridesProjection = effectiveMenuJpaReadRepository
                .findOverridesByOutletId(outletId);

        List<OutletLocalMenuItemProjection> localItemsProjection = effectiveMenuJpaReadRepository
                .findLocalItemsByOutletId(outletId);

        List<OutletLocalMenuItemSnapshot> localItems = new ArrayList<>();
        for (OutletLocalMenuItemProjection projection : localItemsProjection) {
            localItems.add(new OutletLocalMenuItemSnapshot(projection.getMenuItemId(),
                    projection.getOutletId(),
                    projection.getItemName(),
                    projection.getPrice(),
                    projection.getAvailable(),
                    projection.getVisible(),
                    MenuItemStatus.valueOf(projection.getStatus())));
        }

        List<BaseMenuItemSnapshot> baseItems = new ArrayList<>();
        for (BaseMenuItemProjection projection : baseMenuProjection) {
            baseItems.add(new BaseMenuItemSnapshot(
                    projection.getMenuItemId(),
                    projection.getItemName(),
                    projection.getPrice(),
                    projection.getAvailable(),
                    projection.getVisible(),
                    MenuItemStatus.valueOf(projection.getStatus())));
        }

        List<OutletOverrideSnapshot> overrideItems = new ArrayList<>();
        for (OutletOverrideProjection projection : localOverridesProjection) {
            overrideItems.add(new OutletOverrideSnapshot(
                    projection.getMenuItemId(),
                    outletId,
                    projection.getPriceOverride(),
                    projection.getAvailableOverride(),
                    projection.getVisibleOverride()));
        }
        return Optional.of(new EffectiveMenuSourceData(
                outletId,
                ChainStatus.valueOf(localMenuItemProjections.get().getOutletStatus()),
                baseItems,
                overrideItems,
                localItems));

    }
}
