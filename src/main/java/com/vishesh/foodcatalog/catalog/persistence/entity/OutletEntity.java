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

/** The database-shaped view of one physical outlet. */
@Entity
@Table(name = "outlet")
public class OutletEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "outlet_code", nullable = false, length = 100)
    private String outletCode;

    @Column(name = "restaurant_chain_id", nullable = false)
    private Long restaurantChainId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChainStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutletEntity() {
        // JPA uses this constructor while rebuilding an outlet from a database row.
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOutletCode() {
        return outletCode;
    }

    public Long getRestaurantChainId() {
        return restaurantChainId;
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
