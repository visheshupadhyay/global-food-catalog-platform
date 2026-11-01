package com.vishesh.foodcatalog.catalog.reconciliation;

public class ReconciliationResult {
    private final Long menuItemId;
    private final Long outletId;
    private final ReconciliationResultType resultType;
    private final Long catalogVersion;
    private final Long effectiveVersion;
    private final String message;
    public ReconciliationResult(Long menuItemId,
        Long outletId,
            ReconciliationResultType resultType,
        Long catalogVersion,
        Long effectiveVersion,
            String message) {
        this.menuItemId = menuItemId;
        this.outletId = outletId;
        this.resultType = resultType;
        this.catalogVersion = catalogVersion;
        this.effectiveVersion = effectiveVersion;
        this.message = message;
    }
    public Long getMenuItemId() {
        return menuItemId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public ReconciliationResultType getResultType() {
        return resultType;
    }

    public Long getCatalogVersion() {
        return catalogVersion;
    }

    public Long getEffectiveVersion() {
        return effectiveVersion;
    }

    public String getMessage() {
        return message;
    }

}
