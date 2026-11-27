package com.vishesh.foodcatalog.catalog.event.domain;

import java.util.List;

import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

/**
 * Immutable customer-facing summary of one product or nested bundle inside a
 * bundle event.
 */
public record EventBundleChild(Long menuItemId, String name, MenuItemKind kind, int quantity, int displayOrder,
		MenuItemStatus status, boolean visibility, boolean availability, List<EventBundleChild> children) {

	public EventBundleChild {
		if (menuItemId == null || menuItemId <= 0) {
			throw new IllegalArgumentException("Bundle child menu item ID must be positive");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Bundle child name cannot be blank");
		}
		if (kind == null) {
			throw new IllegalArgumentException("Bundle child kind is required");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Bundle child quantity must be positive");
		}
		if (displayOrder < 0) {
			throw new IllegalArgumentException("Bundle child display order cannot be negative");
		}
		if (status == null) {
			throw new IllegalArgumentException("Bundle child status is required");
		}
		if (children == null) {
			throw new IllegalArgumentException("Bundle child list is required");
		}
		if (children.stream().anyMatch(child -> child == null)) {
			throw new IllegalArgumentException("Bundle child list cannot contain null values");
		}

		name = name.trim();
		children = List.copyOf(children);
	}
}
