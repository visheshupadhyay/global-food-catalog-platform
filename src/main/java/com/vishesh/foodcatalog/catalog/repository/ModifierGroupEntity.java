package com.vishesh.foodcatalog.catalog.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA row for one ordered modifier group owned by a menu item. */
@Entity
@Table(name = "modifier_group")
public class ModifierGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_menu_item_id", nullable = false)
    private Long ownerMenuItemId;
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;
    @Column(name = "minimum_selections", nullable = false)
    private int minimumSelections;
    @Column(name = "maximum_selections", nullable = false)
    private int maximumSelections;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected ModifierGroupEntity() {
    }

    public ModifierGroupEntity(Long ownerMenuItemId, String groupName,
            int minimumSelections, int maximumSelections, int displayOrder) {
        this.ownerMenuItemId = ownerMenuItemId;
        this.groupName = groupName;
        this.minimumSelections = minimumSelections;
        this.maximumSelections = maximumSelections;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerMenuItemId() {
        return ownerMenuItemId;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getMinimumSelections() {
        return minimumSelections;
    }

    public int getMaximumSelections() {
        return maximumSelections;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void update(String groupName, int minimumSelections, int maximumSelections, int displayOrder) {
        this.groupName = groupName;
        this.minimumSelections = minimumSelections;
        this.maximumSelections = maximumSelections;
        this.displayOrder = displayOrder;
    }
}
