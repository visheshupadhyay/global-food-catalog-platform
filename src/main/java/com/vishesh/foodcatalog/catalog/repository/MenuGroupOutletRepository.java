package com.vishesh.foodcatalog.catalog.repository;

import java.util.List;


public interface MenuGroupOutletRepository {
    List<Long> findOutletIdsByMenuGroupId(Long menuGroupId);
}
