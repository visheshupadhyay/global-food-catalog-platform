package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;
import java.util.Optional;


public interface MenuGroupOutletRepository {
    List<Long> findOutletIdsByMenuGroupId(Long menuGroupId);

    Optional<Long> findMenuGroupIdByOutletId(Long outletId);

    /** Assigns the one shared menu group that an outlet will inherit. */
    void assignMenuGroupToOutlet(Long menuGroupId, Long outletId);
}
