package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import com.vishesh.foodcatalog.catalog.api.SeasonalAvailabilityRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.application.*;
import com.vishesh.foodcatalog.catalog.event.domain.*;
import com.vishesh.foodcatalog.catalog.repository.*;

@ExtendWith(MockitoExtension.class)
class MenuItemSeasonalAvailabilityServiceTest {
	@Mock
	MenuItemRepository menuItems;
	@Mock
	OutletRepository outlets;
	@Mock
	MenuItemSeasonalAvailabilityRepository seasons;
	@Mock
	MenuItemChangedEventFactory eventFactory;
	@Mock
	OutboxEventFactory outboxFactory;
	@Mock
	OutboxEventRepository outboxEvents;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	BundleRefreshPublisher bundleRefreshPublisher;

	@Test
	void save_baseSeason_createsOutboxForEveryAffectedOutlet() throws Exception {
		MenuItemSeasonalAvailability saved = new MenuItemSeasonalAvailability(101L, null, LocalDate.of(2026, 10, 15),
				LocalDate.of(2026, 11, 5), 0L);
		MenuItemChangedEvent event = mock(MenuItemChangedEvent.class);
		when(menuItems.findById(101L)).thenReturn(Optional.of(item()));
		when(menuItems.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(menuItems.findAffectedOutletIds(101L)).thenReturn(List.of(501L, 502L));
		when(seasons.save(any())).thenReturn(saved);
		// The first lookup decides whether this is a creation. The later
		// lookups happen after persistence while events are built.
		when(seasons.find(101L, null)).thenReturn(Optional.empty()).thenReturn(Optional.of(saved));
		when(seasons.find(101L, 501L)).thenReturn(Optional.empty());
		when(seasons.find(101L, 502L)).thenReturn(Optional.empty());
		when(outlets.findTimezoneById(anyLong())).thenReturn(Optional.of("UTC"));
		when(eventFactory.createSeasonalAvailabilityChanged(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
				anyString(), any(), anyBoolean(), anyString(), any(), any())).thenReturn(event);
		when(objectMapper.writeValueAsString(event)).thenReturn("{}");
		when(outboxFactory.create(any(MenuItemChangedEvent.class), anyString())).thenReturn(mock(OutboxEvent.class));

		MenuItemSeasonalAvailability result = service().save(101L,
				new SeasonalAvailabilityRequest(null, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 5), null));

		assertEquals(saved, result);
		verify(outboxEvents, times(2)).save(any(OutboxEvent.class));
		verify(bundleRefreshPublisher).publishAncestorBundleRefreshes(eq(101L), eq(7L), anyString(), isNull());
	}

	@Test
	void save_rejectsStaleVersionBeforeWriting() {
		when(menuItems.findById(101L)).thenReturn(Optional.of(item()));
		when(seasons.find(101L, null)).thenReturn(Optional.of(new MenuItemSeasonalAvailability(101L, null,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 5), 3L)));
		assertThrows(IllegalArgumentException.class, () -> service().save(101L,
				new SeasonalAvailabilityRequest(null, LocalDate.of(2026, 10, 16), LocalDate.of(2026, 11, 5), 2L)));
		verify(seasons, never()).save(any());
		verifyNoInteractions(outboxEvents);
	}

	private MenuItemSeasonalAvailabilityService service() {
		return new MenuItemSeasonalAvailabilityService(menuItems, outlets, seasons, eventFactory,
				new CatalogEventOutboxWriter(outboxFactory, outboxEvents, objectMapper), bundleRefreshPublisher);
	}

	private MenuItem item() {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new MenuItem(101L, "Burger", BigDecimal.TEN, now, 11L, 12L, null, "BURGER-101", MenuItemStatus.ACTIVE,
				true, true, 0L, 7L, now);
	}
}
