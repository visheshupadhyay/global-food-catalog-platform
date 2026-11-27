package com.vishesh.foodcatalog.catalog.repository;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One persisted seasonal date range for a base scope or an outlet override. */
@Entity
@Table(name = "menu_item_seasonal_availability")
public class MenuItemSeasonalAvailabilityEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "menu_item_id", nullable = false)
	private Long menuItemId;
	@Column(name = "outlet_id")
	private Long outletId;
	@Column(name = "start_date")
	private LocalDate startDate;
	@Column(name = "end_date")
	private LocalDate endDate;
	@Column(name = "season_version", nullable = false)
	private Long seasonVersion;

	protected MenuItemSeasonalAvailabilityEntity() {
	}

	public MenuItemSeasonalAvailabilityEntity(Long menuItemId, Long outletId, LocalDate startDate, LocalDate endDate,
			Long seasonVersion) {
		this.menuItemId = menuItemId;
		this.outletId = outletId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.seasonVersion = seasonVersion;
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

	public Long getSeasonVersion() {
		return seasonVersion;
	}
}
