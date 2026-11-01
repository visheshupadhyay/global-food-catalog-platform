package com.vishesh.foodcatalog.catalog.reconciliation;

import java.math.BigDecimal;

public class EffectiveMenuSnapshotState {
    private final Long menuItemId;
    private final Long outletId;
    private BigDecimal price;
    private boolean availabilty;
    private boolean visibility;
    private Long version;

    public EffectiveMenuSnapshotState(Long menuItemId, Long outletId, BigDecimal price, boolean availabilty,
            boolean visibility, Long version) {
        this.menuItemId = menuItemId;
        this.outletId = outletId;
        this.price = price;
        this.availabilty = availabilty;
        this.visibility = visibility;
        this.version = version;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailabilty() {
        return availabilty;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public Long getVersion() {
        return version;
    }

}
