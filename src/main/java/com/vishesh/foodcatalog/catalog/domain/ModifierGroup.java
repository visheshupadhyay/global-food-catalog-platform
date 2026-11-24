package com.vishesh.foodcatalog.catalog.domain;

/** A set of choices, such as a Burger's "Choose extras" group. */
public class ModifierGroup {

    private final Long id;
    private final Long ownerMenuItemId;
    private final String groupName;
    private final int minimumSelections;
    private final int maximumSelections;
    private final int displayOrder;

    /** Creates a group that PostgreSQL has not stored yet. */
    public ModifierGroup(Long ownerMenuItemId, String groupName,
            int minimumSelections, int maximumSelections, int displayOrder) {
        this(null, ownerMenuItemId, groupName, minimumSelections, maximumSelections, displayOrder);
    }

    /** Recreates a group that already has a database identity. */
    public ModifierGroup(Long id, Long ownerMenuItemId, String groupName,
            int minimumSelections, int maximumSelections, int displayOrder) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Modifier group ID must be greater than zero");
        }
        if (ownerMenuItemId == null || ownerMenuItemId <= 0) {
            throw new IllegalArgumentException("Modifier group owner ID must be greater than zero");
        }
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("Modifier group name cannot be blank");
        }
        if (minimumSelections < 0) {
            throw new IllegalArgumentException("Minimum selections cannot be negative");
        }
        if (maximumSelections < minimumSelections) {
            throw new IllegalArgumentException("Maximum selections cannot be less than minimum selections");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Modifier group display order cannot be negative");
        }
        this.id = id;
        this.ownerMenuItemId = ownerMenuItemId;
        this.groupName = groupName.trim();
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
}
