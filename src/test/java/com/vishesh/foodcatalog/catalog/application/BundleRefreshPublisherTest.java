package com.vishesh.foodcatalog.catalog.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.application.CatalogEventOutboxWriter;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BundleRefreshPublisherTest {
	@Mock
	private BundleRootResolver bundleRootResolver;
	@Mock
	private BundleSnapshotAssembler bundleSnapshotAssembler;
	@Mock
	private MenuItemRepository menuItemRepository;
	@Mock
	private MenuItemChangedEventFactory eventFactory;
	@Mock
	private OutboxEventFactory outboxFactory;
	@Mock
	private OutboxEventRepository outboxEvents;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private MenuItemChangedEvent event;
	@Mock
	private OutboxEvent outboxEvent;

	private BundleRefreshPublisher publisher;

	@BeforeEach
	void setUp() {
		publisher = new BundleRefreshPublisher(bundleRootResolver, bundleSnapshotAssembler, menuItemRepository,
				eventFactory, new CatalogEventOutboxWriter(outboxFactory, outboxEvents, objectMapper));
	}

	@Test
	void childPriceChange_refreshesEveryAncestorBundleForEveryAffectedOutlet() throws Exception {
		MenuItem meal = bundle(20L, "Burger Meal");
		MenuItem combo = bundle(30L, "Weekend Combo");
		when(bundleRootResolver.findAncestorBundleIds(10L)).thenReturn(List.of(20L, 30L));
		when(menuItemRepository.findByIdAndRestaurantChainId(20L, 7L)).thenReturn(Optional.of(meal));
		when(menuItemRepository.findByIdAndRestaurantChainId(30L, 7L)).thenReturn(Optional.of(combo));
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(call -> call.getArgument(0));
		when(menuItemRepository.findAffectedOutletIds(20L)).thenReturn(List.of(501L));
		when(menuItemRepository.findAffectedOutletIds(30L)).thenReturn(List.of(501L, 502L));
		when(bundleSnapshotAssembler.assemble(20L, 7L)).thenReturn(snapshot(20L, "Burger Meal"));
		when(bundleSnapshotAssembler.assemble(30L, 7L)).thenReturn(snapshot(30L, "Weekend Combo"));
		when(eventFactory.createBundleConfigurationChanged(anyLong(), anyLong(), anyLong(), anyLong(), anyString(),
				any(), anyString(), any())).thenReturn(event);
		when(objectMapper.writeValueAsString(event)).thenReturn("{}");
		when(outboxFactory.create(eq(event), anyString())).thenReturn(outboxEvent);

		publisher.publishAncestorBundleRefreshes(10L, 7L, "price-change-10");

		verify(menuItemRepository).save(meal);
		verify(menuItemRepository).save(combo);
		verify(eventFactory, times(3)).createBundleConfigurationChanged(anyLong(), eq(7L), anyLong(), anyLong(),
				eq("price-change-10"), any(), anyString(), any());
		verify(outboxEvents, times(3)).save(outboxEvent);
	}

	@Test
	void outletSpecificScheduleRefreshesOnlyThatOutletsParentSnapshots() throws Exception {
		MenuItem meal = bundle(20L, "Burger Meal");
		when(bundleRootResolver.findAncestorBundleIds(10L)).thenReturn(List.of(20L));
		when(menuItemRepository.findByIdAndRestaurantChainId(20L, 7L)).thenReturn(Optional.of(meal));
		when(menuItemRepository.save(meal)).thenReturn(meal);
		when(menuItemRepository.findAffectedOutletIds(20L)).thenReturn(List.of(501L, 502L));
		when(bundleSnapshotAssembler.assemble(20L, 7L)).thenReturn(snapshot(20L, "Burger Meal"));
		when(eventFactory.createBundleConfigurationChanged(anyLong(), anyLong(), anyLong(), anyLong(), anyString(),
				any(), anyString(), any())).thenReturn(event);
		when(objectMapper.writeValueAsString(event)).thenReturn("{}");
		when(outboxFactory.create(eq(event), anyString())).thenReturn(outboxEvent);

		publisher.publishAncestorBundleRefreshes(10L, 7L, "outlet-501-schedule", List.of(501L));

		verify(eventFactory).createBundleConfigurationChanged(eq(20L), eq(7L), anyLong(), eq(501L),
				eq("outlet-501-schedule"), any(), anyString(), any());
		verify(outboxEvents).save(outboxEvent);
	}

	private MenuItem bundle(Long id, String name) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new MenuItem(id, name, BigDecimal.valueOf(299), now, 11L, 12L, null, "BUNDLE-" + id,
				MenuItemStatus.ACTIVE, true, true, 4L, 7L, now, MenuItemKind.BUNDLE);
	}

	private EventBundleSnapshot snapshot(Long id, String name) {
		return new EventBundleSnapshot(id, name, BigDecimal.valueOf(299), List.of());
	}
}
