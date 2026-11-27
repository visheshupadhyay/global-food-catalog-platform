package com.vishesh.foodcatalog.catalog.event.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

/**
 * Readable, named construction for {@link MenuItemChangedEvent}.
 *
 * <p>
 * The event is deliberately broad because one versioned message can carry an
 * item, schedule, seasonal-availability, modifier, or bundle change. Grouped
 * methods make each event's intent visible at its creation site.
 * </p>
 */
public final class MenuItemChangedEventBuilder {
	UUID eventId;
	UUID originalEventId;
	OutboxEventType eventType;
	Long itemId;
	Long restaurantChainId;
	Long outletId;
	Long aggregateVersion;
	BigDecimal price;
	Boolean availability;
	Boolean visibility;
	Instant occurredAt;
	String correlationId;
	MenuItemStatus status;
	String itemName;
	Boolean scheduleChanged = false;
	Long scheduleVersion;
	String scheduleTimezone;
	Map<String, List<EventScheduleWindow>> scheduleWindows = Map.of();
	Map<String, List<EventScheduleWindow>> baseScheduleWindows = Map.of();
	Map<String, List<EventScheduleWindow>> outletScheduleWindows = Map.of();
	Boolean seasonalAvailabilityChanged = false;
	Long seasonalAvailabilityVersion;
	LocalDate seasonalStartDate;
	LocalDate seasonalEndDate;
	String seasonalTimezone;
	Boolean modifierConfigurationChanged = false;
	List<EventModifierGroup> modifierGroups = List.of();
	Boolean bundleConfigurationChanged = false;
	EventBundleSnapshot bundleSnapshot;

	public MenuItemChangedEventBuilder identity(UUID eventId, UUID originalEventId, OutboxEventType eventType) {
		this.eventId = eventId;
		this.originalEventId = originalEventId;
		this.eventType = eventType;
		return this;
	}

	public MenuItemChangedEventBuilder target(Long itemId, Long restaurantChainId, Long outletId,
			Long aggregateVersion) {
		this.itemId = itemId;
		this.restaurantChainId = restaurantChainId;
		this.outletId = outletId;
		this.aggregateVersion = aggregateVersion;
		return this;
	}

	public MenuItemChangedEventBuilder itemChange(BigDecimal price, Boolean availability, Boolean visibility,
			MenuItemStatus status, String itemName) {
		this.price = price;
		this.availability = availability;
		this.visibility = visibility;
		this.status = status;
		this.itemName = itemName;
		return this;
	}

	public MenuItemChangedEventBuilder metadata(Instant occurredAt, String correlationId) {
		this.occurredAt = occurredAt;
		this.correlationId = correlationId;
		return this;
	}

	public MenuItemChangedEventBuilder schedule(Boolean changed, Long version, String timezone,
			Map<String, List<EventScheduleWindow>> windows, Map<String, List<EventScheduleWindow>> baseWindows,
			Map<String, List<EventScheduleWindow>> outletWindows) {
		this.scheduleChanged = changed;
		this.scheduleVersion = version;
		this.scheduleTimezone = timezone;
		this.scheduleWindows = windows;
		this.baseScheduleWindows = baseWindows;
		this.outletScheduleWindows = outletWindows;
		return this;
	}

	public MenuItemChangedEventBuilder seasonalAvailability(Boolean changed, Long version, LocalDate startDate,
			LocalDate endDate, String timezone) {
		this.seasonalAvailabilityChanged = changed;
		this.seasonalAvailabilityVersion = version;
		this.seasonalStartDate = startDate;
		this.seasonalEndDate = endDate;
		this.seasonalTimezone = timezone;
		return this;
	}

	public MenuItemChangedEventBuilder modifiers(Boolean changed, List<EventModifierGroup> groups) {
		this.modifierConfigurationChanged = changed;
		this.modifierGroups = groups;
		return this;
	}

	public MenuItemChangedEventBuilder bundle(Boolean changed, EventBundleSnapshot snapshot) {
		this.bundleConfigurationChanged = changed;
		this.bundleSnapshot = snapshot;
		return this;
	}

	public MenuItemChangedEvent build() {
		return new MenuItemChangedEvent(this);
	}
}
