package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

/** Immutable message carrying a menu-item change from Catalog to Kafka. */
public final class MenuItemChangedEvent {
	private final UUID eventId;
	private final UUID originalEventId;
	private final OutboxEventType eventType;
	private final Long itemId;
	private final Long restaurantChainId;
	private final Long outletId;
	private final Long aggregateVersion;
	private final BigDecimal price;
	private final Boolean availability;
	private final Boolean visibility;
	private final Instant occurredAt;
	private final String correlationId;
	private final MenuItemStatus status;
	private final String itemName;
	private final Boolean scheduleChanged;
	private final Long scheduleVersion;
	private final String scheduleId;
	private final String scheduleTimezone;
	private final Map<String, List<EventScheduleWindow>> scheduleWindows;
	private final Map<String, List<EventScheduleWindow>> baseScheduleWindows;
	private final Map<String, List<EventScheduleWindow>> outletScheduleWindows;
	private final Boolean seasonalAvailabilityChanged;
	private final Long seasonalAvailabilityVersion;
	private final LocalDate seasonalStartDate;
	private final LocalDate seasonalEndDate;
	private final String seasonalTimezone;
	private final Boolean modifierConfigurationChanged;
	private final List<EventModifierGroup> modifierGroups;
	private final Boolean bundleConfigurationChanged;
	private final EventBundleSnapshot bundleSnapshot;

	MenuItemChangedEvent(MenuItemChangedEventBuilder builder) {
		if (builder.eventId == null) {
			throw new IllegalArgumentException("Event Id cannot be null");
		}
		if (builder.eventType == null) {
			throw new IllegalArgumentException("Event type cannot be null");
		}
		if (builder.itemId == null || builder.itemId <= 0) {
			throw new IllegalArgumentException("Item Id must be positive");
		}
		if (builder.restaurantChainId == null || builder.restaurantChainId <= 0) {
			throw new IllegalArgumentException("Restaurant Chain Id must be positive");
		}
		if (builder.aggregateVersion == null || builder.aggregateVersion < 0) {
			throw new IllegalArgumentException("Aggregate version must be non-negative");
		}
		if (builder.occurredAt == null) {
			throw new IllegalArgumentException("Occurred at cannot be null");
		}
		if (builder.price != null && builder.price.signum() < 0) {
			throw new IllegalArgumentException("Price cannot be negative");
		}
		if (builder.price == null && builder.availability == null && builder.visibility == null
				&& builder.status == null && !Boolean.TRUE.equals(builder.scheduleChanged)
				&& !Boolean.TRUE.equals(builder.seasonalAvailabilityChanged)
				&& !Boolean.TRUE.equals(builder.modifierConfigurationChanged)
				&& !Boolean.TRUE.equals(builder.bundleConfigurationChanged)) {
			throw new IllegalArgumentException("At least one changed field is required");
		}

		if (builder.status == null) {
			throw new IllegalArgumentException("Status cannot be null");
		}

		if (builder.itemName != null && builder.itemName.isBlank()) {
			throw new IllegalArgumentException("Item name cannot be blank when supplied");
		}
		if (Boolean.TRUE.equals(builder.scheduleChanged)
				&& (builder.scheduleVersion == null || builder.scheduleVersion < 0)) {
			throw new IllegalArgumentException("Schedule version must be non-negative when schedule changed");
		}
		if (Boolean.TRUE.equals(builder.scheduleChanged)
				&& (builder.scheduleTimezone == null || builder.scheduleTimezone.isBlank())) {
			throw new IllegalArgumentException("Schedule timezone is required when schedule changed");
		}
		if (Boolean.TRUE.equals(builder.seasonalAvailabilityChanged)
				&& (builder.seasonalAvailabilityVersion == null || builder.seasonalAvailabilityVersion < 0)) {
			throw new IllegalArgumentException("Seasonal availability version must be non-negative when changed");
		}
		if (Boolean.TRUE.equals(builder.seasonalAvailabilityChanged)
				&& (builder.seasonalTimezone == null || builder.seasonalTimezone.isBlank())) {
			throw new IllegalArgumentException("Seasonal availability timezone is required when changed");
		}
		if ((builder.seasonalStartDate == null) != (builder.seasonalEndDate == null)
				|| (builder.seasonalStartDate != null
						&& !builder.seasonalStartDate.isBefore(builder.seasonalEndDate))) {
			throw new IllegalArgumentException("Seasonal availability dates must be an increasing complete range");
		}
		if (Boolean.TRUE.equals(builder.modifierConfigurationChanged) && builder.modifierGroups == null) {
			throw new IllegalArgumentException("Modifier groups are required when modifier configuration changed");
		}
		if (Boolean.TRUE.equals(builder.modifierConfigurationChanged)
				&& builder.eventType != OutboxEventType.MODIFIER_CONFIGURATION_CHANGED) {
			throw new IllegalArgumentException(
					"Modifier configuration changes must use the modifier configuration event type");
		}
		if (builder.eventType == OutboxEventType.MODIFIER_CONFIGURATION_CHANGED
				&& !Boolean.TRUE.equals(builder.modifierConfigurationChanged)) {
			throw new IllegalArgumentException(
					"Modifier configuration event type requires a modifier configuration snapshot");
		}
		if (Boolean.TRUE.equals(builder.modifierConfigurationChanged)) {
			validateModifierGroupDisplayOrders(builder.modifierGroups);
		}
		if (Boolean.TRUE.equals(builder.bundleConfigurationChanged) && builder.bundleSnapshot == null) {
			throw new IllegalArgumentException("Bundle snapshot is required when bundle configuration changed");
		}
		if (!Boolean.TRUE.equals(builder.bundleConfigurationChanged) && builder.bundleSnapshot != null) {
			throw new IllegalArgumentException(
					"Bundle snapshot must be absent when bundle configuration did not change");
		}
		if (builder.bundleSnapshot != null && !builder.itemId.equals(builder.bundleSnapshot.menuItemId())) {
			throw new IllegalArgumentException("Bundle snapshot must belong to the changed menu item");
		}
		if (Boolean.TRUE.equals(builder.bundleConfigurationChanged)
				&& builder.eventType != OutboxEventType.BUNDLE_CONFIGURATION_CHANGED) {
			throw new IllegalArgumentException(
					"Bundle configuration changes must use the bundle configuration event type");
		}
		if (builder.eventType == OutboxEventType.BUNDLE_CONFIGURATION_CHANGED
				&& !Boolean.TRUE.equals(builder.bundleConfigurationChanged)) {
			throw new IllegalArgumentException("Bundle configuration event type requires a bundle snapshot");
		}
		this.eventId = builder.eventId;
		this.originalEventId = builder.originalEventId;
		this.eventType = builder.eventType;
		this.itemId = builder.itemId;
		this.restaurantChainId = builder.restaurantChainId;
		this.outletId = builder.outletId;
		this.aggregateVersion = builder.aggregateVersion;
		this.price = builder.price;
		this.availability = builder.availability;
		this.visibility = builder.visibility;
		this.occurredAt = builder.occurredAt;
		this.correlationId = builder.correlationId;
		this.status = builder.status;
		this.itemName = builder.itemName;
		this.scheduleChanged = builder.scheduleChanged;
		this.scheduleVersion = builder.scheduleVersion;
		this.scheduleId = Boolean.TRUE.equals(builder.scheduleChanged)
				? builder.itemId + ":" + (builder.outletId == null ? "base" : builder.outletId)
				: null;
		this.scheduleTimezone = builder.scheduleTimezone;
		this.scheduleWindows = builder.scheduleWindows == null ? Map.of() : Map.copyOf(builder.scheduleWindows);
		this.baseScheduleWindows = builder.baseScheduleWindows == null
				? Map.of()
				: Map.copyOf(builder.baseScheduleWindows);
		this.outletScheduleWindows = builder.outletScheduleWindows == null
				? Map.of()
				: Map.copyOf(builder.outletScheduleWindows);
		this.seasonalAvailabilityChanged = builder.seasonalAvailabilityChanged;
		this.seasonalAvailabilityVersion = builder.seasonalAvailabilityVersion;
		this.seasonalStartDate = builder.seasonalStartDate;
		this.seasonalEndDate = builder.seasonalEndDate;
		this.seasonalTimezone = builder.seasonalTimezone;
		this.modifierConfigurationChanged = Boolean.TRUE.equals(builder.modifierConfigurationChanged);
		this.modifierGroups = builder.modifierGroups == null ? List.of() : List.copyOf(builder.modifierGroups);
		this.bundleConfigurationChanged = Boolean.TRUE.equals(builder.bundleConfigurationChanged);
		this.bundleSnapshot = builder.bundleSnapshot;
	}

	/** Starts named construction of a versioned Catalog event. */
	public static MenuItemChangedEventBuilder builder() {
		return new MenuItemChangedEventBuilder();
	}

	private static void validateModifierGroupDisplayOrders(List<EventModifierGroup> modifierGroups) {
		Set<Integer> displayOrders = new HashSet<>();
		for (EventModifierGroup modifierGroup : modifierGroups) {
			if (modifierGroup == null) {
				throw new IllegalArgumentException("Modifier groups cannot contain null values");
			}
			if (!displayOrders.add(modifierGroup.displayOrder())) {
				throw new IllegalArgumentException("Modifier group display orders must be unique");
			}
		}
	}

	public UUID getEventId() {
		return eventId;
	}

	public UUID getOriginalEventId() {
		return originalEventId;
	}

	public OutboxEventType getEventType() {
		return eventType;
	}

	public Long getItemId() {
		return itemId;
	}

	public Long getRestaurantChainId() {
		return restaurantChainId;
	}

	public Long getOutletId() {
		return outletId;
	}

	public Long getAggregateVersion() {
		return aggregateVersion;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Boolean getAvailability() {
		return availability;
	}

	public Boolean getVisibility() {
		return visibility;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public MenuItemStatus getStatus() {
		return status;
	}

	public String getItemName() {
		return itemName;
	}

	public Boolean getScheduleChanged() {
		return scheduleChanged;
	}

	public Long getScheduleVersion() {
		return scheduleVersion;
	}

	/**
	 * Stable schedule scope identifier: one base or one outlet override per item.
	 */
	public String getScheduleId() {
		return scheduleId;
	}

	public String getScheduleTimezone() {
		return scheduleTimezone;
	}

	public Map<String, List<EventScheduleWindow>> getScheduleWindows() {
		return scheduleWindows;
	}

	public Map<String, List<EventScheduleWindow>> getBaseScheduleWindows() {
		return baseScheduleWindows;
	}

	public Map<String, List<EventScheduleWindow>> getOutletScheduleWindows() {
		return outletScheduleWindows;
	}

	public Boolean getSeasonalAvailabilityChanged() {
		return seasonalAvailabilityChanged;
	}

	public Long getSeasonalAvailabilityVersion() {
		return seasonalAvailabilityVersion;
	}

	public LocalDate getSeasonalStartDate() {
		return seasonalStartDate;
	}

	public LocalDate getSeasonalEndDate() {
		return seasonalEndDate;
	}

	public String getSeasonalTimezone() {
		return seasonalTimezone;
	}

	/**
	 * True means modifierGroups is the complete replacement configuration for this
	 * item.
	 */
	public Boolean getModifierConfigurationChanged() {
		return modifierConfigurationChanged;
	}

	/**
	 * Complete replacement snapshot; an empty list intentionally removes all
	 * groups.
	 */
	public List<EventModifierGroup> getModifierGroups() {
		return modifierGroups;
	}

	/**
	 * True means bundleSnapshot is the complete replacement structure for this
	 * bundle.
	 */
	public Boolean getBundleConfigurationChanged() {
		return bundleConfigurationChanged;
	}

	/**
	 * Complete nested replacement snapshot for the root bundle; null when a bundle
	 * did not change.
	 */
	public EventBundleSnapshot getBundleSnapshot() {
		return bundleSnapshot;
	}

}
