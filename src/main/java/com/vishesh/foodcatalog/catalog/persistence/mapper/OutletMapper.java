package com.vishesh.foodcatalog.catalog.persistence.mapper;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.domain.Outlet;
import com.vishesh.foodcatalog.catalog.persistence.entity.OutletEntity;

/** Translates the business Outlet object to and from its PostgreSQL row. */
@Component
public class OutletMapper {

	public OutletEntity toEntity(Outlet outlet) {
		if (outlet == null) {
			throw new IllegalArgumentException("Outlet cannot be null");
		}

		return new OutletEntity(outlet.getId(), outlet.getRestaurantChainId(), outlet.getDisplayName(),
				outlet.getOutletCode(), outlet.getCurrency(), outlet.getStatus(), outlet.getCreatedAt(),
				outlet.getUpdatedAt(), outlet.getTimezone(), outlet.getVersion());
	}

	public Outlet toDomain(OutletEntity entity) {
		if (entity == null || entity.getId() == null) {
			throw new IllegalArgumentException("Stored outlet must have an ID");
		}

		return new Outlet(entity.getId(), entity.getRestaurantChainId(), entity.getDisplayName(),
				entity.getOutletCode(), entity.getCurrency(), entity.getStatus(), entity.getCreatedAt(),
				entity.getUpdatedAt(), entity.getTimezone(), entity.getVersion());
	}
}
