package com.vishesh.foodcatalog.catalog.reconciliation;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

@Component
public class CatalogReconciliationStateReader implements ReconciliationStateReader {
    private final MenuItemRepository repository;
    private final EffectiveMenuStateClient client;

    public CatalogReconciliationStateReader(MenuItemRepository repository, EffectiveMenuStateClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Override
    public Optional<ReconciliationStates> find(Long menuItemId, Long outletId) {
        if (menuItemId == null || menuItemId <= 0 || outletId == null || outletId <= 0) {
            throw new IllegalArgumentException("Menu item and outlet IDs must be positive");
        }
        Optional<MenuItem> menuItemResult = repository.findById(menuItemId);
        if (menuItemResult.isEmpty()) {
            return Optional.empty();
        }

        MenuItem menuItem = menuItemResult.get();
        // A local item belongs to exactly one outlet. A shared item may be used by many outlets.
        if (menuItem.getOutletId() != null && !outletId.equals(menuItem.getOutletId())) {
            return Optional.empty();
        }
        CatalogMenuItemState catalogMenuItemState = new CatalogMenuItemState(menuItemId,
                outletId,
                menuItem.getItemPrice(),
                menuItem.isAvailability(),
                menuItem.isVisibility(),
                menuItem.getVersion());

        // A missing snapshot is meaningful to reconciliation, so keep the catalog state and
        // represent the snapshot as null instead of dropping the whole comparison.
        EffectiveMenuSnapshotState effectiveMenuSnapshotState = client
                .findSnapshotState(menuItemId, outletId)
                .orElse(null);

        return Optional.of(new ReconciliationStates(catalogMenuItemState, effectiveMenuSnapshotState));
    }

}
