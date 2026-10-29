package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupOutletRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

import com.vishesh.foodcatalog.catalog.event.application.MenuItemChangedEventFactory;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

@ExtendWith(MockitoExtension.class)
public class MenuItemServiceTest {
	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RestaurantChainRepository restaurantChainRepository;

	@Mock
	private OutletRepository outletRepository;

	@Mock
	private MenuGroupRepository menuGroupRepository;

	@Mock
	private MenuGroupOutletRepository menuGroupOutletRepository;

	@Mock
	private MenuItemChangedEventFactory menuItemChangedEventFactory;

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private ObjectMapper objectMapper;

	MenuItemService service;

	@BeforeEach
	void setUp() {
		service = new MenuItemService(menuItemRepository,
				restaurantChainRepository,
				outletRepository,
				menuGroupRepository,
				menuItemChangedEventFactory,
				outboxEventRepository,
				objectMapper,
				menuGroupOutletRepository);

		lenient().when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(anyLong()))
				.thenReturn(List.of());

		MenuItemChangedEvent event = new MenuItemChangedEvent(
				UUID.randomUUID(),
				null,
				OutboxEventType.MENU_ITEM_CHANGED,
				701L,
				101L,
				null,
				1L,
				BigDecimal.valueOf(12),
				true,
				true,
				Instant.parse("2026-01-01T00:00:00Z"),
				null, MenuItemStatus.ACTIVE, "Veg Burger");

		lenient().when(menuItemChangedEventFactory.create(
				anyLong(),
				anyLong(),
				anyLong(),
				any(),
				any(),
				any(),
				any(),
				any(),
				any(MenuItemStatus.class),
				nullable(String.class)))
				.thenReturn(event);

		lenient().when(objectMapper.writeValueAsString(any(MenuItemChangedEvent.class)))
				.thenReturn("{\"itemId\":701}");
	}

	@Test
	void createMenuItem_savesNewDraftItem() {
		when(restaurantChainRepository.findById(101L))
				.thenReturn(Optional.of(mock(RestaurantChain.class)));

		when(menuItemRepository.existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong()))
				.thenReturn(false);

		when(menuItemRepository.save(any(MenuItem.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(menuGroupRepository.findByIdAndRestaurantChainId(201L, 101L))
				.thenReturn(Optional.of(mock(MenuGroup.class)));

		String businessCode = "BH-GE-101";
		String itemName = "Mc Veg Burger";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = 101L;
		Long menuGroupId = 201L;
		Long menuCategoryId = 301L;
		Long outletId = null;

		MenuItem menuItem = service.createMenuItem(businessCode,
				itemName,
				itemPrice,
				restaurantChainId,
				menuGroupId,
				menuCategoryId,
				outletId);

		assertNotNull(menuItem);
		assertEquals(MenuItemStatus.DRAFT, menuItem.getStatus());
		assertEquals(restaurantChainId, menuItem.getRestaurantChainId());
		assertEquals(itemName, menuItem.getItemName());
		verify(menuItemRepository).save(menuItem);
		assertEquals(0L, menuItem.getVersion());
	}

	@Test
	void createMenuItem_rejectsDuplicateBusinessCode() {
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong()))
				.thenReturn(true);

		when(menuGroupRepository.findByIdAndRestaurantChainId(201L, 101L))
				.thenReturn(Optional.of(mock(MenuGroup.class)));

		String businessCode = "BH-GE-101";
		String itemName = "Mc Veg Burger";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = 101L;
		Long menuGroupId = 201L;
		Long menuCategoryId = 301L;
		Long outletId = null;

		assertThrows(DuplicateMenuItemException.class,
				() -> service.createMenuItem(businessCode,
						itemName,
						itemPrice,
						restaurantChainId,
						menuGroupId,
						menuCategoryId,
						outletId));

		verify(menuItemRepository).existsByBusinessCodeAndRestaurantChainId(businessCode, restaurantChainId);
		verify(menuItemRepository, never()).save(any(MenuItem.class));
	}

	@Test
	void createMenuItem_trimsBusinessCodeBeforeCheckingAndSaving() {
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.existsByBusinessCodeAndRestaurantChainId("BH-GE-101", 101L))
				.thenReturn(false);
		when(menuItemRepository.save(any(MenuItem.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(menuGroupRepository.findByIdAndRestaurantChainId(201L, 101L))
				.thenReturn(Optional.of(mock(MenuGroup.class)));

		MenuItem result = service.createMenuItem("  BH-GE-101  ", "Mc Veg Burger",
				BigDecimal.valueOf(1.50), 101L, 201L, 301L, null);

		assertEquals("BH-GE-101", result.getBusinessCode());
		verify(menuItemRepository)
				.existsByBusinessCodeAndRestaurantChainId("BH-GE-101", 101L);
		verify(menuItemRepository).save(result);
	}

	@Test
	void createMenuItem_rejectsMissingRestaurantChain_beforeOwnerChecks() {
		assertThrows(RestaurantChainNotFoundException.class, () -> service.createMenuItem(
				"BH-GE-101", "Mc Veg Burger", BigDecimal.valueOf(1.50),
				101L, 201L, 301L, null));

		verifyNoInteractions(menuGroupRepository);
		verify(menuItemRepository, never())
				.existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong());
		verify(menuItemRepository, never()).save(any(MenuItem.class));
	}

	@Test
	void createMenuItem_rejectsNonPositiveRestaurantChainId() {
		String businessCode = "BH-GE-101";
		String itemName = "Mc Veg Burger";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = -101L;
		Long menuGroupId = 201L;
		Long menuCategoryId = 301L;
		Long outletId = null;

		assertThrows(IllegalArgumentException.class,
				() -> service.createMenuItem(businessCode,
						itemName,
						itemPrice,
						restaurantChainId,
						menuGroupId,
						menuCategoryId,
						outletId));
		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void createMenuItem_rejectsNullDisplayName() {
		String businessCode = "BH-GE-101";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = 101L;
		Long menuGroupId = 201L;
		Long menuCategoryId = 301L;
		Long outletId = null;

		assertThrows(IllegalArgumentException.class,
				() -> service.createMenuItem(businessCode,
						null,
						itemPrice,
						restaurantChainId,
						menuGroupId,
						menuCategoryId,
						outletId));

		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void createMenuItem_rejectsBlankDisplayName() {
		String businessCode = "BH-GE-101";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = 101L;
		Long menuGroupId = 201L;
		Long menuCategoryId = 301L;
		Long outletId = null;
		assertThrows(IllegalArgumentException.class,
				() -> service.createMenuItem(businessCode,
						"    ",
						itemPrice,
						restaurantChainId,
						menuGroupId,
						menuCategoryId,
						outletId));
		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void createMenuItem_localItem_checksOutletOwnershipBeforeSaving() {
		when(outletRepository.existsByIdAndRestaurantChainId(anyLong(), anyLong())).thenReturn(true);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong()))
				.thenReturn(false);
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

		String businessCode = "BH-GE-101";
		String itemName = "Mc Veg Burger";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = 101L;
		Long menuGroupId = null;
		Long menuCategoryId = 301L;
		Long outletId = 501L;

		MenuItem menuItem = service.createMenuItem(businessCode,
				itemName,
				itemPrice,
				restaurantChainId,
				menuGroupId,
				menuCategoryId,
				outletId);

		assertNotNull(menuItem);
		assertEquals(MenuItemStatus.DRAFT, menuItem.getStatus());
		assertEquals(restaurantChainId, menuItem.getRestaurantChainId());
		assertEquals(itemName, menuItem.getItemName());
		verify(menuItemRepository).save(menuItem);
		verify(outletRepository).existsByIdAndRestaurantChainId(outletId, restaurantChainId);
		assertEquals(0L, menuItem.getVersion());
		verifyNoInteractions(menuGroupRepository);

	}

	@Test
	void createMenuItem_sharedItem_doesNotCheckOutletOwnership() {
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong()))
				.thenReturn(false);
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

		when(menuGroupRepository.findByIdAndRestaurantChainId(201L, 101L))
				.thenReturn(Optional.of(mock(MenuGroup.class)));

		String businessCode = "BH-GE-101";
		String itemName = "Mc Veg Burger";
		BigDecimal itemPrice = BigDecimal.valueOf(1.5);
		Long restaurantChainId = 101L;
		Long menuGroupId = 201L;
		Long menuCategoryId = 301L;
		Long outletId = null;

		MenuItem menuItem = service.createMenuItem(businessCode,
				itemName,
				itemPrice,
				restaurantChainId,
				menuGroupId,
				menuCategoryId,
				outletId);

		assertNotNull(menuItem);
		assertEquals(MenuItemStatus.DRAFT, menuItem.getStatus());
		assertEquals(restaurantChainId, menuItem.getRestaurantChainId());
		assertEquals(itemName, menuItem.getItemName());
		verify(menuItemRepository).save(menuItem);
		verifyNoInteractions(outletRepository);
		assertEquals(0L, menuItem.getVersion());

	}

	@Test
	void createMenuItem_rejectsMissingOrCrossChainMenuGroup_beforeDuplicateCheck() {
		when(restaurantChainRepository.findById(101L))
				.thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuGroupRepository.findByIdAndRestaurantChainId(201L, 101L))
				.thenReturn(Optional.empty());

		assertThrows(MenuGroupNotFoundException.class, () -> service.createMenuItem(
				"BH-GE-101", "Halloween Burger", BigDecimal.valueOf(1.50),
				101L, 201L, 301L, null));

		verify(menuGroupRepository).findByIdAndRestaurantChainId(201L, 101L);
		verify(menuItemRepository, never())
				.existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong());
		verify(menuItemRepository, never()).save(any(MenuItem.class));
	}

	@Test
	void createMenuItem_rejectsMissingOrMismatchedOutlet_beforeDuplicateCheck() {
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(outletRepository.existsByIdAndRestaurantChainId(501L, 101L)).thenReturn(false);

		assertThrows(OutletNotFoundException.class, () -> service.createMenuItem(
				"BH-GE-101", "Halloween Burger", BigDecimal.valueOf(1.50),
				101L, null, 301L, 501L));

		verify(outletRepository).existsByIdAndRestaurantChainId(501L, 101L);
		verify(menuItemRepository, never()).existsByBusinessCodeAndRestaurantChainId(anyString(), anyLong());
		verify(menuItemRepository, never()).save(any(MenuItem.class));
	}

	@Test
	void findMenuItemById_returnsItemWhenRepositoryFindsIt() {
		MenuItem existingItem = mock(MenuItem.class);
		when(menuItemRepository.findById(701L)).thenReturn(Optional.of(existingItem));

		MenuItem result = service.findMenuItemById(701L);

		assertEquals(existingItem, result);
		verify(menuItemRepository).findById(701L);
	}

	@Test
	void findMenuItemById_throwsNotFoundWhenRepositoryIsEmpty() {
		when(menuItemRepository.findById(701L)).thenReturn(Optional.empty());

		assertThrows(MenuItemNotFoundException.class,
				() -> service.findMenuItemById(701L));

		verify(menuItemRepository).findById(701L);
	}

	@Test
	void findMenuItemById_rejectsNullIdBeforeRepositoryCall() {
		assertThrows(IllegalArgumentException.class,
				() -> service.findMenuItemById(null));

		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void findMenuItemById_rejectsZeroIdBeforeRepositoryCall() {
		assertThrows(IllegalArgumentException.class,
				() -> service.findMenuItemById(0L));

		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void findMenuItemById_rejectsNegativeIdBeforeRepositoryCall() {
		assertThrows(IllegalArgumentException.class,
				() -> service.findMenuItemById(-1L));

		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void updateMenuItem_priceOnlyUpdate_changesPriceAndSaves() {
		MenuItem item = persistedItem(0L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L))
				.thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);

		MenuItem result = service.updateMenuItem(701L, 101L,
				new com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest(
						0L, BigDecimal.valueOf(12), null, null));

		assertEquals(BigDecimal.valueOf(12), result.getItemPrice());
		assertEquals(0L, item.getVersion());
		assertEquals(true, item.isAvailability());
		assertEquals(true, item.isVisibility());
		verify(menuItemRepository).save(item);
	}

	@Test
	void updateMenuItem_sparseFields_changeOnlyProvidedValues() {
		MenuItem item = persistedItem(2L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L))
				.thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);

		service.updateMenuItem(701L, 101L,
				new com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest(
						2L, null, false, null));

		assertEquals(BigDecimal.valueOf(10), item.getItemPrice());
		assertFalse(item.isAvailability());
		assertEquals(true, item.isVisibility());
		verify(menuItemRepository).save(item);
	}

	@Test
	void updateMenuItem_staleVersion_rejectsAndDoesNotSave() {
		MenuItem item = persistedItem(3L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L))
				.thenReturn(Optional.of(item));

		assertThrows(MenuItemVersionConflictException.class,
				() -> service.updateMenuItem(701L, 101L,
						new com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest(
								2L, BigDecimal.valueOf(12), null, null)));

		verify(menuItemRepository, never()).save(any(MenuItem.class));
	}

	@Test
	void updateMenuItem_missingChain_rejectsBeforeLoadingItem() {
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.empty());

		assertThrows(RestaurantChainNotFoundException.class,
				() -> service.updateMenuItem(701L, 101L,
						new com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest(
								0L, BigDecimal.valueOf(12), null, null)));

		verifyNoInteractions(menuItemRepository);
	}

	@Test
	void updateMenuItem_missingItemForChain_rejectsAndDoesNotSave() {
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L))
				.thenReturn(Optional.empty());

		assertThrows(MenuItemNotFoundException.class,
				() -> service.updateMenuItem(701L, 101L,
						new com.vishesh.foodcatalog.catalog.api.MenuItemUpdateRequest(
								0L, BigDecimal.valueOf(12), null, null)));

		verify(menuItemRepository, never()).save(any(MenuItem.class));
	}

	@Test
	void updateMenuItem_invalidIds_rejectBeforeRepositoryCalls() {
		MenuItemUpdateRequest request = new MenuItemUpdateRequest(
				0L, BigDecimal.valueOf(12), null, null);

		assertThrows(IllegalArgumentException.class,
				() -> service.updateMenuItem(0L, 101L, request));
		assertThrows(IllegalArgumentException.class,
				() -> service.updateMenuItem(701L, 0L, request));

		verifyNoInteractions(menuItemRepository, restaurantChainRepository);
	}

	@Test
	void updateMenuItem_visibilityOnlyUpdate_changesVisibility() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L))
				.thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, null, null, false));

		assertFalse(item.isVisibility());
		assertEquals(BigDecimal.valueOf(10), item.getItemPrice());
		assertEquals(true, item.isAvailability());
		verify(menuItemRepository).save(item);
	}

	@Test
	void updateMenuItem_statusOnlyUpdate_changesStatus() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);
		when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(201L)).thenReturn(List.of(501L));

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, null, null, null, MenuItemStatus.HIDDEN));

		assertEquals(MenuItemStatus.HIDDEN, item.getStatus());
		verify(menuItemRepository).save(item);
		verify(menuItemChangedEventFactory).create(anyLong(), anyLong(), anyLong(), any(), any(), any(),
				anyLong(), any(), eq(MenuItemStatus.HIDDEN), nullable(String.class));
		verify(menuItemChangedEventFactory).create(anyLong(), eq(101L), eq(1L), any(), any(), any(),
				anyLong(), any(), eq(MenuItemStatus.HIDDEN), nullable(String.class));
	}

	@Test
	void updateMenuItem_archivedStatus_reachesEventFactory() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);
		when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(201L)).thenReturn(List.of(501L));

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, null, null, null, MenuItemStatus.ARCHIVED));

		verify(menuItemChangedEventFactory).create(anyLong(), anyLong(), anyLong(), any(), any(), any(),
				anyLong(), any(), eq(MenuItemStatus.ARCHIVED), nullable(String.class));
		verify(outboxEventRepository).save(any(OutboxEvent.class));
	}

	@Test
	void updateMenuItem_draftItem_reachesEventFactoryWithDraftStatus() {
		MenuItem item = persistedItem(1L, MenuItemStatus.DRAFT);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);
		when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(201L)).thenReturn(List.of(501L));

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, BigDecimal.valueOf(11), null, null));

		verify(menuItemChangedEventFactory).create(anyLong(), anyLong(), anyLong(), any(), any(), any(),
				anyLong(), any(), eq(MenuItemStatus.DRAFT), nullable(String.class));
	}

	@Test
	void updateMenuItem_multipleFields_updatesAllProvidedValues() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L))
				.thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, BigDecimal.valueOf(12), false, false));

		assertEquals(BigDecimal.valueOf(12), item.getItemPrice());
		assertFalse(item.isAvailability());
		assertFalse(item.isVisibility());
		verify(menuItemRepository).save(item);
	}

	@Test
	void updateMenuItem_localItem_createsOneEventForOwningOutlet() {
		MenuItem item = localPersistedItem(1L, 501L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, BigDecimal.valueOf(12), null, null));

		verify(menuGroupOutletRepository, never()).findOutletIdsByMenuGroupId(anyLong());
		verify(menuItemChangedEventFactory).create(anyLong(), anyLong(), anyLong(), any(), any(), any(),
				eq(501L), any(), any(MenuItemStatus.class), nullable(String.class));
		verify(outboxEventRepository).save(any(OutboxEvent.class));
	}

	@Test
	void updateMenuItem_sharedItem_createsOneEventPerAssignedOutlet() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);
		when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(201L)).thenReturn(List.of(501L, 502L));

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, BigDecimal.valueOf(12), null, null));

		verify(menuGroupOutletRepository).findOutletIdsByMenuGroupId(201L);
		verify(menuItemChangedEventFactory).create(anyLong(), anyLong(), anyLong(), any(), any(), any(),
				eq(501L), any(), any(MenuItemStatus.class), nullable(String.class));
		verify(menuItemChangedEventFactory).create(anyLong(), anyLong(), anyLong(), any(), any(), any(),
				eq(502L), any(), any(MenuItemStatus.class), nullable(String.class));
		verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
	}

	@Test
	void updateMenuItem_sharedItemWithoutAssignedOutlets_createsNoEvent() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);
		when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(201L)).thenReturn(List.of());

		service.updateMenuItem(701L, 101L,
				new MenuItemUpdateRequest(1L, BigDecimal.valueOf(12), null, null));

		verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
	}

	@Test
    void updateMenuItem_priceOnlyUpdate_sendsOnlyChangedFieldsInEvent() {
		MenuItem item = persistedItem(1L);
		when(restaurantChainRepository.findById(101L)).thenReturn(Optional.of(mock(RestaurantChain.class)));
		when(menuItemRepository.findByIdAndRestaurantChainId(701L, 101L)).thenReturn(Optional.of(item));
		when(menuItemRepository.save(item)).thenReturn(item);
		when(menuGroupOutletRepository.findOutletIdsByMenuGroupId(201L)).thenReturn(List.of(401L));

		service.updateMenuItem(
						item.getId(),
						item.getRestaurantChainId(),
			new MenuItemUpdateRequest(1L, BigDecimal.valueOf(12), null, null));

		ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
		ArgumentCaptor<Boolean> availabilityCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<Boolean> visibilityCaptor = ArgumentCaptor.forClass(Boolean.class);
		ArgumentCaptor<MenuItemStatus> statusCaptor = ArgumentCaptor.forClass(MenuItemStatus.class);
		ArgumentCaptor<String> itemNameCaptor = ArgumentCaptor.forClass(String.class);

		verify(menuItemChangedEventFactory).create(
				eq(701L),
				eq(101L),
				eq(1L),
				priceCaptor.capture(),
				availabilityCaptor.capture(),
				visibilityCaptor.capture(),
				eq(401L),
				isNull(),
				statusCaptor.capture(),
				itemNameCaptor.capture());

		assertEquals(BigDecimal.valueOf(12), priceCaptor.getValue());
		assertNull(availabilityCaptor.getValue());
		assertNull(visibilityCaptor.getValue());
		assertNull(itemNameCaptor.getValue());
		assertEquals(MenuItemStatus.ACTIVE, statusCaptor.getValue());

	}

	private MenuItem persistedItem(Long version) {
		return persistedItem(version, MenuItemStatus.ACTIVE);
	}

	private MenuItem persistedItem(Long version, MenuItemStatus status) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new MenuItem(701L,
				"Burger",
				BigDecimal.valueOf(10), now,
				201L,
				301L,
				null,
				"BURGER-701",
				status,
				true,
				true,
				version,
				101L,
				now);
	}

	private MenuItem localPersistedItem(Long version, Long outletId) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new MenuItem(701L,
				"Burger",
				BigDecimal.valueOf(10),
				now,
				null,
				301L,
				outletId,
				"BURGER-701",
				MenuItemStatus.ACTIVE,
				true,
				true,
				version,
				101L,
				now);
	}

}
