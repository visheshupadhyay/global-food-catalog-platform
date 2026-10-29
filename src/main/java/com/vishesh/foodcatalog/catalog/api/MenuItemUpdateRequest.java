package com.vishesh.foodcatalog.catalog.api;

import java.math.BigDecimal;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Describes one safe change an administrator wants to make to a menu item. */
public class MenuItemUpdateRequest {
    private Long expectedVersion;
    private BigDecimal price;
    private Boolean availability;
    private Boolean visibility;
    private MenuItemStatus status;

    public MenuItemUpdateRequest(Long expectedVersion,
            BigDecimal price,
            Boolean availability,
            Boolean visibility) {
        this(expectedVersion, price, availability, visibility, null);
    }

    @JsonCreator
    public MenuItemUpdateRequest(
            @JsonProperty("expectedVersion") Long expectedVersion,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("availability") Boolean availability,
            @JsonProperty("visibility") Boolean visibility,
            @JsonProperty("status") MenuItemStatus status) {

        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version must be zero or greater");
        }

        if (price == null
                && availability == null
                && visibility == null
                && status == null) {

            throw new IllegalArgumentException("At least one menu item field must be supplied");
        }

        if (price != null && price.signum() < 0) {

            throw new IllegalArgumentException("Price must be zero or greater");
        }

        this.expectedVersion = expectedVersion;
        this.price = price;
        this.availability = availability;
        this.visibility = visibility;
        this.status = status;
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

    public MenuItemStatus getStatus() {
        return status;
    }

}
