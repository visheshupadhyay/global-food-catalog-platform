package com.vishesh.foodcatalog.catalog.domain;

import java.time.Instant;

/**
 * A shared menu category owned by a restaurant chain.
 * Examples are Burgers and Drinks, which outlets may inherit.
 */
public class MenuGroup {
    private Long id;
    private String displayName;
    private ChainStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long restaurantChainId;
    private Long version;

    public MenuGroup(Long restaurantChainId,
            String displayName,
            ChainStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version,
            Long id) {

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

        displayName = displayName.trim();

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        if (id == null) {
            throw new IllegalArgumentException("Menu group ID cannot be null");
        }

        if (id <= 0) {
            throw new IllegalArgumentException("Menu group ID should be greater than zero");
        }

        if (status == null) {
            throw new IllegalArgumentException("Menu group status should be present");
        }

        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps should be present");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version must be zero or greater");
        }
        this.displayName = displayName;
        this.restaurantChainId = restaurantChainId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version= version;
        this.id = id;

    }

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

        displayName = displayName.trim();

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        this.displayName = displayName;
        this.restaurantChainId = restaurantChainId;
        this.status = ChainStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.version = 0L;
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

    public void changeDisplayName(String newDisplayName) {
        if (newDisplayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        newDisplayName = newDisplayName.trim();

        if (newDisplayName.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }
        this.updatedAt = Instant.now();
        this.displayName = newDisplayName;
    }

    public void incrementVersion() {
        if (this.version == null) {
            throw new IllegalStateException("Cannot increment a null version");
        }
        this.version++;
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

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }
}
