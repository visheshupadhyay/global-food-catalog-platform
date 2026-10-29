package com.vishesh.foodcatalog.catalog.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.DuplicateMenuItemException;
import com.vishesh.foodcatalog.catalog.application.MenuItemService;
import com.vishesh.foodcatalog.catalog.application.MenuItemNotFoundException;
import com.vishesh.foodcatalog.catalog.application.MenuItemVersionConflictException;
import com.vishesh.foodcatalog.catalog.application.RestaurantChainNotFoundException;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

@WebMvcTest(MenuItemController.class)
public class MenuItemControllerMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemService service;

    @Test
    void createSharedMenuItem_returns201WithPersistedItem() throws Exception {
        String businessCode = "BH-GE-101";
        String itemName = "Mc Veg Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = 201L;
        Long menuCategoryId = 301L;
        Long outletId = null;

        MenuItem savedItem = persistedMenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);

        when(service.createMenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId)).thenReturn(savedItem);

        mockMvc.perform(post("/api/v1/restaurant-chains/" + restaurantChainId + "/menu-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                               {
                                 "businessCode": "BH-GE-101",
                                 "itemName": "Mc Veg Burger",
                                 "itemPrice": 1.5,
                                 "menuGroupId": 201,
                                 "menuCategoryId": 301
                               }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.businessCode").value(savedItem.getBusinessCode()))
                .andExpect(jsonPath("$.itemName").value(savedItem.getItemName()))
                .andExpect(jsonPath("$.itemPrice").value(savedItem.getItemPrice()))
                .andExpect(jsonPath("$.restaurantChainId").value(savedItem.getRestaurantChainId()))
                .andExpect(jsonPath("$.version").value(savedItem.getVersion()))
                .andExpect(jsonPath("$.status").value(savedItem.getStatus().name()))
                .andExpect(jsonPath("$.createdAt")
                        .value(savedItem.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updatedAt")
                        .value(savedItem.getUpdatedAt().toString()))
                .andExpect(jsonPath("$.outletId").value(savedItem.getOutletId()))
                .andExpect(jsonPath("$.menuGroupId").value(savedItem.getMenuGroupId()))
                .andExpect(jsonPath("$.menuCategoryId").value(savedItem.getMenuCategoryId()))
                .andExpect(header().string("Location",
                        "/api/v1/restaurant-chains/" + restaurantChainId
                                + "/menu-items/" + savedItem.getId()));

        verify(service).createMenuItem(businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);
    }

    @Test
    void createOutletMenuItem_returns201WithPersistedItem() throws Exception {
        String businessCode = "BH-GE-101";
        String itemName = "Halloween Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = null;
        Long menuCategoryId = 301L;
        Long outletId = 201L;

         MenuItem savedItem = persistedMenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);

        when(service.createMenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId)).thenReturn(savedItem);
    
        mockMvc.perform(post("/api/v1/restaurant-chains/" + restaurantChainId + "/outlets/" + outletId + "/menu-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                               {
                                 "businessCode": "BH-GE-101",
                                 "itemName": "Halloween Burger",
                                 "itemPrice": 1.5,
                                 "outletId": 201,
                                 "menuCategoryId": 301
                               }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.businessCode").value(savedItem.getBusinessCode()))
                .andExpect(jsonPath("$.itemName").value(savedItem.getItemName()))
                .andExpect(jsonPath("$.itemPrice").value(savedItem.getItemPrice()))
                .andExpect(jsonPath("$.restaurantChainId").value(savedItem.getRestaurantChainId()))
                .andExpect(jsonPath("$.version").value(savedItem.getVersion()))
                .andExpect(jsonPath("$.status").value(savedItem.getStatus().name()))
                .andExpect(jsonPath("$.createdAt")
                        .value(savedItem.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updatedAt")
                        .value(savedItem.getUpdatedAt().toString()))
                .andExpect(jsonPath("$.outletId").value(savedItem.getOutletId()))
                .andExpect(jsonPath("$.menuCategoryId").value(savedItem.getMenuCategoryId()))
                .andExpect(header().string("Location",
                        "/api/v1/restaurant-chains/" + restaurantChainId + "/outlets/" + outletId + "/menu-items/" + savedItem.getId()));

        verify(service).createMenuItem(businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId);
    }

    @Test
    void invalidSharedAndOutletRequests_return400() throws Exception {
        Long restaurantChainId =101L;
        Long outletId = 201L;
        mockMvc.perform(post("/api/v1/restaurant-chains/" + restaurantChainId + "/outlets/" + outletId + "/menu-items")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(post("/api/v1/restaurant-chains/" + restaurantChainId + "/menu-items")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test 
    void duplicateOutletMenuItem_returns409() throws Exception {
        String businessCode = "BH-GE-101";
        String itemName = "Halloween Burger";
        BigDecimal itemPrice = BigDecimal.valueOf(1.5);
        Long restaurantChainId = 101L;
        Long menuGroupId = null;
        Long menuCategoryId = 301L;
        Long outletId = 201L;

        when(service.createMenuItem(
                businessCode,
                itemName,
                itemPrice,
                restaurantChainId,
                menuGroupId,
                menuCategoryId,
                outletId)).thenThrow(new DuplicateMenuItemException("Menu item already exists"));
        
        mockMvc.perform(post("/api/v1/restaurant-chains/" + restaurantChainId + "/outlets/" + outletId + "/menu-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                               {
                                 "businessCode": "BH-GE-101",
                                 "itemName": "Halloween Burger",
                                 "itemPrice": 1.5,
                                 "outletId": 201,
                                 "menuCategoryId": 301
                               }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void missingRestaurantChain_returns404WithApiError() throws Exception {
        Long restaurantChainId = 901L;
        when(service.createMenuItem(
                "BH-GE-101",
                "Mc Veg Burger",
                BigDecimal.valueOf(1.5),
                restaurantChainId,
                201L,
                301L,
                null)).thenThrow(new RestaurantChainNotFoundException(
                        "Restaurant chain with ID: 901 not found"));

        mockMvc.perform(post("/api/v1/restaurant-chains/" + restaurantChainId + "/menu-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "businessCode": "BH-GE-101",
                          "itemName": "Mc Veg Burger",
                          "itemPrice": 1.5,
                          "menuGroupId": 201,
                          "menuCategoryId": 301
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant chain with ID: 901 not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/901/menu-items"));
    }

    @Test
    void updateMenuItem_statusChange_returnsUpdatedStatus() throws Exception {
        MenuItem savedItem = persistedMenuItem("BH-GE-101", "Mc Veg Burger", BigDecimal.valueOf(1.5),
                101L, 201L, 301L, null);
        MenuItem hiddenItem = new MenuItem(savedItem.getId(), savedItem.getItemName(), savedItem.getItemPrice(),
                savedItem.getCreatedAt(), savedItem.getMenuGroupId(), savedItem.getMenuCategoryId(),
                savedItem.getOutletId(), savedItem.getBusinessCode(), MenuItemStatus.HIDDEN,
                false, true, 1L, savedItem.getRestaurantChainId(), savedItem.getUpdatedAt());

        when(service.updateMenuItem(eq(401L), eq(101L), any(MenuItemUpdateRequest.class)))
                .thenReturn(hiddenItem);

        mockMvc.perform(patch("/api/v1/restaurant-chains/101/menu-items/401")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "expectedVersion": 0,
                          "status": "HIDDEN"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(401))
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.version").value(1));

        verify(service).updateMenuItem(eq(401L), eq(101L), any(MenuItemUpdateRequest.class));
    }

    @Test
    void updateMenuItem_archivedStatus_returnsArchived() throws Exception {
        MenuItem archived = new MenuItem(401L, "Burger", BigDecimal.TEN,
                Instant.parse("2026-01-01T00:00:00Z"), 201L, 301L, null,
                "BURGER-401", MenuItemStatus.ARCHIVED, true, true, 1L, 101L,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(service.updateMenuItem(eq(401L), eq(101L), any(MenuItemUpdateRequest.class))).thenReturn(archived);

        mockMvc.perform(patch("/api/v1/restaurant-chains/101/menu-items/401")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void updateMenuItem_staleVersion_returns409() throws Exception {
        when(service.updateMenuItem(eq(401L), eq(101L), any(MenuItemUpdateRequest.class)))
                .thenThrow(new MenuItemVersionConflictException("Menu item version is stale"));

        mockMvc.perform(patch("/api/v1/restaurant-chains/101/menu-items/401")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"status\":\"HIDDEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateMenuItem_missingItem_returns404() throws Exception {
        when(service.updateMenuItem(eq(999L), eq(101L), any(MenuItemUpdateRequest.class)))
                .thenThrow(new MenuItemNotFoundException("Menu item was not found"));

        mockMvc.perform(patch("/api/v1/restaurant-chains/101/menu-items/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"status\":\"HIDDEN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateMenuItem_missingRestaurantChain_returns404() throws Exception {
        when(service.updateMenuItem(eq(401L), eq(999L), any(MenuItemUpdateRequest.class)))
                .thenThrow(new RestaurantChainNotFoundException("Restaurant chain was not found"));

        mockMvc.perform(patch("/api/v1/restaurant-chains/999/menu-items/401")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"status\":\"HIDDEN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateMenuItem_invalidTransition_returns409() throws Exception {
        when(service.updateMenuItem(eq(401L), eq(101L), any(MenuItemUpdateRequest.class)))
                .thenThrow(new IllegalStateException("Cannot change menu item status"));

        mockMvc.perform(patch("/api/v1/restaurant-chains/101/menu-items/401")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }


    private MenuItem persistedMenuItem(String businessCode,
            String itemName,
            BigDecimal itemPrice,
            Long restaurantChainId,
            Long menuGroupId,
            Long menuCategoryId,
            Long outletId) {
        // This fixture represents the object after the database has saved it.
        // Therefore it has an ID, timestamps, status, and persistence version.
        Instant createdAt = Instant.parse("2026-10-23T08:03:06.069040700Z");
        Instant updatedAt = Instant.parse("2026-10-23T08:03:06.069040700Z");

        return new MenuItem(
                401L,
                itemName,
                itemPrice,
                createdAt,
                menuGroupId,
                menuCategoryId,
                outletId,
                businessCode,
                MenuItemStatus.DRAFT,
                true,
                true,
                0L,
                restaurantChainId,
                updatedAt);
    }
}
