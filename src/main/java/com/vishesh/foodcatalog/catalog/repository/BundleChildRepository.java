package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;

/** Port for replacing and reading the ordered children of one bundle. */
public interface BundleChildRepository {
	List<BundleChild> replaceForBundle(Long bundleMenuItemId, List<BundleChild> children);

	List<BundleChild> findByBundleMenuItemId(Long bundleMenuItemId);

	List<Long> findParentBundleIdsByChildMenuItemId(Long childMenuItemId);
}
