package com.vishesh.foodcatalog.catalog.domain;

import java.time.LocalDate;

/**
 * Optional date-range availability rule for a menu item. A null outlet ID is
 * the base rule; a non-null outlet ID is that outlet's override. The end date
 * is exclusive, so 2026-10-15 to 2026-11-05 ends at the start of 5 November.
 */
public final class MenuItemSeasonalAvailability {
	private final Long menuItemId;
	private final Long outletId;
	private final LocalDate startDate;
	private final LocalDate endDate;
	private final Long version;

	public MenuItemSeasonalAvailability(Long menuItemId, Long outletId, LocalDate startDate, LocalDate endDate,
			Long version) {
		if (menuItemId == null || menuItemId <= 0) {
			throw new IllegalArgumentException("Menu item ID must be positive");
		}
		if (outletId != null && outletId <= 0) {
			throw new IllegalArgumentException("Outlet ID must be positive when supplied");
		}
		if ((startDate == null) != (endDate == null)) {
			throw new IllegalArgumentException("Season start and end dates must be supplied together");
		}
		if (startDate != null && !startDate.isBefore(endDate)) {
			throw new IllegalArgumentException("Season end date must be after start date");
		}
		if (version == null || version < 0) {
			throw new IllegalArgumentException("Season version must be non-negative");
		}
		this.menuItemId = menuItemId;
		this.outletId = outletId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.version = version;
	}

	public Long getMenuItemId() {
		return menuItemId;
	}

	public Long getOutletId() {
		return outletId;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public Long getVersion() {
		return version;
	}

	public boolean isRestricted() {
		return startDate != null;
	}
}
