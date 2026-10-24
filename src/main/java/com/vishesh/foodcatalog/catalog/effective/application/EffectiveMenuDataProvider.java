package com.vishesh.foodcatalog.catalog.effective.application;

import java.util.Optional;

import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;

public interface EffectiveMenuDataProvider {
    Optional<EffectiveMenuSourceData> findByOutletId(Long outletId);
}
