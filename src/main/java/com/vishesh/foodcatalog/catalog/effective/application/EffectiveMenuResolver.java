package com.vishesh.foodcatalog.catalog.effective.application;

import com.vishesh.foodcatalog.catalog.effective.domain.BaseMenuItemSnapshot;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;
import com.vishesh.foodcatalog.catalog.effective.domain.OutletOverrideSnapshot;

public interface EffectiveMenuResolver {
    EffectiveMenuItem resolve(BaseMenuItemSnapshot baseItem, Long outletId, OutletOverrideSnapshot override);
}
