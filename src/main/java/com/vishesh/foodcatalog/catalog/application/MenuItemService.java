package com.vishesh.foodcatalog.catalog.application;

import java.math.BigDecimal;
import java.util.Optional;

import tools.jackson.core.JacksonException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

import tools.jackson.databind.ObjectMapper;

/** Applies business rules before a menu item is saved. */
@Service
public class MenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final RestaurantChainRepository restaurantChainRepository;
    private final OutletRepository outletRepository;
    private final MenuGroupRepository menuGroupRepository;

    private final MenuItemChangedEventFactory menuItemChangedEventFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public MenuItemService(MenuItemRepository menuItemRepository,
            RestaurantChainRepository restaurantChainRepository,
            OutletRepository outletRepository,
            MenuGroupRepository menuGroupRepository,
            MenuItemChangedEventFactory menuItemChangedEventFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {

        this.menuItemRepository = menuItemRepository;
        this.restaurantChainRepository = restaurantChainRepository;
        this.outletRepository = outletRepository;
        this.menuGroupRepository = menuGroupRepository;
        this.menuItemChangedEventFactory = menuItemChangedEventFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public MenuItem findMenuItemById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Menu item ID must be greater than zero");
        }
        Optional<MenuItem> o = menuItemRepository.findById(id);

        if (o.isEmpty()) {
            throw new MenuItemNotFoundException("Menu item with ID: " + id + " not found");
        }
        return o.get();
    }

    public MenuItem createMenuItem(String businessCode,
            String itemName,
            BigDecimal itemPrice,
            Long restaurantChainId,
            Long menuGroupId,
            Long menuCategoryId,
            Long outletId) {

        if (businessCode == null || businessCode.isBlank()) {
            throw new IllegalArgumentException("Business code cannot be null ");
        }

        businessCode = businessCode.trim();

        if (businessCode.isBlank()) {
            throw new IllegalArgumentException("Business code cannot be blank");
        }

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null");
        }

        itemName = itemName.trim();

        if (itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank");
        }

        if (itemPrice == null || itemPrice.signum() < 0) {
            throw new IllegalArgumentException("Item price must be zero or greater");
        }
        if (restaurantChainId == null || restaurantChainId <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }
        if ((menuGroupId == null) == (outletId == null)) {
            throw new IllegalArgumentException("Exactly one of menu group ID or outlet ID is required");
        }
        if (menuGroupId != null && menuGroupId <= 0) {
            throw new IllegalArgumentException("Menu group ID must be greater than zero");
        }
        if (outletId != null && outletId <= 0) {
            throw new IllegalArgumentException("Outlet ID must be greater than zero");
        }
        if (menuCategoryId != null && menuCategoryId <= 0) {
            throw new IllegalArgumentException("Menu category ID must be greater than zero");
        }

        if (restaurantChainRepository.findById(restaurantChainId).isEmpty()) {
            throw new RestaurantChainNotFoundException("Restaurant Chain not found!");
        }
        if (menuGroupId != null
                && menuGroupRepository.findByIdAndRestaurantChainId(menuGroupId, restaurantChainId).isEmpty()) {
            throw new MenuGroupNotFoundException("Menu Group " + menuGroupId
                    + " was not found for restaurant chain " + restaurantChainId);
        }
        if (outletId != null
                && !outletRepository.existsByIdAndRestaurantChainId(outletId, restaurantChainId)) {
            throw new OutletNotFoundException("Outlet " + outletId
                    + " was not found for restaurant chain " + restaurantChainId);
        }
        if (menuItemRepository.existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId)) {
            throw new DuplicateMenuItemException("Restaurant Chain with id: " + restaurantChainId
                    + "  and with businessCode: " + businessCode + " already exists!");
        }

        MenuItem menuItem = new MenuItem(businessCode, itemName, itemPrice, restaurantChainId, menuGroupId,
                menuCategoryId, outletId);

        return menuItemRepository.save(menuItem);
    }

    /** Updates the item and creates its outbox event in the same transaction. */
    @Transactional
    public MenuItem updateMenuItem(Long menuItemId, Long restaurantChainId, MenuItemUpdateRequest request) {
        if (menuItemId == null || menuItemId <= 0) {
            throw new IllegalArgumentException("Menu item ID must be greater than zero");
        }

        if (restaurantChainId == null || restaurantChainId <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }

        if (request == null) {
            throw new IllegalArgumentException("Update request must be present");
        }

        if (restaurantChainRepository.findById(restaurantChainId).isEmpty()) {
            throw new RestaurantChainNotFoundException(
                    "Restaurant chain with ID " + restaurantChainId + " was not found");
        }

        MenuItem menuItem = menuItemRepository
                .findByIdAndRestaurantChainId(menuItemId, restaurantChainId)
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item with ID " + menuItemId + " was not found for restaurant chain "
                                + restaurantChainId));

        if (!menuItem.getVersion().equals(request.getExpectedVersion())) {
            throw new MenuItemVersionConflictException(
                    "Menu item version is stale; expected " + menuItem.getVersion()
                            + " but received " + request.getExpectedVersion());
        }

        BigDecimal updatedPrice = request.getPrice();
        Boolean updatedAvailability = request.getAvailability();
        Boolean updatedVisibility = request.getVisibility();
        if (updatedPrice != null) {
            menuItem.updatePrice(updatedPrice);
        }

        if (updatedAvailability != null) {
            menuItem.updateAvailability(updatedAvailability);
        }

        if (updatedVisibility != null) {
            menuItem.updateVisibility(updatedVisibility);
        }

        MenuItem savedItem = menuItemRepository.save(menuItem);
        MenuItemChangedEvent menuItemChangedEvent = menuItemChangedEventFactory.create(
                savedItem.getId(),
                savedItem.getRestaurantChainId(),
                savedItem.getVersion(),
                savedItem.getItemPrice(),
                savedItem.isAvailability(),
                savedItem.isVisibility(),
                savedItem.getOutletId(),
                null);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(menuItemChangedEvent);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize menu-item change event", exception);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                null,
                menuItemChangedEvent.getEventId(),
                menuItemChangedEvent.getOriginalEventId(),
                menuItemChangedEvent.getEventType(),
                String.valueOf(savedItem.getId()),
                payload,
                OutboxEventStatus.PENDING,
                0L,
                null,
                null,
                menuItemChangedEvent.getOccurredAt(),
                null,
                null,
                savedItem.getId(),
                savedItem.getVersion());

        outboxEventRepository.save(outboxEvent);
        return savedItem;

    }
}
