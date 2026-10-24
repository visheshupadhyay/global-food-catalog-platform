package com.vishesh.foodcatalog.catalog.effective.persistence.projection;

import java.math.BigDecimal;

public interface BaseMenuItemProjection {
    Long getMenuItemId();

    String getItemName();

    BigDecimal getPrice();

    Boolean getAvailable();

    Boolean getVisible();

    String getStatus();
}
