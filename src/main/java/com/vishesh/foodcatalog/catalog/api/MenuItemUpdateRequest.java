package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;

public class MenuItemUpdateRequest {
    private Long expectedVersion;
    private BigDecimal price;
    private Boolean availability;
    private Boolean visibility;

    public MenuItemUpdateRequest(Long expectedVersion,
            BigDecimal price,
            Boolean availability,
            Boolean visibility) {

        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version must be zero or greater");
        }

        if (price == null
                && availability == null
                && visibility == null) {

            throw new IllegalArgumentException("At least one menu item field must be supplied");
        }

        if (price != null && price.signum() < 0) {

            throw new IllegalArgumentException("Price must be zero or greater");
        }

        this.expectedVersion = expectedVersion;
        this.price = price;
        this.availability = availability;
        this.visibility = visibility;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public Boolean getVisibility() {
        return visibility;
    }

}
