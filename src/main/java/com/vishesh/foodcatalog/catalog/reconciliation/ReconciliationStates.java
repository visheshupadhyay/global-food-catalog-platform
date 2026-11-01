package com.vishesh.foodcatalog.catalog.reconciliation;

public record ReconciliationStates(CatalogMenuItemState catalogState,
        EffectiveMenuSnapshotState snapshotState) {
}
