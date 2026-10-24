package com.vishesh.foodcatalog.catalog.effective.persistence.projection;

import java.math.BigDecimal;

public interface OutletLocalMenuItemProjection {
    Long getMenuItemId();

    Long getOutletId();

    String getItemName();

    BigDecimal getPrice();

    Boolean getAvailable();

    Boolean getVisible();

    String getStatus();
}
