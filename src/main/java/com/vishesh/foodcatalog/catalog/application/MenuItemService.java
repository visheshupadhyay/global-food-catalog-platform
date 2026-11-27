package com.vishesh.foodcatalog.catalog.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.CatalogEventOutboxWriter;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupOutletRepository;
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
	private final MenuGroupOutletRepository menuGroupOutletRepository;
	private final MenuItemChangedEventFactory menuItemChangedEventFactory;
	private final CatalogEventOutboxWriter outboxWriter;
	private final BundleRefreshPublisher bundleRefreshPublisher;

	public MenuItemService(@Qualifier("menuItemPersistenceAdapter") MenuItemRepository menuItemRepository,
			@Qualifier("restaurantChainPersistenceAdapter") RestaurantChainRepository restaurantChainRepository,
			@Qualifier("outletPersistenceAdapter") OutletRepository outletRepository,
			@Qualifier("menuGroupPersistenceAdapter") MenuGroupRepository menuGroupRepository,
			MenuItemChangedEventFactory menuItemChangedEventFactory, CatalogEventOutboxWriter outboxWriter,
			@Qualifier("menuGroupOutletPersistenceAdapter") MenuGroupOutletRepository menuGroupOutletRepository,
			BundleRefreshPublisher bundleRefreshPublisher) {

		this.menuItemRepository = menuItemRepository;
		this.restaurantChainRepository = restaurantChainRepository;
		this.outletRepository = outletRepository;
		this.menuGroupRepository = menuGroupRepository;
		this.menuItemChangedEventFactory = menuItemChangedEventFactory;
		this.outboxWriter = outboxWriter;
		this.menuGroupOutletRepository = menuGroupOutletRepository;
		this.bundleRefreshPublisher = bundleRefreshPublisher;
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

	public MenuItem createMenuItem(String businessCode, String itemName, BigDecimal itemPrice, Long restaurantChainId,
			Long menuGroupId, Long menuCategoryId, Long outletId) {

		// Existing callers represent ordinary products. Keeping this overload
		// preserves their behavior while exposing an explicit BUNDLE path.
		return createMenuItem(businessCode, itemName, itemPrice, restaurantChainId, menuGroupId, menuCategoryId,
				outletId, MenuItemKind.PRODUCT);
	}

	/** Creates either a normal product or a bundle root. */
	public MenuItem createMenuItem(String businessCode, String itemName, BigDecimal itemPrice, Long restaurantChainId,
			Long menuGroupId, Long menuCategoryId, Long outletId, MenuItemKind menuItemKind) {

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
		if (menuItemKind == null) {
			throw new IllegalArgumentException("Menu item kind must be present");
		}

		if (restaurantChainRepository.findById(restaurantChainId).isEmpty()) {
			throw new RestaurantChainNotFoundException("Restaurant Chain not found!");
		}
		if (menuGroupId != null
				&& menuGroupRepository.findByIdAndRestaurantChainId(menuGroupId, restaurantChainId).isEmpty()) {
			throw new MenuGroupNotFoundException(
					"Menu Group " + menuGroupId + " was not found for restaurant chain " + restaurantChainId);
		}
		if (outletId != null && !outletRepository.existsByIdAndRestaurantChainId(outletId, restaurantChainId)) {
			throw new OutletNotFoundException(
					"Outlet " + outletId + " was not found for restaurant chain " + restaurantChainId);
		}
		if (menuItemRepository.existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId)) {
			throw new DuplicateMenuItemException("Restaurant Chain with id: " + restaurantChainId
					+ "  and with businessCode: " + businessCode + " already exists!");
		}

		MenuItem menuItem = new MenuItem(businessCode, itemName, itemPrice, restaurantChainId, menuGroupId,
				menuCategoryId, outletId, menuItemKind);

		return menuItemRepository.save(menuItem);
	}

	/** Updates the item and creates one outlet event in the same transaction. */
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

		MenuItem menuItem = menuItemRepository.findByIdAndRestaurantChainId(menuItemId, restaurantChainId)
				.orElseThrow(() -> new MenuItemNotFoundException("Menu item with ID " + menuItemId
						+ " was not found for restaurant chain " + restaurantChainId));

		if (!menuItem.getVersion().equals(request.getExpectedVersion())) {
			throw new MenuItemVersionConflictException("Menu item version is stale; expected " + menuItem.getVersion()
					+ " but received " + request.getExpectedVersion());
		}

		MenuItemStatus previousStatus = menuItem.getStatus();
		applyRequestedChanges(menuItem, request);

		MenuItem savedItem = menuItemRepository.save(menuItem);
		List<Long> outletIds = resolveAffectedOutletIds(savedItem);

		// A draft item is intentionally private to Catalog. Its first customer-facing
		// event is the complete activation snapshot, not a sparse update.
		boolean activated = previousStatus == MenuItemStatus.DRAFT && savedItem.getStatus() == MenuItemStatus.ACTIVE;
		boolean remainsDraft = savedItem.getStatus() == MenuItemStatus.DRAFT;
		boolean statusChanged = request.getStatus() != null && previousStatus != savedItem.getStatus();

		// A lifecycle transition must carry the stored visibility. In particular,
		// HIDDEN -> ACTIVE needs to restore the item's real visibility downstream.
		Boolean eventVisibility = statusChanged ? Boolean.valueOf(savedItem.isVisibility()) : request.getVisibility();

		// One admin action may emit the changed child plus multiple parent
		// bundles. A shared correlation ID keeps that fan-out traceable.
		String correlationId = UUID.randomUUID().toString();
		if (!remainsDraft) {
			for (Long outletId : outletIds) {
				MenuItemChangedEvent menuItemChangedEvent = activated
						? menuItemChangedEventFactory.createMenuItemActivated(savedItem.getId(),
								savedItem.getRestaurantChainId(), savedItem.getVersion(), savedItem.getItemPrice(),
								savedItem.isAvailability(), savedItem.isVisibility(), outletId, correlationId,
								savedItem.getStatus(), savedItem.getItemName())
						: menuItemChangedEventFactory.create(savedItem.getId(), savedItem.getRestaurantChainId(),
								savedItem.getVersion(), request.getPrice(), request.getAvailability(), eventVisibility,
								outletId, correlationId, savedItem.getStatus(), null);

				outboxWriter.save(menuItemChangedEvent);
			}
		}

		// A child can be reused by multiple meals/combos. Refreshing those
		// complete snapshots prevents Effective Menu from serving stale child
		// details without ever reading Catalog tables directly.
		bundleRefreshPublisher.publishAncestorBundleRefreshes(savedItem.getId(), savedItem.getRestaurantChainId(),
				correlationId);
		return savedItem;

	}

	/**
	 * Applies only the fields an administrator supplied; null means "leave it
	 * unchanged".
	 */
	private void applyRequestedChanges(MenuItem menuItem, MenuItemUpdateRequest request) {
		if (request.getPrice() != null) {
			menuItem.updatePrice(request.getPrice());
		}
		if (request.getAvailability() != null) {
			menuItem.updateAvailability(request.getAvailability());
		}
		if (request.getVisibility() != null) {
			menuItem.updateVisibility(request.getVisibility());
		}
		if (request.getStatus() != null) {
			menuItem.updateStatus(request.getStatus());
		}
	}

	/**
	 * A local item affects one outlet; a shared item affects every assigned outlet.
	 */
	private List<Long> resolveAffectedOutletIds(MenuItem menuItem) {
		if (menuItem.getOutletId() != null) {
			return List.of(menuItem.getOutletId());
		}
		return menuGroupOutletRepository.findOutletIdsByMenuGroupId(menuItem.getMenuGroupId());
	}
}
