package com.vishesh.foodcatalog.catalog.repository;

import java.time.LocalTime;
import com.vishesh.foodcatalog.catalog.domain.Weekday;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** One database row representing a single recurring schedule window. */
@Entity
@Table(name = "menu_item_schedule")
public class MenuItemScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "outlet_id")
    private Long outletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private Weekday weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "schedule_version", nullable = false)
    private Long scheduleVersion;

    protected MenuItemScheduleEntity() {
    }

    public MenuItemScheduleEntity(Long item, Long outlet, Weekday day, LocalTime start, LocalTime end) {
        this(item, outlet, day, start, end, 0L);
    }

    public MenuItemScheduleEntity(Long item, Long outlet, Weekday day, LocalTime start, LocalTime end, Long version) {
        menuItemId = item;
        outletId = outlet;
        weekday = day;
        startTime = start;
        endTime = end;
        this.version = 0L;
        this.scheduleVersion = version;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public Weekday getWeekday() {
        return weekday;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Long getVersion() {
        return scheduleVersion;
    }
}
