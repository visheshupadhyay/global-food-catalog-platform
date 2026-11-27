package com.vishesh.foodcatalog.catalog.application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;

/**
 * Finds every bundle snapshot that needs rebuilding after one bundle changes.
 *
 * For example, changing Burger Meal must also refresh Lunch Combo when Lunch
 * Combo contains Burger Meal. The changed bundle itself is always part of the
 * result because it can be sold independently.
 * </p>
 */
@Component
public class BundleRootResolver {
	private final BundleChildRepository bundleChildRepository;

	public BundleRootResolver(BundleChildRepository bundleChildRepository) {
		this.bundleChildRepository = bundleChildRepository;
	}

	/**
	 * Returns the changed bundle plus every ancestor bundle, once each, in a stable
	 * order. The visited set also prevents corrupt circular data from causing an
	 * endless lookup.
	 */
	public List<Long> findAffectedBundleIds(Long changedBundleId) {
		if (changedBundleId == null || changedBundleId <= 0) {
			throw new IllegalArgumentException("Changed bundle ID must be greater than zero");
		}

		Set<Long> discoveredIds = new HashSet<>();
		ArrayDeque<Long> bundlesToInspect = new ArrayDeque<>();
		discoveredIds.add(changedBundleId);
		bundlesToInspect.add(changedBundleId);

		while (!bundlesToInspect.isEmpty()) {
			Long childId = bundlesToInspect.removeFirst();
			List<Long> parentIds = bundleChildRepository.findParentBundleIdsByChildMenuItemId(childId);
			if (parentIds == null) {
				continue;
			}
			for (Long parentId : parentIds) {
				if (parentId == null || parentId <= 0) {
					throw new IllegalStateException("Stored bundle relationship contains an invalid parent ID");
				}
				if (discoveredIds.add(parentId)) {
					bundlesToInspect.addLast(parentId);
				}
			}
		}

		// Keep the directly changed bundle first; sort ancestor IDs only so
		// event creation stays deterministic without hiding the primary item.
		List<Long> affectedBundleIds = new ArrayList<>();
		affectedBundleIds.add(changedBundleId);
		discoveredIds.stream().filter(bundleId -> !changedBundleId.equals(bundleId)).sorted()
				.forEach(affectedBundleIds::add);
		return List.copyOf(affectedBundleIds);
	}

	/**
	 * Finds only the parent bundles of a changed item. This is used when a normal
	 * child such as Fries changes: Fries receives its own ordinary event, while
	 * each parent bundle needs a separate refreshed snapshot.
	 */
	public List<Long> findAncestorBundleIds(Long changedMenuItemId) {
		return findAffectedBundleIds(changedMenuItemId).stream().filter(bundleId -> !changedMenuItemId.equals(bundleId))
				.toList();
	}
}
