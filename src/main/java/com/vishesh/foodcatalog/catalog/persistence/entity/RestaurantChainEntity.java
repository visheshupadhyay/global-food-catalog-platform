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

/**
 * The database-shaped version of one restaurant chain.
 * One instance represents one row in the restaurant_chain table.
 */
@Entity
@Table(name = "restaurant_chain")
public class RestaurantChainEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "business_code", nullable = false)
    private String businessCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChainStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RestaurantChainEntity() {
        // JPA needs an empty constructor when it reads a row from PostgreSQL.
    }

    public RestaurantChainEntity(Long id,
            String businessCode,
            String displayName,
            ChainStatus status,
            Instant createdAt,
            Instant updatedAt) {

        if (businessCode == null || displayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Id must be positive when provided");
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

    public RestaurantChainEntity(String businessCode, String displayName) {
        // The database creates the ID when this new entity is saved.
        if (displayName == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }

        if (businessCode == null) {
            throw new IllegalArgumentException("Business Code cannot be null");
        }

        if (businessCode.isBlank()) {
            throw new IllegalArgumentException("Business Code cannot be blank");
        }
        businessCode = businessCode.trim();
        displayName = displayName.trim();
        this.businessCode = businessCode;
        this.displayName = displayName;
        this.status = ChainStatus.DRAFT;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
