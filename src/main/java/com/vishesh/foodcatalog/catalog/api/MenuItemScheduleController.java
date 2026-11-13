package com.vishesh.foodcatalog.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.vishesh.foodcatalog.catalog.application.MenuItemScheduleService;

/** HTTP API for replacing and reading recurring menu-item schedules. */
@RestController
@RequestMapping("/api/v1/menu-items/{menuItemId}/schedules")
public class MenuItemScheduleController {
    private final MenuItemScheduleService service;

    public MenuItemScheduleController(MenuItemScheduleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(@PathVariable Long menuItemId, @RequestBody CreateScheduleRequest request) {
        return service.save(menuItemId, request);
    }

    @PutMapping("/{scheduleId}")
    public Object update(@PathVariable Long menuItemId, @PathVariable Long scheduleId,
            @RequestBody UpdateScheduleRequest request) {
        if (request == null || request.outletId() == null || !scheduleId.equals(request.outletId()))
            throw new IllegalArgumentException("Schedule ID must match outlet ID");
        return service.update(menuItemId, request);
    }

    /** Updates the one base schedule for a menu item; base schedules have no outlet ID. */
    @PutMapping("/base")
    public Object updateBase(@PathVariable Long menuItemId, @RequestBody UpdateScheduleRequest request) {
        if (request == null || request.outletId() != null) {
            throw new IllegalArgumentException("Base schedules cannot have an outlet ID");
        }
        return service.update(menuItemId, request);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long menuItemId, @PathVariable Long scheduleId) {
        // Deletion is represented by replacing the scoped schedule with no windows.
        service.save(menuItemId, new CreateScheduleRequest(scheduleId, java.util.Map.of()));
    }

    @DeleteMapping("/base")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBase(@PathVariable Long menuItemId) {
        service.delete(menuItemId, null);
    }

    @GetMapping
    public Object get(@PathVariable Long menuItemId, @RequestParam(required = false) Long outletId) {
        return service.find(menuItemId, outletId).orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
    }
}
