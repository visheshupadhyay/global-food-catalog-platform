package com.vishesh.foodcatalog.catalog.effective.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuDataProvider;
import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletLocalMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.BaseMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletLocalMenuItemProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletOverrideProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.projection.OutletStatusProjection;
import com.vishesh.foodcatalog.catalog.effective.persistence.repository.EffectiveMenuJpaReadRepository;

@Component("postgresEffectiveMenuDataProvider")
/** Reads only the small set of rows needed to calculate one outlet's menu. */
public class PostgresEffectiveMenuDataProvider implements EffectiveMenuDataProvider {
	private final EffectiveMenuJpaReadRepository effectiveMenuJpaReadRepository;

	public PostgresEffectiveMenuDataProvider(EffectiveMenuJpaReadRepository effectiveMenuJpaReadRepository) {
		this.effectiveMenuJpaReadRepository = effectiveMenuJpaReadRepository;
	}

	@Override
	public Optional<EffectiveMenuSourceData> findByOutletId(Long outletId) {
		if (outletId == null || outletId <= 0) {
			throw new IllegalArgumentException("Outlet not found");
		}

		Optional<OutletStatusProjection> outletStatusProjection = effectiveMenuJpaReadRepository
				.findOutletStatusByOutletId(outletId);

		if (outletStatusProjection.isEmpty()) {
			// No outlet row means the outlet does not exist.
			return Optional.empty();
		}

		List<BaseMenuItemProjection> baseMenuItemProjections = effectiveMenuJpaReadRepository
				.findBaseItemsByOutletId(outletId);

		List<OutletOverrideProjection> outletOverrideProjections = effectiveMenuJpaReadRepository
				.findOverridesByOutletId(outletId);

		List<OutletLocalMenuItemProjection> outletLocalMenuItemProjections = effectiveMenuJpaReadRepository
				.findLocalItemsByOutletId(outletId);

		List<OutletLocalMenuItemSnapshot> localItems = outletLocalMenuItemProjections.stream()
				.map(this::toLocalItemSnapshot).toList();
		List<BaseMenuItemSnapshot> baseItems = baseMenuItemProjections.stream().map(this::toBaseItemSnapshot).toList();
		List<OutletOverrideSnapshot> overrideItems = outletOverrideProjections.stream()
				.map(projection -> toOverrideSnapshot(outletId, projection)).toList();

		return Optional.of(new EffectiveMenuSourceData(outletId,
				ChainStatus.valueOf(outletStatusProjection.get().getOutletStatus()), baseItems, overrideItems,
				localItems));
	}

	private BaseMenuItemSnapshot toBaseItemSnapshot(BaseMenuItemProjection projection) {
		return new BaseMenuItemSnapshot(projection.getMenuItemId(), projection.getItemName(), projection.getPrice(),
				projection.getAvailable(), projection.getVisible(), MenuItemStatus.valueOf(projection.getStatus()));
	}

	private OutletLocalMenuItemSnapshot toLocalItemSnapshot(OutletLocalMenuItemProjection projection) {
		return new OutletLocalMenuItemSnapshot(projection.getMenuItemId(), projection.getOutletId(),
				projection.getItemName(), projection.getPrice(), projection.getAvailable(), projection.getVisible(),
				MenuItemStatus.valueOf(projection.getStatus()));
	}

	private OutletOverrideSnapshot toOverrideSnapshot(Long outletId, OutletOverrideProjection projection) {
		return new OutletOverrideSnapshot(projection.getMenuItemId(), outletId, projection.getPriceOverride(),
				projection.getAvailableOverride(), projection.getVisibleOverride());
	}
}
