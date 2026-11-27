package com.vishesh.foodcatalog.catalog.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleChild;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

/**
 * Reads normalized bundle relationship rows and turns them into the complete,
 * nested snapshot carried by a bundle-configuration event.
 */
@Component
public class BundleSnapshotAssembler {
	private static final int MAXIMUM_NESTING_HOPS = 3;
	private static final int MAXIMUM_RESOLVED_COMPONENTS = 250;

	private final MenuItemRepository menuItemRepository;
	private final BundleChildRepository bundleChildRepository;

	public BundleSnapshotAssembler(MenuItemRepository menuItemRepository, BundleChildRepository bundleChildRepository) {
		this.menuItemRepository = menuItemRepository;
		this.bundleChildRepository = bundleChildRepository;
	}

	/**
	 * Assembles one root bundle for one restaurant chain. The chain ID prevents a
	 * relationship from accidentally exposing an item from another chain.
	 */
	public EventBundleSnapshot assemble(Long bundleMenuItemId, Long restaurantChainId) {
		validatePositive(bundleMenuItemId, "Bundle menu item ID");
		validatePositive(restaurantChainId, "Restaurant chain ID");

		MenuItem rootBundle = loadItem(bundleMenuItemId, restaurantChainId, "Bundle");
		if (rootBundle.getMenuItemKind() != MenuItemKind.BUNDLE) {
			throw new IllegalArgumentException("Only a BUNDLE menu item can be assembled as a bundle snapshot");
		}

		Set<Long> rootPath = new HashSet<>();
		rootPath.add(rootBundle.getId());
		int[] resolvedComponents = {0};

		return new EventBundleSnapshot(rootBundle.getId(), rootBundle.getItemName(), rootBundle.getItemPrice(),
				loadChildren(rootBundle.getId(), restaurantChainId, 0, rootPath, resolvedComponents));
	}

	private List<EventBundleChild> loadChildren(Long parentBundleId, Long restaurantChainId, int parentHops,
			Set<Long> parentPath, int[] resolvedComponents) {

		return bundleChildRepository.findByBundleMenuItemId(parentBundleId).stream()
				.map(relationship -> toEventChild(relationship, restaurantChainId, parentHops, parentPath,
						resolvedComponents))
				.toList();
	}

	private EventBundleChild toEventChild(BundleChild relationship, Long restaurantChainId, int parentHops,
			Set<Long> parentPath, int[] resolvedComponents) {

		int childHops = parentHops + 1;
		if (childHops > MAXIMUM_NESTING_HOPS) {
			throw new IllegalStateException(
					"Stored bundle structure exceeds " + MAXIMUM_NESTING_HOPS + " nesting hops");
		}
		// Each child starts with its parent's path only. Sibling branches are
		// independent, so one sibling must not look like an ancestor of another.
		Set<Long> childPath = new HashSet<>(parentPath);
		if (!childPath.add(relationship.getChildMenuItemId())) {
			throw new IllegalStateException("Stored bundle structure contains a circular child relationship");
		}
		if (++resolvedComponents[0] > MAXIMUM_RESOLVED_COMPONENTS) {
			throw new IllegalStateException(
					"Stored bundle structure exceeds " + MAXIMUM_RESOLVED_COMPONENTS + " resolved components");
		}

		MenuItem child = loadItem(relationship.getChildMenuItemId(), restaurantChainId, "Bundle child");
		List<EventBundleChild> nestedChildren = child.getMenuItemKind() == MenuItemKind.BUNDLE
				? loadChildren(child.getId(), restaurantChainId, childHops, childPath, resolvedComponents)
				: List.of();

		return new EventBundleChild(child.getId(), child.getItemName(), child.getMenuItemKind(),
				relationship.getQuantity(), relationship.getDisplayOrder(), child.getStatus(), child.isVisibility(),
				child.isAvailability(), nestedChildren);
	}

	private MenuItem loadItem(Long menuItemId, Long restaurantChainId, String itemDescription) {
		return menuItemRepository.findByIdAndRestaurantChainId(menuItemId, restaurantChainId)
				.orElseThrow(() -> new MenuItemNotFoundException(itemDescription + " with ID " + menuItemId
						+ " was not found for restaurant chain " + restaurantChainId));
	}

	private static void validatePositive(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
	}
}
