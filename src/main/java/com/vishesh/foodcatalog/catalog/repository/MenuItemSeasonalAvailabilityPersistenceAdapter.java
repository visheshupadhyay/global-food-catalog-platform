package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;

/** JPA adapter that replaces one scope atomically with its latest version. */
@Repository
public class MenuItemSeasonalAvailabilityPersistenceAdapter implements MenuItemSeasonalAvailabilityRepository {
	private final MenuItemSeasonalAvailabilityJpaRepository repository;
	public MenuItemSeasonalAvailabilityPersistenceAdapter(MenuItemSeasonalAvailabilityJpaRepository repository) {
		this.repository = repository;
	}
	@Override
	public MenuItemSeasonalAvailability save(MenuItemSeasonalAvailability season) {
		Optional<MenuItemSeasonalAvailabilityEntity> current = findEntity(season.getMenuItemId(), season.getOutletId());
		long nextVersion = current.map(value -> value.getSeasonVersion() + 1L).orElse(0L);
		if (season.getOutletId() == null)
			repository.deleteByMenuItemIdAndOutletIdIsNull(season.getMenuItemId());
		else
			repository.deleteByMenuItemIdAndOutletId(season.getMenuItemId(), season.getOutletId());
		// The database allows only one rule for this item/scope. Flush the queued
		// delete before inserting its replacement so the unique index sees no old row.
		repository.flush();
		if (season.isRestricted()) {
			repository.save(new MenuItemSeasonalAvailabilityEntity(season.getMenuItemId(), season.getOutletId(),
					season.getStartDate(), season.getEndDate(), nextVersion));
		}
		return new MenuItemSeasonalAvailability(season.getMenuItemId(), season.getOutletId(), season.getStartDate(),
				season.getEndDate(), nextVersion);
	}
	@Override
	public Optional<MenuItemSeasonalAvailability> find(Long menuItemId, Long outletId) {
		return findEntity(menuItemId, outletId).map(value -> new MenuItemSeasonalAvailability(value.getMenuItemId(),
				value.getOutletId(), value.getStartDate(), value.getEndDate(), value.getSeasonVersion()));
	}
	private Optional<MenuItemSeasonalAvailabilityEntity> findEntity(Long menuItemId, Long outletId) {
		return outletId == null
				? repository.findByMenuItemIdAndOutletIdIsNull(menuItemId)
				: repository.findByMenuItemIdAndOutletId(menuItemId, outletId);
	}
}
