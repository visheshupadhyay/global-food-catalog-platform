package com.vishesh.foodcatalog.catalog.repository;

import java.util.Optional;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;

/** Application-facing port for reading and replacing one schedule aggregate. */
public interface MenuItemScheduleRepository {
    MenuItemSchedule save(MenuItemSchedule schedule);
    Optional<MenuItemSchedule> find(Long menuItemId, Long outletId);
}
