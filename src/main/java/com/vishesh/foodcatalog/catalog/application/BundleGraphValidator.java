package com.vishesh.foodcatalog.catalog.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;

/**
 * Protects bundle composition from cycles, excessive nesting, and excessively
 * large resolved structures before a replacement is persisted.
 */
@Component
public class BundleGraphValidator {
	private static final int MAXIMUM_NESTING_HOPS = 3;
	private static final int MAXIMUM_RESOLVED_COMPONENTS = 250;

	private final BundleChildRepository bundleChildRepository;

	public BundleGraphValidator(BundleChildRepository bundleChildRepository) {
		this.bundleChildRepository = bundleChildRepository;
	}

	/**
	 * Validates the proposed direct children of {@code rootBundleId}. The root
	 * itself starts at zero hops, so its direct children are at one hop.
	 */
	public void validateReplacement(Long rootBundleId, List<BundleChild> replacementChildren) {
		if (rootBundleId == null || rootBundleId <= 0) {
			throw new IllegalArgumentException("Root bundle ID must be greater than zero");
		}
		if (replacementChildren == null) {
			throw new IllegalArgumentException("Replacement bundle children are required");
		}

		int[] resolvedComponents = {0};
		Set<Long> rootPath = new HashSet<>();
		rootPath.add(rootBundleId);

		for (BundleChild child : replacementChildren) {
			if (child == null) {
				throw new IllegalArgumentException("Replacement bundle children cannot contain null values");
			}
			visit(child.getChildMenuItemId(), 1, new HashSet<>(rootPath), resolvedComponents);
		}
	}

	private void visit(Long currentMenuItemId, int currentHops, Set<Long> currentPath, int[] resolvedComponents) {

		if (!currentPath.add(currentMenuItemId)) {
			throw new IllegalArgumentException("A bundle cannot contain a circular child relationship");
		}
		if (currentHops > MAXIMUM_NESTING_HOPS) {
			throw new IllegalArgumentException("A bundle cannot exceed " + MAXIMUM_NESTING_HOPS + " nesting hops");
		}
		if (++resolvedComponents[0] > MAXIMUM_RESOLVED_COMPONENTS) {
			throw new IllegalArgumentException(
					"A bundle cannot resolve more than " + MAXIMUM_RESOLVED_COMPONENTS + " components");
		}

		List<BundleChild> nestedChildren = bundleChildRepository.findByBundleMenuItemId(currentMenuItemId);
		for (BundleChild nestedChild : nestedChildren) {
			visit(nestedChild.getChildMenuItemId(), currentHops + 1, new HashSet<>(currentPath), resolvedComponents);
		}
	}
}
