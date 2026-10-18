package com.vishesh.foodcatalog.catalog.domain;

import java.time.Instant;

/**
 * The business representation of one restaurant brand.
 * For example, Burger House can own outlets in Berlin and Munich.
 * This class protects the rules that must always be true for a chain.
 */
public class RestaurantChain {
    private String businessCode;
    private String displayName;
    private ChainStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long id;

    public RestaurantChain(String businessCode, String displayName, ChainStatus status, Instant createdAt,
            Instant updatedAt, Long id) {

        // This constructor rebuilds a chain that already came from the database.

        if (businessCode == null || displayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Id cannot be zero or null");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps cannot be null");
        }
        businessCode = businessCode.trim();
        displayName = displayName.trim();

        if (businessCode.isEmpty() || displayName.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        this.businessCode = businessCode;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.id = id;
    }

    public RestaurantChain(String businessCode, String displayName) {
        // This constructor creates a brand-new chain with fresh timestamps.
        if (businessCode == null || displayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        businessCode = businessCode.trim();
        displayName = displayName.trim();

        if (businessCode.isEmpty() || displayName.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        this.businessCode = businessCode;

        this.displayName = displayName;

        this.status = ChainStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeStatusTo(ChainStatus status) {
        // Normal lifecycle movement is DRAFT -> ACTIVE -> ARCHIVED.
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

    /**
     * Explicitly reactivates an archived chain after a deliberate restore decision.
     * Reopening is kept separate from the normal forward lifecycle transition.
     */
    public void restoreFromArchive() {
        // Restoring an archived chain is an intentional exception to forward movement.
        if (this.status != ChainStatus.ARCHIVED) {
            throw new IllegalStateException("Only an archived chain can be restored");
        }
        applyStatus(ChainStatus.ACTIVE);
    }

    private void applyStatus(ChainStatus status) {
        // Keep the last-change time current whenever the status changes.
        this.updatedAt = Instant.now();
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getId() {
        return id;
    }
}
