package com.vishesh.foodcatalog.catalog.reconciliation;

public enum ReconciliationResultType {
    MATCH,
    MISSING_SNAPSHOT,
    STALE_SNAPSHOT,
    FIELD_MISMATCH
}
