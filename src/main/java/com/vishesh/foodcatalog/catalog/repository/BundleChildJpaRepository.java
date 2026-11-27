package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BundleChildJpaRepository extends JpaRepository<BundleChildEntity, Long> {
	List<BundleChildEntity> findByBundleMenuItemIdOrderByDisplayOrder(Long bundleMenuItemId);

	List<BundleChildEntity> findByChildMenuItemId(Long childMenuItemId);

	void deleteByBundleMenuItemId(Long bundleMenuItemId);
}
