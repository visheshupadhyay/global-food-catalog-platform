package com.vishesh.foodcatalog.catalog.application;

import java.util.Currency;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.domain.Outlet;
import com.vishesh.foodcatalog.catalog.event.application.OutletMenuInitializedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.CatalogEventOutboxWriter;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupOutletRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

/** Creates an outlet in DRAFT state and gives it one shared menu group. */
@Service
public class OutletService {
	private final OutletRepository outlets;
	private final RestaurantChainRepository restaurantChains;
	private final MenuGroupRepository menuGroups;
	private final MenuGroupOutletRepository menuGroupOutlets;
	private final OutletMenuInitializationAssembler menuInitializationAssembler;
	private final OutletMenuInitializedEventFactory initializedEventFactory;
	private final CatalogEventOutboxWriter outboxWriter;

	public OutletService(@Qualifier("outletPersistenceAdapter") OutletRepository outlets,
			@Qualifier("restaurantChainPersistenceAdapter") RestaurantChainRepository restaurantChains,
			@Qualifier("menuGroupPersistenceAdapter") MenuGroupRepository menuGroups,
			@Qualifier("menuGroupOutletPersistenceAdapter") MenuGroupOutletRepository menuGroupOutlets,
			OutletMenuInitializationAssembler menuInitializationAssembler,
			OutletMenuInitializedEventFactory initializedEventFactory, CatalogEventOutboxWriter outboxWriter) {
		this.outlets = outlets;
		this.restaurantChains = restaurantChains;
		this.menuGroups = menuGroups;
		this.menuGroupOutlets = menuGroupOutlets;
		this.menuInitializationAssembler = menuInitializationAssembler;
		this.initializedEventFactory = initializedEventFactory;
		this.outboxWriter = outboxWriter;
	}

	/**
	 * The assignment is saved with the outlet. If either write fails, the
	 * transaction rolls both back so a draft outlet never has an unknown menu.
	 */
	@Transactional
	public Outlet createDraftOutlet(Long restaurantChainId, CreateOutletCommand command) {
		if (restaurantChainId == null || restaurantChainId <= 0) {
			throw new IllegalArgumentException("Restaurant chain ID must be positive");
		}
		if (command == null) {
			throw new IllegalArgumentException("Create outlet command is required");
		}
		if (restaurantChains.findById(restaurantChainId).isEmpty()) {
			throw new RestaurantChainNotFoundException("Restaurant chain with ID: " + restaurantChainId + " not found");
		}
		if (command.menuGroupId() == null || command.menuGroupId() <= 0) {
			throw new IllegalArgumentException("Menu group ID must be positive");
		}
		menuGroups.findByIdAndRestaurantChainId(command.menuGroupId(), restaurantChainId)
				.orElseThrow(() -> new MenuGroupNotFoundException("Menu group with ID: " + command.menuGroupId()
						+ " not found in restaurant chain " + restaurantChainId));

		String outletCode = requireText(command.outletCode(), "Outlet code");
		if (outlets.existsByRestaurantChainIdAndOutletCode(restaurantChainId, outletCode)) {
			throw new DuplicateOutletCodeException(
					"Outlet code " + outletCode + " already exists in restaurant chain " + restaurantChainId);
		}

		Outlet savedOutlet = outlets
				.save(new Outlet(restaurantChainId, requireText(command.displayName(), "Display name"), outletCode,
						parseCurrency(command.currencyCode()), requireText(command.timezone(), "Timezone")));

		menuGroupOutlets.assignMenuGroupToOutlet(command.menuGroupId(), savedOutlet.getId());
		return savedOutlet;
	}

	/**
	 * Makes a prepared outlet customer-ready. A repeat request for an already
	 * active outlet is intentionally harmless and writes no duplicate event.
	 */
	@Transactional
	public Outlet activateOutlet(Long outletId, Long expectedVersion) {
		if (outletId == null || outletId <= 0) {
			throw new IllegalArgumentException("Outlet ID must be positive");
		}
		if (expectedVersion == null || expectedVersion < 0) {
			throw new IllegalArgumentException("Expected outlet version must be zero or greater");
		}

		Outlet outlet = outlets.findById(outletId)
				.orElseThrow(() -> new OutletNotFoundException("Outlet with ID: " + outletId + " not found"));

		// Retry safety: the earlier successful transaction already created the
		// complete initialization event, so do not emit another one.
		if (outlet.getStatus() == ChainStatus.ACTIVE) {
			return outlet;
		}
		if (outlet.getStatus() != ChainStatus.DRAFT) {
			throw new IllegalStateException("Only a draft outlet can be activated");
		}
		if (!outlet.getVersion().equals(expectedVersion)) {
			throw new OutletVersionConflictException("Outlet version is stale");
		}

		Long menuGroupId = menuGroupOutlets.findMenuGroupIdByOutletId(outletId).orElseThrow(
				() -> new IllegalStateException("Outlet must have an assigned menu group before activation"));
		MenuGroup menuGroup = menuGroups.findByIdAndRestaurantChainId(menuGroupId, outlet.getRestaurantChainId())
				.orElseThrow(
						() -> new IllegalStateException("Assigned menu group does not belong to the outlet's chain"));
		if (menuGroup.getStatus() != ChainStatus.ACTIVE) {
			throw new IllegalStateException("Assigned menu group must be ACTIVE before outlet activation");
		}

		// Build the complete body before exposing the outlet. If any source
		// data is invalid, the outlet stays DRAFT and no event is written.
		var menuItems = menuInitializationAssembler.assemble(outlet, menuGroup);

		outlet.changeStatusTo(ChainStatus.ACTIVE);
		Outlet activeOutlet = outlets.save(outlet);

		OutletMenuInitializedEvent event = initializedEventFactory.create(activeOutlet.getId(),
				activeOutlet.getRestaurantChainId(), menuGroup.getId(), activeOutlet.getVersion(),
				activeOutlet.getTimezone(), UUID.randomUUID().toString(), menuItems);
		outboxWriter.save(event);
		return activeOutlet;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " cannot be blank");
		}
		return value.trim();
	}

	private static Currency parseCurrency(String currencyCode) {
		try {
			return Currency.getInstance(requireText(currencyCode, "Currency code"));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Currency code must be a valid ISO 4217 currency", exception);
		}
	}
}
