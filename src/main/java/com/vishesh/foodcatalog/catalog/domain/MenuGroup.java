package com.vishesh.foodcatalog.catalog.domain;

import java.time.Instant;

/**
 * A shared menu category owned by a restaurant chain.
 * Examples are Burgers and Drinks, which outlets may inherit.
 */
public class MenuGroup {
    private String displayName;
    private ChainStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long restaurantChainId;

    public MenuGroup(Long restaurantChainId, String displayName) {
        // A new menu group starts as DRAFT until someone activates it.
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

        displayName = displayName.trim();
        this.displayName = displayName;
        this.restaurantChainId = restaurantChainId;
        this.status = ChainStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void restoreFromArchive() {
        // Restoration is an explicit action, separate from normal forward movement.
        if (this.status != ChainStatus.ARCHIVED) {
            throw new IllegalStateException("Only an archived menu group can be restored");
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

    public String getDisplayName() {
        return displayName;
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

    public Long getRestaurantChainId() {
        return restaurantChainId;
    }

}
