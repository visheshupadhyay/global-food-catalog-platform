package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.api.CreateScheduleRequest;
import com.vishesh.foodcatalog.catalog.api.UpdateScheduleRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.Weekday;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemScheduleRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MenuItemScheduleServiceTest {
    @Mock MenuItemRepository menuItems;
    @Mock OutletRepository outlets;
    @Mock MenuItemScheduleRepository schedules;
    @Mock MenuItemChangedEventFactory eventFactory;
    @Mock OutboxEventFactory outboxFactory;
    @Mock OutboxEventRepository outboxEvents;
    @Mock ObjectMapper objectMapper;

    @Test
    void save_baseSchedule_createsOutboxForEveryAffectedOutlet() throws Exception {
        MenuItem item = item();
        MenuItemSchedule saved = new MenuItemSchedule(101L, null,
                Map.of(Weekday.MONDAY, List.of(new com.vishesh.foodcatalog.catalog.domain.ScheduleWindow(
                        LocalTime.of(9, 0), LocalTime.of(12, 0)))));
        MenuItemChangedEvent event = org.mockito.Mockito.mock(MenuItemChangedEvent.class);

        when(menuItems.findById(101L)).thenReturn(Optional.of(item));
        when(menuItems.findAffectedOutletIds(101L)).thenReturn(List.of(501L, 502L));
        when(schedules.save(any(MenuItemSchedule.class))).thenReturn(saved);
        when(schedules.find(101L, null)).thenReturn(Optional.of(saved));
        when(schedules.find(101L, 501L)).thenReturn(Optional.empty());
        when(schedules.find(101L, 502L)).thenReturn(Optional.empty());
        when(outlets.findTimezoneById(501L)).thenReturn(Optional.of("UTC"));
        when(outlets.findTimezoneById(502L)).thenReturn(Optional.of("UTC"));
        when(eventFactory.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(outboxFactory.create(any(MenuItemChangedEvent.class), anyString())).thenReturn(
                org.mockito.Mockito.mock(OutboxEvent.class));

        MenuItemScheduleService service = service();
        MenuItemSchedule result = service.save(101L, request(null));

        assertEquals(saved, result);
        verify(outboxEvents, org.mockito.Mockito.times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void save_rejectsCrossChainOutletBeforeWritingSchedule() {
        when(menuItems.findById(101L)).thenReturn(Optional.of(item()));
        when(outlets.existsByIdAndRestaurantChainId(999L, 7L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service().save(101L, request(999L)));

        verify(schedules, never()).save(any(MenuItemSchedule.class));
        verify(outboxEvents, never()).save(any(OutboxEvent.class));
    }

    @Test
    void update_rejectsStaleScheduleVersionBeforeWritingAnything() {
        MenuItemSchedule current = new MenuItemSchedule(101L, 501L,
                Map.of(Weekday.MONDAY, List.of(new com.vishesh.foodcatalog.catalog.domain.ScheduleWindow(
                        LocalTime.of(9, 0), LocalTime.of(12, 0)))), 3L);
        when(schedules.find(101L, 501L)).thenReturn(Optional.of(current));

        assertThrows(IllegalArgumentException.class, () -> service().update(101L,
                new UpdateScheduleRequest(501L, 2L, request(501L))));

        verify(schedules, never()).save(any(MenuItemSchedule.class));
        verify(outboxEvents, never()).save(any(OutboxEvent.class));
    }

    private MenuItemScheduleService service() {
        return new MenuItemScheduleService(menuItems, outlets, schedules, eventFactory,
                outboxFactory, outboxEvents, objectMapper);
    }

    private CreateScheduleRequest request(Long outletId) {
        return new CreateScheduleRequest(outletId,
                Map.of(Weekday.MONDAY, List.of(new CreateScheduleRequest.Window(
                        LocalTime.of(9, 0), LocalTime.of(12, 0)))));
    }

    private MenuItem item() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new MenuItem(101L, "Burger", BigDecimal.TEN, now, 11L, 12L, null,
                "BURGER-101", MenuItemStatus.ACTIVE, true, true, 0L, 7L, now);
    }
}
