package com.vishesh.foodcatalog.catalog.repository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;
import com.vishesh.foodcatalog.catalog.domain.ScheduleWindow;
import com.vishesh.foodcatalog.catalog.domain.Weekday;

/**
 * Converts the schedule aggregate to rows while keeping callers independent of
 * JPA.
 */
@Repository
public class MenuItemSchedulePersistenceAdapter implements MenuItemScheduleRepository {
    private final MenuItemScheduleJpaRepository repository;

    public MenuItemSchedulePersistenceAdapter(MenuItemScheduleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public MenuItemSchedule save(MenuItemSchedule schedule) {
        Long currentVersion = repository.findMaxVersion(schedule.getMenuItemId(), schedule.getOutletId());
        Long nextVersion = (currentVersion == null ? -1L : currentVersion) + 1L;
        if (schedule.getOutletId() == null) {
            repository.deleteByMenuItemIdAndOutletIdIsNull(schedule.getMenuItemId());
        } else {
            repository.deleteByMenuItemIdAndOutletId(schedule.getMenuItemId(), schedule.getOutletId());
        }
        List<MenuItemScheduleEntity> rows = new ArrayList<>();

        for (var dayEntry : schedule.getWindowsByDay().entrySet()) {
            Weekday weekday = dayEntry.getKey();
            List<ScheduleWindow> dayWindows = dayEntry.getValue();

            for (ScheduleWindow window : dayWindows) {
                MenuItemScheduleEntity row = new MenuItemScheduleEntity(
                        schedule.getMenuItemId(), schedule.getOutletId(), weekday,
                        window.getStartTime(), window.getEndTime(), nextVersion);
                rows.add(row);
            }
        }
        repository.saveAll(rows);
        return new MenuItemSchedule(schedule.getMenuItemId(), schedule.getOutletId(),
                schedule.getWindowsByDay(), nextVersion);
    }

    @Override
    public Optional<MenuItemSchedule> find(Long menuItemId, Long outletId) {
        List<MenuItemScheduleEntity> rows = outletId == null
                ? repository.findByMenuItemIdAndOutletIdIsNull(menuItemId)
                : repository.findByMenuItemIdAndOutletId(menuItemId, outletId);
        if (rows.isEmpty())
            return Optional.empty();
        EnumMap<Weekday, List<ScheduleWindow>> windows = new EnumMap<>(Weekday.class);
        Long version = rows.stream().map(MenuItemScheduleEntity::getVersion).max(Long::compareTo).orElse(0L);
        for (MenuItemScheduleEntity row : rows) {
            Weekday weekday = row.getWeekday();
            List<ScheduleWindow> dayWindows = windows.computeIfAbsent(
                    weekday,
                    ignored -> new ArrayList<>());

            ScheduleWindow window = new ScheduleWindow(
                    row.getStartTime(), row.getEndTime());
            dayWindows.add(window);
        }
        return Optional.of(new MenuItemSchedule(menuItemId, outletId, windows, version));
    }
}
