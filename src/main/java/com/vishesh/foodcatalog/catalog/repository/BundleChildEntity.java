package com.vishesh.foodcatalog.catalog.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA row for one parent-bundle to child-menu-item relationship. */
@Entity
@Table(name = "bundle_child")
public class BundleChildEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bundle_menu_item_id", nullable = false)
    private Long bundleMenuItemId;
    @Column(name = "child_menu_item_id", nullable = false)
    private Long childMenuItemId;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected BundleChildEntity() {
    }

    public BundleChildEntity(Long bundleMenuItemId, Long childMenuItemId, int quantity, int displayOrder) {
        this.bundleMenuItemId = bundleMenuItemId;
        this.childMenuItemId = childMenuItemId;
        this.quantity = quantity;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getBundleMenuItemId() {
        return bundleMenuItemId;
    }

    public Long getChildMenuItemId() {
        return childMenuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
