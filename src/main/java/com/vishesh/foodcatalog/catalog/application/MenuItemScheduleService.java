package com.vishesh.foodcatalog.catalog.application;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.api.CreateScheduleRequest;
import com.vishesh.foodcatalog.catalog.api.UpdateScheduleRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;
import com.vishesh.foodcatalog.catalog.domain.ScheduleWindow;
import com.vishesh.foodcatalog.catalog.domain.Weekday;
import com.vishesh.foodcatalog.catalog.event.application.CatalogEventOutboxWriter;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.domain.EventScheduleWindow;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemScheduleRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;

/**
 * Validates schedule ownership and delegates durable replacement to the
 * repository.
 */
@Service
public class MenuItemScheduleService {
	private final MenuItemRepository menuItems;
	private final OutletRepository outlets;
	private final MenuItemScheduleRepository schedules;
	private final MenuItemChangedEventFactory eventFactory;
	private final CatalogEventOutboxWriter outboxWriter;
	private final BundleRefreshPublisher bundleRefreshPublisher;

	public MenuItemScheduleService(MenuItemRepository menuItems, OutletRepository outlets,
			MenuItemScheduleRepository schedules, MenuItemChangedEventFactory eventFactory,
			CatalogEventOutboxWriter outboxWriter, BundleRefreshPublisher bundleRefreshPublisher) {
		this.menuItems = menuItems;
		this.outlets = outlets;
		this.schedules = schedules;
		this.eventFactory = eventFactory;
		this.outboxWriter = outboxWriter;
		this.bundleRefreshPublisher = bundleRefreshPublisher;
	}

	@Transactional
	public MenuItemSchedule save(Long menuItemId, CreateScheduleRequest request) {
		if (menuItemId == null) {
			throw new IllegalArgumentException("Menu item ID is required");
		}
		var menuItem = menuItems.findById(menuItemId)
				.orElseThrow(() -> new IllegalArgumentException("Menu item was not found"));

		if (request == null) {
			throw new IllegalArgumentException("Schedule request is required");
		}

		Long outletId = request.outletId();
		if (outletId != null) {
			Long chainId = menuItem.getRestaurantChainId();
			boolean outletBelongsToChain = outlets.existsByIdAndRestaurantChainId(outletId, chainId);
			if (!outletBelongsToChain) {
				throw new IllegalArgumentException("Outlet does not belong to the menu item's restaurant chain");
			}
		}

		EnumMap<Weekday, List<ScheduleWindow>> windowsByDay = new EnumMap<>(Weekday.class);
		if (request.windows() != null) {
			for (var dayEntry : request.windows().entrySet()) {
				Weekday weekday = dayEntry.getKey();
				List<ScheduleWindow> windowsForDay = new ArrayList<>();

				for (CreateScheduleRequest.Window window : dayEntry.getValue()) {
					windowsForDay.add(new ScheduleWindow(window.startTime(), window.endTime()));
				}

				windowsByDay.put(weekday, windowsForDay);
			}
		}

		MenuItemSchedule schedule = new MenuItemSchedule(menuItemId, outletId, windowsByDay);

		MenuItemSchedule savedSchedule = schedules.save(schedule);

		List<Long> affectedOutletIds = outletId == null
				? menuItems.findAffectedOutletIds(menuItemId)
				: List.of(outletId);
		if (affectedOutletIds.isEmpty()) {
			throw new IllegalArgumentException("No affected outlets were found for the menu item");
		}

		String correlationId = UUID.randomUUID().toString();
		for (Long affectedOutletId : affectedOutletIds) {
			MenuItemSchedule outletSchedule = schedules.find(menuItemId, affectedOutletId).orElse(null);
			MenuItemSchedule baseSchedule = schedules.find(menuItemId, null).orElse(null);
			MenuItemSchedule effectiveSchedule = outletSchedule != null ? outletSchedule : baseSchedule;
			Map<String, List<EventScheduleWindow>> eventWindows = toEffectiveEventWindows(baseSchedule,
					effectiveSchedule);
			Map<String, List<EventScheduleWindow>> baseWindows = toEventWindows(baseSchedule);
			Map<String, List<EventScheduleWindow>> outletWindows = toEventWindows(outletSchedule);
			String timezone = outlets.findTimezoneById(affectedOutletId)
					.orElseThrow(() -> new IllegalArgumentException("Outlet was not found"));
			MenuItemChangedEvent event = eventFactory.create(menuItemId, menuItem.getRestaurantChainId(),
					savedSchedule.getVersion(), null, menuItem.isAvailability(), null, affectedOutletId, correlationId,
					menuItem.getStatus(), null, true, savedSchedule.getVersion(), timezone, eventWindows, baseWindows,
					outletWindows);
			outboxWriter.save(event);
		}
		bundleRefreshPublisher.publishAncestorBundleRefreshes(menuItemId, menuItem.getRestaurantChainId(),
				correlationId, outletId == null ? null : List.of(outletId));
		return savedSchedule;
	}

	/** Replaces a schedule only when the caller still holds its current version. */
	@Transactional
	public MenuItemSchedule update(Long menuItemId, UpdateScheduleRequest request) {
		if (request == null || request.expectedVersion() == null || request.request() == null) {
			throw new IllegalArgumentException("Expected schedule version and schedule body are required");
		}
		MenuItemSchedule current = schedules.find(menuItemId, request.outletId())
				.orElseThrow(() -> new IllegalArgumentException("Schedule was not found"));
		if (!request.expectedVersion().equals(current.getVersion())) {
			throw new IllegalArgumentException("Schedule version is stale");
		}
		CreateScheduleRequest body = request.request();
		if (!Objects.equals(request.outletId(), body.outletId())) {
			throw new IllegalArgumentException("Schedule outlet ID cannot change");
		}
		return save(menuItemId, body);
	}

	/** Deletes one schedule scope by replacing it with an unrestricted schedule. */
	@Transactional
	public void delete(Long menuItemId, Long outletId) {
		save(menuItemId, new CreateScheduleRequest(outletId, Map.of()));
	}

	private Map<String, List<EventScheduleWindow>> toEffectiveEventWindows(MenuItemSchedule baseSchedule,
			MenuItemSchedule outletSchedule) {
		Map<String, List<EventScheduleWindow>> result = new HashMap<>();
		for (Weekday day : Weekday.values()) {
			List<ScheduleWindow> windows = outletSchedule == null ? List.of() : outletSchedule.getWindows(day);
			if (windows.isEmpty() && baseSchedule != null) {
				windows = baseSchedule.getWindows(day);
			}
			result.put(day.name(), windows.stream()
					.map(window -> new EventScheduleWindow(window.getStartTime(), window.getEndTime())).toList());
		}
		return result;
	}

	private Map<String, List<EventScheduleWindow>> toEventWindows(MenuItemSchedule schedule) {
		if (schedule == null) {
			return Map.of();
		}
		Map<String, List<EventScheduleWindow>> result = new HashMap<>();
		schedule.getWindowsByDay().forEach((day, windows) -> result.put(day.name(), windows.stream()
				.map(window -> new EventScheduleWindow(window.getStartTime(), window.getEndTime())).toList()));
		return result;
	}

	@Transactional(readOnly = true)
	public Optional<MenuItemSchedule> find(Long menuItemId, Long outletId) {
		return schedules.find(menuItemId, outletId);
	}
}
