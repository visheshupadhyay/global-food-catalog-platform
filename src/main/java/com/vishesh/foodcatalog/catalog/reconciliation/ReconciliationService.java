package com.vishesh.foodcatalog.catalog.reconciliation;

import org.springframework.stereotype.Service;

@Service
/** Compares Catalog truth with an Effective Menu snapshot and explains any drift. */
public class ReconciliationService {
    public ReconciliationResult compare(CatalogMenuItemState catalogState, EffectiveMenuSnapshotState snapshotState) {
        if (catalogState == null) {
            throw new IllegalArgumentException("Catalog state cannot be null");
        }
        if (snapshotState == null) {
            return new ReconciliationResult(catalogState.getMenuItemId(), catalogState.getOutletId(),
                    ReconciliationResultType.MISSING_SNAPSHOT, catalogState.getVersion(), null,
                    "Effective menu snapshot is missing");
        }
        if (snapshotState.getVersion() < catalogState.getVersion()) {
            return new ReconciliationResult(catalogState.getMenuItemId(), catalogState.getOutletId(),
                    ReconciliationResultType.STALE_SNAPSHOT, catalogState.getVersion(), snapshotState.getVersion(),
                    "Effective menu snapshot is older than Catalog state");
        }

        if (!catalogState.getMenuItemId().equals(snapshotState.getMenuItemId())
                || !catalogState.getOutletId().equals(snapshotState.getOutletId())
                || catalogState.getPrice().compareTo(snapshotState.getPrice()) != 0
                || catalogState.isAvailabilty() != snapshotState.isAvailabilty()
                || catalogState.isVisibility() != snapshotState.isVisibility()) {
            return new ReconciliationResult(catalogState.getMenuItemId(), catalogState.getOutletId(),
                    ReconciliationResultType.FIELD_MISMATCH, catalogState.getVersion(), snapshotState.getVersion(),
                    "Catalog and effective menu fields do not match");
        }

        return new ReconciliationResult(catalogState.getMenuItemId(), catalogState.getOutletId(),
                ReconciliationResultType.MATCH, catalogState.getVersion(), snapshotState.getVersion(), "States match");
    }
}
