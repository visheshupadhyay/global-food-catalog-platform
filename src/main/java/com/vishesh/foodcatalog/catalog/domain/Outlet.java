package com.vishesh.foodcatalog.catalog.domain;

import java.time.Instant;
import java.util.Currency;

/**
 * One physical restaurant location.
 * An outlet may inherit a chain menu but can also have local prices,
 * availability, and outlet-only items.
 */
public class Outlet {
    private Long restaurantChainId;
    private String displayName;
    private String outletCode;
    private Currency currency;
    private ChainStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Outlet(Long restaurantChainId,
            String displayName,
            String outletCode,
            Currency currency) {

        // A new outlet starts as DRAFT until it is ready for customers.

        if (restaurantChainId == null) {
            throw new IllegalArgumentException("Restaurant chain ID cannot be null");
        }

        if (restaurantChainId <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID cannot be less than or equal to 0");
        }

        if (displayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        if (outletCode == null) {
            throw new IllegalArgumentException("Outlet Code cannot be null");
        }

        if (outletCode.isBlank()) {
            throw new IllegalArgumentException("Outlet Code cannot be blank");
        }

        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }

        displayName = displayName.trim();
        outletCode = outletCode.trim();
        this.currency = currency;
        this.outletCode = outletCode;
        this.displayName = displayName;
        this.restaurantChainId = restaurantChainId;
        this.status = ChainStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void restoreFromArchive() {
        // Reopening an outlet is deliberate, so it is not a normal forward transition.
        if (this.status != ChainStatus.ARCHIVED) {
            throw new IllegalStateException("Only an archived outlet can be restored");
        }
        applyStatus(ChainStatus.ACTIVE);
    }

    private void applyStatus(ChainStatus status) {
        this.updatedAt = Instant.now();
        this.status = status;
    }

    public void changeStatusTo(ChainStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (status == this.status) {
            return;
        }

        boolean validForwardTransition = (this.status == ChainStatus.DRAFT && status == ChainStatus.ACTIVE)
                || (this.status == ChainStatus.ACTIVE && status == ChainStatus.ARCHIVED);
        if (!validForwardTransition) {
            throw new IllegalStateException(
                    "Invalid status transition from " + this.status + " to " + status);
        }

        applyStatus(status);
    }

    public Long getRestaurantChainId() {
        return restaurantChainId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOutletCode() {
        return outletCode;
    }

    public Currency getCurrency() {
        return currency;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
