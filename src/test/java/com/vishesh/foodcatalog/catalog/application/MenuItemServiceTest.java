package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.MenuGroup;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.repository.MenuGroupRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;
import com.vishesh.foodcatalog.catalog.repository.OutletRepository;
import com.vishesh.foodcatalog.catalog.repository.RestaurantChainRepository;

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

    MenuItemService service;

    @BeforeEach
    void setUp() {
        service = new MenuItemService(menuItemRepository, 
            restaurantChainRepository, 
            outletRepository,
            menuGroupRepository);
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
}
