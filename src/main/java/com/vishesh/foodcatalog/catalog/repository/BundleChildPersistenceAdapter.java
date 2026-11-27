package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;

/** JPA adapter that replaces a bundle's whole ordered child list atomically. */
@Repository
public class BundleChildPersistenceAdapter implements BundleChildRepository {
	private final BundleChildJpaRepository repository;

	public BundleChildPersistenceAdapter(BundleChildJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public List<BundleChild> replaceForBundle(Long bundleMenuItemId, List<BundleChild> children) {
		if (bundleMenuItemId == null || bundleMenuItemId <= 0) {
			throw new IllegalArgumentException("Bundle menu item ID must be greater than zero");
		}
		if (children == null) {
			throw new IllegalArgumentException("Bundle children are required");
		}
		if (children.stream().anyMatch(child -> !bundleMenuItemId.equals(child.getBundleMenuItemId()))) {
			throw new IllegalArgumentException("Every child must belong to the bundle being replaced");
		}

		repository.deleteByBundleMenuItemId(bundleMenuItemId);
		repository.flush();
		List<BundleChildEntity> saved = repository.saveAll(children.stream().map(this::toEntity).toList());
		return saved.stream().map(this::toDomain).toList();
	}

	@Override
	public List<BundleChild> findByBundleMenuItemId(Long bundleMenuItemId) {
		return repository.findByBundleMenuItemIdOrderByDisplayOrder(bundleMenuItemId).stream().map(this::toDomain)
				.toList();
	}

	@Override
	public List<Long> findParentBundleIdsByChildMenuItemId(Long childMenuItemId) {
		return repository.findByChildMenuItemId(childMenuItemId).stream().map(BundleChildEntity::getBundleMenuItemId)
				.distinct().toList();
	}

	private BundleChild toDomain(BundleChildEntity entity) {
		return new BundleChild(entity.getId(), entity.getBundleMenuItemId(), entity.getChildMenuItemId(),
				entity.getQuantity(), entity.getDisplayOrder());
	}

	private BundleChildEntity toEntity(BundleChild child) {
		return new BundleChildEntity(child.getBundleMenuItemId(), child.getChildMenuItemId(), child.getQuantity(),
				child.getDisplayOrder());
	}
}
