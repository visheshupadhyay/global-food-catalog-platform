package com.vishesh.foodcatalog.catalog.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Validates and replaces the ordered children that make up one bundle.
 * A bundle and its child relationships are changed in one transaction.
 */
@Service
public class BundleCompositionService {
    private static final int MAXIMUM_DIRECT_CHILDREN = 25;

    private final MenuItemRepository menuItemRepository;
    private final RestaurantChainRepository restaurantChainRepository;
    private final BundleChildRepository bundleChildRepository;
    private final BundleGraphValidator bundleGraphValidator;
    private final BundleRootResolver bundleRootResolver;
    private final BundleSnapshotAssembler bundleSnapshotAssembler;
    private final MenuItemChangedEventFactory menuItemChangedEventFactory;
    private final OutboxEventFactory outboxEventFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BundleCompositionService(
            MenuItemRepository menuItemRepository,
            RestaurantChainRepository restaurantChainRepository,
            BundleChildRepository bundleChildRepository,
            BundleGraphValidator bundleGraphValidator,
            BundleRootResolver bundleRootResolver,
            BundleSnapshotAssembler bundleSnapshotAssembler,
            MenuItemChangedEventFactory menuItemChangedEventFactory,
            OutboxEventFactory outboxEventFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {

        this.menuItemRepository = menuItemRepository;
        this.restaurantChainRepository = restaurantChainRepository;
        this.bundleChildRepository = bundleChildRepository;
        this.bundleGraphValidator = bundleGraphValidator;
        this.bundleRootResolver = bundleRootResolver;
        this.bundleSnapshotAssembler = bundleSnapshotAssembler;
        this.menuItemChangedEventFactory = menuItemChangedEventFactory;
        this.outboxEventFactory = outboxEventFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Replaces every direct child of one bundle after checking ownership and
     * optimistic-locking and nested-structure rules.
     */
    @Transactional
    public List<BundleChild> replaceChildren(
            Long bundleMenuItemId,
            Long restaurantChainId,
            Long expectedVersion,
            List<BundleChild> children) {

        return replaceChildrenWithResult(bundleMenuItemId, restaurantChainId,
                expectedVersion, children).children();
    }

    /**
     * Returns the current complete direct-child list and its version. Admin
     * clients read this before preparing their next replacement request.
     */
    @Transactional(readOnly = true)
    public BundleCompositionResult getComposition(
            Long bundleMenuItemId,
            Long restaurantChainId) {

        validatePositiveIds(bundleMenuItemId, restaurantChainId);
        if (restaurantChainRepository.findById(restaurantChainId).isEmpty()) {
            throw new RestaurantChainNotFoundException(
                    "Restaurant chain with ID " + restaurantChainId + " was not found");
        }

        MenuItem bundle = loadBundle(bundleMenuItemId, restaurantChainId);
        return new BundleCompositionResult(
                bundle.getId(), bundle.getVersion(),
                bundleChildRepository.findByBundleMenuItemId(bundle.getId()));
    }

    /**
     * Replaces every direct child and returns the bundle's new version for the
     * HTTP API. Relationship writes and all outbox rows are one transaction.
     */
    @Transactional
    public BundleCompositionResult replaceChildrenWithResult(
            Long bundleMenuItemId,
            Long restaurantChainId,
            Long expectedVersion,
            List<BundleChild> children) {

        validateRequest(bundleMenuItemId, restaurantChainId, expectedVersion, children);

        if (restaurantChainRepository.findById(restaurantChainId).isEmpty()) {
            throw new RestaurantChainNotFoundException(
                    "Restaurant chain with ID " + restaurantChainId + " was not found");
        }

        MenuItem bundle = menuItemRepository
                .findByIdAndRestaurantChainId(bundleMenuItemId, restaurantChainId)
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item with ID " + bundleMenuItemId
                                + " was not found for restaurant chain " + restaurantChainId));

        if (bundle.getMenuItemKind() != MenuItemKind.BUNDLE) {
            throw new IllegalArgumentException("Only a BUNDLE menu item can contain bundle children");
        }
        if (!bundle.getVersion().equals(expectedVersion)) {
            throw new MenuItemVersionConflictException(
                    "Bundle version is stale; expected " + bundle.getVersion()
                            + " but received " + expectedVersion);
        }

        validateChildList(bundleMenuItemId, restaurantChainId, bundle, children);
        bundleGraphValidator.validateReplacement(bundleMenuItemId, children);

        // A changed nested bundle also changes every parent bundle that embeds
        // it. Work out the complete delivery plan before mutating any rows, so
        // an undispatchable change cannot leave Catalog and Effective Menu out
        // of sync.
        List<Long> affectedBundleIds = bundleRootResolver.findAffectedBundleIds(bundleMenuItemId);
        Map<Long, List<Long>> affectedOutletIdsByBundle = findAffectedOutlets(affectedBundleIds);

        // Touching the directly changed bundle makes JPA advance its version.
        bundle.markStructureChanged();
        MenuItem savedBundle = menuItemRepository.save(bundle);
        Map<Long, MenuItem> savedBundlesById = new LinkedHashMap<>();
        savedBundlesById.put(savedBundle.getId(), savedBundle);

        List<BundleChild> savedChildren = bundleChildRepository.replaceForBundle(savedBundle.getId(), children);

        // Parent bundles are independent effective-menu aggregates. Advance
        // each parent once so its event is newer than its previous snapshot.
        for (Long affectedBundleId : affectedBundleIds) {
            if (savedBundlesById.containsKey(affectedBundleId)) {
                continue;
            }
            MenuItem affectedBundle = loadBundle(affectedBundleId, restaurantChainId);
            affectedBundle.markStructureChanged();
            savedBundlesById.put(affectedBundleId, menuItemRepository.save(affectedBundle));
        }

        publishBundleConfigurationChanges(savedBundlesById, affectedOutletIdsByBundle, restaurantChainId);
        return new BundleCompositionResult(
                savedBundle.getId(), savedBundle.getVersion(), savedChildren);
    }

    private Map<Long, List<Long>> findAffectedOutlets(List<Long> affectedBundleIds) {
        Map<Long, List<Long>> affectedOutletIdsByBundle = new HashMap<>();
        for (Long affectedBundleId : affectedBundleIds) {
            List<Long> affectedOutletIds = menuItemRepository.findAffectedOutletIds(affectedBundleId);
            if (affectedOutletIds == null || affectedOutletIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "No affected outlets were found for bundle " + affectedBundleId);
            }
            affectedOutletIdsByBundle.put(affectedBundleId, List.copyOf(affectedOutletIds));
        }
        return affectedOutletIdsByBundle;
    }

    private MenuItem loadBundle(Long bundleMenuItemId, Long restaurantChainId) {
        MenuItem bundle = menuItemRepository
                .findByIdAndRestaurantChainId(bundleMenuItemId, restaurantChainId)
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Bundle menu item with ID " + bundleMenuItemId
                                + " was not found for restaurant chain " + restaurantChainId));
        if (bundle.getMenuItemKind() != MenuItemKind.BUNDLE) {
            throw new IllegalStateException("Affected menu item " + bundleMenuItemId + " is not a bundle");
        }
        return bundle;
    }

    private void publishBundleConfigurationChanges(
            Map<Long, MenuItem> savedBundlesById,
            Map<Long, List<Long>> affectedOutletIdsByBundle,
            Long restaurantChainId) {

        // All events caused by one admin request share a correlation ID. It
        // lets an operator trace one Burger Meal change through every parent
        // combo and every outlet's outbox row.
        String correlationId = UUID.randomUUID().toString();
        try {
            for (Map.Entry<Long, MenuItem> entry : savedBundlesById.entrySet()) {
                MenuItem savedBundle = entry.getValue();
                EventBundleSnapshot bundleSnapshot = bundleSnapshotAssembler
                        .assemble(savedBundle.getId(), restaurantChainId);
                for (Long outletId : affectedOutletIdsByBundle.get(entry.getKey())) {
                    MenuItemChangedEvent event = menuItemChangedEventFactory
                            .createBundleConfigurationChanged(
                                    savedBundle.getId(),
                                    restaurantChainId,
                                    savedBundle.getVersion(),
                                    outletId,
                                    correlationId,
                                    savedBundle.getStatus(),
                                    savedBundle.getItemName(),
                                    bundleSnapshot);
                    String payload = objectMapper.writeValueAsString(event);
                    outboxEventRepository.save(outboxEventFactory.create(event, payload));
                }
            }
        } catch (JacksonException exception) {
            // Let the unchecked exception escape the @Transactional method so
            // Spring rolls back children, versions, and every outbox row.
            throw new IllegalStateException("Could not serialize bundle configuration event", exception);
        }
    }

    private void validateRequest(
            Long bundleMenuItemId,
            Long restaurantChainId,
            Long expectedVersion,
            List<BundleChild> children) {

        validatePositiveIds(bundleMenuItemId, restaurantChainId);
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version must be zero or greater");
        }
        if (children == null) {
            throw new IllegalArgumentException("Bundle children are required");
        }
        if (children.isEmpty()) {
            throw new IllegalArgumentException("A bundle must contain at least one child");
        }
        if (children.size() > MAXIMUM_DIRECT_CHILDREN) {
            throw new IllegalArgumentException(
                    "A bundle cannot contain more than " + MAXIMUM_DIRECT_CHILDREN + " direct children");
        }
    }

    private void validatePositiveIds(Long bundleMenuItemId, Long restaurantChainId) {
        if (bundleMenuItemId == null || bundleMenuItemId <= 0) {
            throw new IllegalArgumentException("Bundle menu item ID must be greater than zero");
        }
        if (restaurantChainId == null || restaurantChainId <= 0) {
            throw new IllegalArgumentException("Restaurant chain ID must be greater than zero");
        }
    }

    private void validateChildList(
            Long bundleMenuItemId,
            Long restaurantChainId,
            MenuItem bundle,
            List<BundleChild> children) {

        Set<Long> childMenuItemIds = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();

        for (BundleChild childRelationship : children) {
            if (childRelationship == null) {
                throw new IllegalArgumentException("Bundle children cannot contain null values");
            }
            if (!bundleMenuItemId.equals(childRelationship.getBundleMenuItemId())) {
                throw new IllegalArgumentException("Every child must belong to the bundle being replaced");
            }
            if (!childMenuItemIds.add(childRelationship.getChildMenuItemId())) {
                throw new IllegalArgumentException("A bundle cannot contain the same child more than once");
            }
            if (!displayOrders.add(childRelationship.getDisplayOrder())) {
                throw new IllegalArgumentException("Each bundle child must have a unique display order");
            }

            MenuItem child = menuItemRepository
                    .findByIdAndRestaurantChainId(childRelationship.getChildMenuItemId(), restaurantChainId)
                    .orElseThrow(() -> new MenuItemNotFoundException(
                            "Child menu item with ID " + childRelationship.getChildMenuItemId()
                                    + " was not found for restaurant chain " + restaurantChainId));

            validateOwnership(bundle, child);
        }
    }

    private void validateOwnership(MenuItem bundle, MenuItem child) {
        boolean bundleIsShared = bundle.getOutletId() == null;
        boolean childIsShared = child.getOutletId() == null;

        if (bundleIsShared && !childIsShared) {
            throw new IllegalArgumentException("A shared bundle cannot contain an outlet-local child");
        }
        if (!bundleIsShared && !childIsShared && !bundle.getOutletId().equals(child.getOutletId())) {
            throw new IllegalArgumentException("An outlet-local bundle can contain only shared children "
                    + "or children from the same outlet");
        }
    }
}
