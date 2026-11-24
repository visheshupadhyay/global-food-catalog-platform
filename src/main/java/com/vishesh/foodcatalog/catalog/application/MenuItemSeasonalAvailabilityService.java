package com.vishesh.foodcatalog.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vishesh.foodcatalog.catalog.api.SeasonalAvailabilityRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemSeasonalAvailabilityRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;

/**
 * Replaces a seasonal date-range rule and writes its Kafka outbox events in
 * the same transaction. A missing date range intentionally clears the rule.
 */
@Service
public class MenuItemSeasonalAvailabilityService {
    private final MenuItemRepository menuItems;
    private final OutletRepository outlets;
    private final MenuItemSeasonalAvailabilityRepository seasons;
    private final MenuItemChangedEventFactory eventFactory;
    private final OutboxEventFactory outboxFactory;
    private final OutboxEventRepository outboxEvents;
    private final ObjectMapper objectMapper;
    private final BundleRefreshPublisher bundleRefreshPublisher;

    public MenuItemSeasonalAvailabilityService(MenuItemRepository menuItems, OutletRepository outlets,
            MenuItemSeasonalAvailabilityRepository seasons, MenuItemChangedEventFactory eventFactory,
            OutboxEventFactory outboxFactory, OutboxEventRepository outboxEvents, ObjectMapper objectMapper,
            BundleRefreshPublisher bundleRefreshPublisher) {
        this.menuItems = menuItems;
        this.outlets = outlets;
        this.seasons = seasons;
        this.eventFactory = eventFactory;
        this.outboxFactory = outboxFactory;
        this.outboxEvents = outboxEvents;
        this.objectMapper = objectMapper;
        this.bundleRefreshPublisher = bundleRefreshPublisher;
    }

    @Transactional
    public MenuItemSeasonalAvailability save(Long menuItemId, SeasonalAvailabilityRequest request) {
        if (request == null)
            throw new IllegalArgumentException("Seasonal availability request is required");
        var menuItem = menuItems.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item was not found"));
        if (request.outletId() != null && !outlets.existsByIdAndRestaurantChainId(
                request.outletId(), menuItem.getRestaurantChainId())) {
            throw new IllegalArgumentException("Outlet does not belong to the menu item's restaurant chain");
        }
        seasons.find(menuItemId, request.outletId()).ifPresent(current -> {
            if (request.expectedVersion() == null || !request.expectedVersion().equals(current.getVersion())) {
                throw new IllegalArgumentException("Seasonal availability version is stale");
            }
        });
        MenuItemSeasonalAvailability saved = seasons.save(new MenuItemSeasonalAvailability(menuItemId,
                request.outletId(), request.startDate(), request.endDate(), 0L));
        // The customer-visible availability rules are part of the menu-item aggregate.
        // Persisting this touch makes the event version strictly newer than the prior snapshot.
        menuItem.markAvailabilityRulesChanged();
        menuItem = menuItems.save(menuItem);
        List<Long> affected = request.outletId() == null ? menuItems.findAffectedOutletIds(menuItemId)
                : List.of(request.outletId());
        if (affected.isEmpty())
            throw new IllegalArgumentException("No affected outlets were found for the menu item");
        String correlationId = UUID.randomUUID().toString();
        try {
            for (Long outletId : affected) {
                MenuItemSeasonalAvailability outletSeason = seasons.find(menuItemId, outletId).orElse(null);
                MenuItemSeasonalAvailability baseSeason = seasons.find(menuItemId, null).orElse(null);
                MenuItemSeasonalAvailability effective = outletSeason != null ? outletSeason : baseSeason;
                String timezone = outlets.findTimezoneById(outletId)
                        .orElseThrow(() -> new IllegalArgumentException("Outlet was not found"));
                MenuItemChangedEvent event = eventFactory.createSeasonalAvailabilityChanged(menuItemId,
                        menuItem.getRestaurantChainId(), menuItem.getVersion(), saved.getVersion(), outletId, correlationId,
                        menuItem.getStatus(), menuItem.isAvailability(), timezone,
                        effective == null ? null : effective.getStartDate(),
                        effective == null ? null : effective.getEndDate());
                outboxEvents.save(outboxFactory.create(event, objectMapper.writeValueAsString(event)));
            }
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize seasonal availability event", exception);
        }
        bundleRefreshPublisher.publishAncestorBundleRefreshes(
                menuItemId, menuItem.getRestaurantChainId(), correlationId,
                request.outletId() == null ? null : List.of(request.outletId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<MenuItemSeasonalAvailability> find(Long menuItemId, Long outletId) {
        return seasons.find(menuItemId, outletId);
    }
}
