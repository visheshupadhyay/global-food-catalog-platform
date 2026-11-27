package com.vishesh.foodcatalog.catalog.domain;

/** A child product or nested bundle included in a parent bundle. */
public class BundleChild {

	private final Long id;
	private final Long bundleMenuItemId;
	private final Long childMenuItemId;
	private final int quantity;
	private final int displayOrder;

	/** Creates a relationship that PostgreSQL has not stored yet. */
	public BundleChild(Long bundleMenuItemId, Long childMenuItemId, int quantity, int displayOrder) {
		this(null, bundleMenuItemId, childMenuItemId, quantity, displayOrder);
	}

	/** Recreates a relationship that already has a database identity. */
	public BundleChild(Long id, Long bundleMenuItemId, Long childMenuItemId, int quantity, int displayOrder) {
		if (id != null && id <= 0) {
			throw new IllegalArgumentException("Bundle child ID must be greater than zero");
		}
		if (bundleMenuItemId == null || bundleMenuItemId <= 0) {
			throw new IllegalArgumentException("Bundle menu item ID must be greater than zero");
		}
		if (childMenuItemId == null || childMenuItemId <= 0) {
			throw new IllegalArgumentException("Child menu item ID must be greater than zero");
		}
		if (bundleMenuItemId.equals(childMenuItemId)) {
			throw new IllegalArgumentException("A bundle cannot contain itself directly");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Bundle child quantity must be greater than zero");
		}
		if (displayOrder < 0) {
			throw new IllegalArgumentException("Bundle child display order cannot be negative");
		}
		this.id = id;
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
