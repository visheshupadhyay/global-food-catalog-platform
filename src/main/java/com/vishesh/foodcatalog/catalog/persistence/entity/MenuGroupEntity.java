package com.vishesh.foodcatalog.catalog.persistence.entity;

import java.time.Instant;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** The database-shaped copy of a menu group; Hibernate tracks its version. */
@Table(name = "menu_group")
@Entity
public class MenuGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_chain_id", nullable = false)
    private Long restaurantChainId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChainStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "normalized_display_name", insertable = false, updatable = false)
    private String normalizedDisplayName;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected MenuGroupEntity() {

    }

    public MenuGroupEntity(Long restaurantChainId, String displayName) {
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

        this.restaurantChainId = restaurantChainId;
        this.displayName = displayName;
        this.status = ChainStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.version = 0L;
    }

    public MenuGroupEntity(Long restaurantChainId,
            String displayName,
            ChainStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version,
            Long id) {

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

        this.id = id;
        this.restaurantChainId = restaurantChainId;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public void rename(String newDisplayName) {
        if (newDisplayName == null) {
            throw new IllegalArgumentException("Display name cannot be null");
        }

        String trimmedName = newDisplayName.trim();

        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be blank");
        }

        this.displayName = trimmedName;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantChainId() {
        return restaurantChainId;
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

    public String getNormalizedDisplayName() {
        return normalizedDisplayName;
    }

    public Long getVersion() {
        return version;
    }
}
