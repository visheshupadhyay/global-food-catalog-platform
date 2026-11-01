package com.vishesh.foodcatalog.catalog.reconciliation;

import java.util.Optional;

public interface ReconciliationStateReader {
    Optional<ReconciliationStates> find(Long menuItemId, Long outletId);
}
