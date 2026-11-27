package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.event.application.CatalogEventOutboxWriter;
import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BundleCompositionServiceTest {
	@Mock
	private MenuItemRepository menuItemRepository;
	@Mock
	private RestaurantChainRepository restaurantChainRepository;
	@Mock
	private BundleChildRepository bundleChildRepository;
	@Mock
	private BundleGraphValidator bundleGraphValidator;
	@Mock
	private BundleRootResolver bundleRootResolver;
	@Mock
	private BundleSnapshotAssembler bundleSnapshotAssembler;
	@Mock
	private MenuItemChangedEventFactory menuItemChangedEventFactory;
	@Mock
	private OutboxEventFactory outboxEventFactory;
	@Mock
	private OutboxEventRepository outboxEventRepository;
	@Mock
	private ObjectMapper objectMapper;

	private BundleCompositionService service;

	@BeforeEach
	void setUp() {
		service = new BundleCompositionService(menuItemRepository, restaurantChainRepository, bundleChildRepository,
				bundleGraphValidator, bundleRootResolver, bundleSnapshotAssembler, menuItemChangedEventFactory,
				new CatalogEventOutboxWriter(outboxEventFactory, outboxEventRepository, objectMapper));
	}

	@Test
	void replaceChildren_graphValidationFails_doesNotPersistAnything() {
		MenuItem bundle = sharedMenuItem(1L, MenuItemKind.BUNDLE);
		MenuItem burger = sharedMenuItem(2L, MenuItemKind.PRODUCT);
		List<BundleChild> children = List.of(new BundleChild(1L, 2L, 1, 0));

		stubValidChange(bundle, burger);
		doThrow(new IllegalArgumentException("Circular structure")).when(bundleGraphValidator).validateReplacement(1L,
				children);

		assertThrows(IllegalArgumentException.class, () -> service.replaceChildren(1L, 100L, 4L, children));

		verify(menuItemRepository, never()).save(any(MenuItem.class));
		verify(bundleChildRepository, never()).replaceForBundle(any(), any());
		verify(outboxEventRepository, never()).save(any());
	}

	@Test
	void replaceChildren_validGraph_savesChildrenAndOneBundleOutboxEvent() throws Exception {
		MenuItem bundle = sharedMenuItem(1L, MenuItemKind.BUNDLE);
		MenuItem burger = sharedMenuItem(2L, MenuItemKind.PRODUCT);
		List<BundleChild> children = List.of(new BundleChild(1L, 2L, 1, 0));
		EventBundleSnapshot snapshot = snapshot(1L);
		MenuItemChangedEvent event = mock(MenuItemChangedEvent.class);
		OutboxEvent outboxEvent = mock(OutboxEvent.class);

		stubValidChange(bundle, burger);
		when(bundleRootResolver.findAffectedBundleIds(1L)).thenReturn(List.of(1L));
		when(menuItemRepository.findAffectedOutletIds(1L)).thenReturn(List.of(501L));
		when(menuItemRepository.save(bundle)).thenReturn(bundle);
		when(bundleChildRepository.replaceForBundle(1L, children)).thenReturn(children);
		when(bundleSnapshotAssembler.assemble(1L, 100L)).thenReturn(snapshot);
		when(menuItemChangedEventFactory.createBundleConfigurationChanged(eq(1L), eq(100L), eq(4L), eq(501L), any(),
				eq(MenuItemStatus.ACTIVE), eq("Item 1"), eq(snapshot))).thenReturn(event);
		when(objectMapper.writeValueAsString(event)).thenReturn("{bundle-json}");
		when(outboxEventFactory.create(event, "{bundle-json}")).thenReturn(outboxEvent);

		service.replaceChildren(1L, 100L, 4L, children);

		verify(menuItemRepository).save(bundle);
		verify(bundleChildRepository).replaceForBundle(1L, children);
		verify(bundleSnapshotAssembler).assemble(1L, 100L);
		verify(outboxEventRepository).save(outboxEvent);
	}

	@Test
	void replaceChildren_nestedBundleChange_refreshesChangedBundleAndEveryParent() throws Exception {
		MenuItem burgerMeal = sharedMenuItem(1L, MenuItemKind.BUNDLE);
		MenuItem burger = sharedMenuItem(2L, MenuItemKind.PRODUCT);
		MenuItem weekendCombo = sharedMenuItem(3L, MenuItemKind.BUNDLE);
		List<BundleChild> children = List.of(new BundleChild(1L, 2L, 1, 0));
		EventBundleSnapshot burgerMealSnapshot = snapshot(1L);
		EventBundleSnapshot weekendComboSnapshot = snapshot(3L);
		MenuItemChangedEvent mealEvent = mock(MenuItemChangedEvent.class);
		MenuItemChangedEvent comboEvent = mock(MenuItemChangedEvent.class);
		OutboxEvent mealOutbox = mock(OutboxEvent.class);
		OutboxEvent comboOutbox = mock(OutboxEvent.class);

		stubValidChange(burgerMeal, burger);
		when(bundleRootResolver.findAffectedBundleIds(1L)).thenReturn(List.of(1L, 3L));
		when(menuItemRepository.findAffectedOutletIds(1L)).thenReturn(List.of(501L));
		when(menuItemRepository.findAffectedOutletIds(3L)).thenReturn(List.of(501L));
		when(menuItemRepository.findByIdAndRestaurantChainId(3L, 100L)).thenReturn(Optional.of(weekendCombo));
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(bundleChildRepository.replaceForBundle(1L, children)).thenReturn(children);
		when(bundleSnapshotAssembler.assemble(1L, 100L)).thenReturn(burgerMealSnapshot);
		when(bundleSnapshotAssembler.assemble(3L, 100L)).thenReturn(weekendComboSnapshot);
		when(menuItemChangedEventFactory.createBundleConfigurationChanged(eq(1L), eq(100L), eq(4L), eq(501L), any(),
				eq(MenuItemStatus.ACTIVE), eq("Item 1"), eq(burgerMealSnapshot))).thenReturn(mealEvent);
		when(menuItemChangedEventFactory.createBundleConfigurationChanged(eq(3L), eq(100L), eq(4L), eq(501L), any(),
				eq(MenuItemStatus.ACTIVE), eq("Item 3"), eq(weekendComboSnapshot))).thenReturn(comboEvent);
		when(objectMapper.writeValueAsString(mealEvent)).thenReturn("{meal}");
		when(objectMapper.writeValueAsString(comboEvent)).thenReturn("{combo}");
		when(outboxEventFactory.create(mealEvent, "{meal}")).thenReturn(mealOutbox);
		when(outboxEventFactory.create(comboEvent, "{combo}")).thenReturn(comboOutbox);

		service.replaceChildren(1L, 100L, 4L, children);

		verify(menuItemRepository, times(2)).save(any(MenuItem.class));
		verify(bundleSnapshotAssembler).assemble(1L, 100L);
		verify(bundleSnapshotAssembler).assemble(3L, 100L);
		verify(outboxEventRepository).save(mealOutbox);
		verify(outboxEventRepository).save(comboOutbox);

		ArgumentCaptor<String> correlationIds = ArgumentCaptor.forClass(String.class);
		verify(menuItemChangedEventFactory).createBundleConfigurationChanged(eq(1L), eq(100L), eq(4L), eq(501L),
				correlationIds.capture(), eq(MenuItemStatus.ACTIVE), eq("Item 1"), eq(burgerMealSnapshot));
		verify(menuItemChangedEventFactory).createBundleConfigurationChanged(eq(3L), eq(100L), eq(4L), eq(501L),
				correlationIds.capture(), eq(MenuItemStatus.ACTIVE), eq("Item 3"), eq(weekendComboSnapshot));
		assertEquals(correlationIds.getAllValues().getFirst(), correlationIds.getAllValues().getLast());
	}

	@Test
	void replaceChildren_withoutAnAffectedOutlet_rejectsBeforePersistingAnything() {
		MenuItem bundle = sharedMenuItem(1L, MenuItemKind.BUNDLE);
		MenuItem burger = sharedMenuItem(2L, MenuItemKind.PRODUCT);
		List<BundleChild> children = List.of(new BundleChild(1L, 2L, 1, 0));

		stubValidChange(bundle, burger);
		when(bundleRootResolver.findAffectedBundleIds(1L)).thenReturn(List.of(1L));
		when(menuItemRepository.findAffectedOutletIds(1L)).thenReturn(List.of());

		assertThrows(IllegalArgumentException.class, () -> service.replaceChildren(1L, 100L, 4L, children));

		verify(menuItemRepository, never()).save(any(MenuItem.class));
		verify(bundleChildRepository, never()).replaceForBundle(any(), any());
		verify(outboxEventRepository, never()).save(any());
	}

	@Test
	void replaceChildren_outboxPersistenceFailureEscapesSoTheTransactionCanRollBack() throws Exception {
		MenuItem bundle = sharedMenuItem(1L, MenuItemKind.BUNDLE);
		MenuItem burger = sharedMenuItem(2L, MenuItemKind.PRODUCT);
		List<BundleChild> children = List.of(new BundleChild(1L, 2L, 1, 0));
		EventBundleSnapshot snapshot = snapshot(1L);
		MenuItemChangedEvent event = mock(MenuItemChangedEvent.class);
		OutboxEvent outboxEvent = mock(OutboxEvent.class);

		stubValidChange(bundle, burger);
		when(bundleRootResolver.findAffectedBundleIds(1L)).thenReturn(List.of(1L));
		when(menuItemRepository.findAffectedOutletIds(1L)).thenReturn(List.of(501L));
		when(menuItemRepository.save(bundle)).thenReturn(bundle);
		when(bundleChildRepository.replaceForBundle(1L, children)).thenReturn(children);
		when(bundleSnapshotAssembler.assemble(1L, 100L)).thenReturn(snapshot);
		when(menuItemChangedEventFactory.createBundleConfigurationChanged(eq(1L), eq(100L), eq(4L), eq(501L), any(),
				eq(MenuItemStatus.ACTIVE), eq("Item 1"), eq(snapshot))).thenReturn(event);
		when(objectMapper.writeValueAsString(event)).thenReturn("{bundle-json}");
		when(outboxEventFactory.create(event, "{bundle-json}")).thenReturn(outboxEvent);
		when(outboxEventRepository.save(outboxEvent)).thenThrow(new IllegalStateException("Outbox unavailable"));

		assertThrows(IllegalStateException.class, () -> service.replaceChildren(1L, 100L, 4L, children));

		// This is a unit test, so it proves the failure escapes. Spring's real
		// @Transactional proxy then uses that unchecked exception to roll back
		// the already attempted relationship/version writes.
		verify(outboxEventRepository).save(outboxEvent);
	}

	private void stubValidChange(MenuItem bundle, MenuItem child) {
		when(restaurantChainRepository.findById(100L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(1L, 100L)).thenReturn(Optional.of(bundle));
		when(menuItemRepository.findByIdAndRestaurantChainId(2L, 100L)).thenReturn(Optional.of(child));
	}

	private EventBundleSnapshot snapshot(Long id) {
		return new EventBundleSnapshot(id, "Item " + id, BigDecimal.TEN, List.of());
	}

	private MenuItem sharedMenuItem(Long id, MenuItemKind kind) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new MenuItem(id, "Item " + id, BigDecimal.TEN, now, 10L, 20L, null, "ITEM-" + id, MenuItemStatus.ACTIVE,
				true, true, 4L, 100L, now, kind);
	}
}
