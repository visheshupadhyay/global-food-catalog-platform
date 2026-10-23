package com.vishesh.foodcatalog.catalog.application;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

/** Applies business rules before a menu item is saved. */
@Service
public class MenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final RestaurantChainRepository restaurantChainRepository;
    private final OutletRepository outletRepository;
    private final MenuGroupRepository menuGroupRepository;

    public MenuItemService(MenuItemRepository menuItemRepository,
            RestaurantChainRepository restaurantChainRepository,
            OutletRepository outletRepository,
            MenuGroupRepository menuGroupRepository) {

        this.menuItemRepository = menuItemRepository;
        this.restaurantChainRepository = restaurantChainRepository;
        this.outletRepository = outletRepository;
        this.menuGroupRepository = menuGroupRepository;
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
}
