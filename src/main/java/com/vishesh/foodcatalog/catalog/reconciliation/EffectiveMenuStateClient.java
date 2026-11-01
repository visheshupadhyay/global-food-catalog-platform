package com.vishesh.foodcatalog.catalog.reconciliation;

import java.util.Optional;

public interface EffectiveMenuStateClient {
    Optional<EffectiveMenuSnapshotState> findSnapshotState(Long menuItemId,Long outletId);
}
