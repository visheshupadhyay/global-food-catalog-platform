package com.vishesh.foodcatalog.catalog.domain;

import java.time.Instant;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Currency;

/**
 * One physical restaurant location. An outlet may inherit a chain menu but can
 * also have local prices, availability, and outlet-only items.
 */
public class Outlet {
	private Long id;
	private Long restaurantChainId;
	private String displayName;
	private String outletCode;
	private Currency currency;
	private ChainStatus status;
	private Instant createdAt;
	private Instant updatedAt;
	private String timezone;
	private Long version;

	/**
	 * Backward-compatible constructor; existing outlets use UTC until configured
	 * otherwise.
	 */
	public Outlet(Long restaurantChainId, String displayName, String outletCode, Currency currency) {
		this(restaurantChainId, displayName, outletCode, currency, "UTC");
	}

	public Outlet(Long restaurantChainId, String displayName, String outletCode, Currency currency, String timezone) {

		// A new outlet starts as DRAFT until it is ready for customers.

		if (restaurantChainId == null) {
			throw new IllegalArgumentException("Restaurant chain ID cannot be null");
		}

		if (restaurantChainId <= 0) {
			throw new IllegalArgumentException("Restaurant chain ID cannot be less than or equal to 0");
		}

		if (displayName == null) {
			throw new IllegalArgumentException("Value cannot be null");
		}

		if (displayName.isBlank()) {
			throw new IllegalArgumentException("Value cannot be blank");
		}

		if (outletCode == null) {
			throw new IllegalArgumentException("Outlet Code cannot be null");
		}

		if (outletCode.isBlank()) {
			throw new IllegalArgumentException("Outlet Code cannot be blank");
		}

		if (currency == null) {
			throw new IllegalArgumentException("Currency cannot be null");
		}

		if (timezone == null || timezone.isBlank()) {
			throw new IllegalArgumentException("Timezone cannot be null or blank");
		}

		displayName = displayName.trim();
		outletCode = outletCode.trim();
		this.currency = currency;
		this.outletCode = outletCode;
		this.displayName = displayName;
		this.restaurantChainId = restaurantChainId;
		this.status = ChainStatus.DRAFT;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
		try {
			this.timezone = ZoneId.of(timezone.trim()).toString();
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("Timezone must be a valid IANA timezone", exception);
		}
		this.version = 0L;
	}

	/**
	 * Rebuilds an outlet that already exists in PostgreSQL. New outlets use the
	 * shorter constructors above; persistence uses this constructor instead.
	 */
	public Outlet(Long id, Long restaurantChainId, String displayName, String outletCode, Currency currency,
			ChainStatus status, Instant createdAt, Instant updatedAt, String timezone, Long version) {

		if (id == null || id <= 0) {
			throw new IllegalArgumentException("Outlet ID must be positive");
		}
		if (restaurantChainId == null || restaurantChainId <= 0) {
			throw new IllegalArgumentException("Restaurant chain ID must be positive");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("Display name cannot be blank");
		}
		if (outletCode == null || outletCode.isBlank()) {
			throw new IllegalArgumentException("Outlet code cannot be blank");
		}
		if (currency == null) {
			throw new IllegalArgumentException("Currency cannot be null");
		}
		if (status == null) {
			throw new IllegalArgumentException("Status cannot be null");
		}
		if (createdAt == null || updatedAt == null) {
			throw new IllegalArgumentException("Timestamps cannot be null");
		}
		if (timezone == null || timezone.isBlank()) {
			throw new IllegalArgumentException("Timezone cannot be blank");
		}
		if (version == null || version < 0) {
			throw new IllegalArgumentException("Version must be zero or greater");
		}

		try {
			this.timezone = ZoneId.of(timezone.trim()).toString();
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("Timezone must be a valid IANA timezone", exception);
		}

		this.id = id;
		this.restaurantChainId = restaurantChainId;
		this.displayName = displayName.trim();
		this.outletCode = outletCode.trim();
		this.currency = currency;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
	}

	public void restoreFromArchive() {
		// Reopening an outlet is deliberate, so it is not a normal forward transition.
		if (this.status != ChainStatus.ARCHIVED) {
			throw new IllegalStateException("Only an archived outlet can be restored");
		}
		applyStatus(ChainStatus.ACTIVE);
	}

	private void applyStatus(ChainStatus status) {
		this.updatedAt = Instant.now();
		this.status = status;
	}

	public void changeStatusTo(ChainStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Status cannot be null");
		}

		if (status == this.status) {
			return;
		}

		boolean validForwardTransition = (this.status == ChainStatus.DRAFT && status == ChainStatus.ACTIVE)
				|| (this.status == ChainStatus.ACTIVE && status == ChainStatus.ARCHIVED);
		if (!validForwardTransition) {
			throw new IllegalStateException("Invalid status transition from " + this.status + " to " + status);
		}

		applyStatus(status);
	}

	public Long getRestaurantChainId() {
		return restaurantChainId;
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

	public Currency getCurrency() {
		return currency;
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

	public String getTimezone() {
		return timezone;
	}

	public Long getVersion() {
		return version;
	}

}
