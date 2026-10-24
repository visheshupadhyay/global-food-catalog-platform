package com.vishesh.foodcatalog.catalog.effective.persistence.projection;

import java.math.BigDecimal;

public interface OutletOverrideProjection {
    Long getMenuItemId();

    BigDecimal getPriceOverride();

    Boolean getAvailableOverride();

    Boolean getVisibleOverride();
}
