package com.vishesh.foodcatalog.catalog.application;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Republishes every parent bundle after one of its nested children changes.
 *
 * <p>The child itself is already updated by its owning service. This class
 * only refreshes the denormalized bundle snapshots that contain that child,
 * so Effective Menu never has to query Catalog to rebuild a meal or combo.</p>
 */
@Component
public class BundleRefreshPublisher {
    private final BundleRootResolver bundleRootResolver;
    private final BundleSnapshotAssembler bundleSnapshotAssembler;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemChangedEventFactory menuItemChangedEventFactory;
    private final OutboxEventFactory outboxEventFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BundleRefreshPublisher(
            BundleRootResolver bundleRootResolver,
            BundleSnapshotAssembler bundleSnapshotAssembler,
            MenuItemRepository menuItemRepository,
            MenuItemChangedEventFactory menuItemChangedEventFactory,
            OutboxEventFactory outboxEventFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.bundleRootResolver = bundleRootResolver;
        this.bundleSnapshotAssembler = bundleSnapshotAssembler;
        this.menuItemRepository = menuItemRepository;
        this.menuItemChangedEventFactory = menuItemChangedEventFactory;
        this.outboxEventFactory = outboxEventFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Advances each affected parent bundle once and writes a full bundle event
     * for every outlet that can sell it. The caller must already be inside the
     * transaction that changed the child.
     */
    public void publishAncestorBundleRefreshes(
            Long changedMenuItemId,
            Long restaurantChainId,
            String correlationId) {

        publishAncestorBundleRefreshes(changedMenuItemId, restaurantChainId, correlationId, null);
    }

    /**
     * @param outletScope null means the child change affects every outlet.
     *        A non-null scope is used by an outlet-local schedule/seasonal
     *        override so it cannot invalidate another outlet's bundle menu.
     */
    public void publishAncestorBundleRefreshes(
            Long changedMenuItemId,
            Long restaurantChainId,
            String correlationId,
            List<Long> outletScope) {

        for (Long bundleId : bundleRootResolver.findAncestorBundleIds(changedMenuItemId)) {
            MenuItem bundle = menuItemRepository.findByIdAndRestaurantChainId(bundleId, restaurantChainId)
                    .orElseThrow(() -> new MenuItemNotFoundException(
                            "Affected bundle with ID " + bundleId + " was not found for restaurant chain "
                                    + restaurantChainId));
            if (bundle.getMenuItemKind() != MenuItemKind.BUNDLE) {
                throw new IllegalStateException("Affected menu item " + bundleId + " is not a bundle");
            }

            List<Long> outletIds = resolveTargetOutlets(bundle.getId(), outletScope);
            // An outlet-local rule can affect a shared child in Outlet A even
            // though this particular parent bundle is sold only in Outlet B.
            // No customer-facing snapshot changes in that case, so do not
            // create a meaningless version bump or outbox event.
            if (outletIds.isEmpty()) {
                continue;
            }

            // The bundle's own price and composition did not change. Its
            // version changes because its resolved child snapshot did.
            bundle.markStructureChanged();
            MenuItem savedBundle = menuItemRepository.save(bundle);
            publishBundleSnapshot(savedBundle, restaurantChainId, correlationId, outletIds);
        }
    }

    private List<Long> resolveTargetOutlets(Long bundleId, List<Long> outletScope) {
        List<Long> outletIds = menuItemRepository.findAffectedOutletIds(bundleId);
        if (outletIds == null || outletIds.isEmpty()) {
            throw new IllegalArgumentException("No affected outlets were found for bundle " + bundleId);
        }
        if (outletScope == null) {
            return outletIds;
        }
        Set<Long> allowedOutletIds = Set.copyOf(outletScope);
        return outletIds.stream().filter(allowedOutletIds::contains).toList();
    }

    private void publishBundleSnapshot(
            MenuItem bundle,
            Long restaurantChainId,
            String correlationId,
            List<Long> outletIds) {

        EventBundleSnapshot snapshot = bundleSnapshotAssembler.assemble(bundle.getId(), restaurantChainId);
        try {
            for (Long outletId : outletIds) {
                MenuItemChangedEvent event = menuItemChangedEventFactory.createBundleConfigurationChanged(
                        bundle.getId(), restaurantChainId, bundle.getVersion(), outletId,
                        correlationId, bundle.getStatus(), bundle.getItemName(), snapshot);
                outboxEventRepository.save(outboxEventFactory.create(event, objectMapper.writeValueAsString(event)));
            }
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize refreshed bundle event", exception);
        }
    }
}
