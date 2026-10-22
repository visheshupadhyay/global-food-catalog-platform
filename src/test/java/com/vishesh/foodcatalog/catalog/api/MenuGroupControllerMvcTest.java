package com.vishesh.foodcatalog.catalog.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.DuplicateMenuGroupException;
import com.vishesh.foodcatalog.catalog.application.MenuGroupNotFoundException;
import com.vishesh.foodcatalog.catalog.application.MenuGroupService;
import com.vishesh.foodcatalog.catalog.application.RestaurantChainNotFoundException;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuGroup;

@WebMvcTest(MenuGroupController.class)
class MenuGroupControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuGroupService service;

    @Test
    void successfulCreation_returns201WithLocationAndMenuGroup() throws Exception {
        MenuGroup savedGroup = persistedMenuGroup(42L, 101L, "Burgers");
        when(service.createMenuGroup(101L, "Burgers")).thenReturn(savedGroup);

        mockMvc.perform(post("/api/v1/restaurant-chains/101/menu-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Burgers"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.restaurantChainId").value(101))
                .andExpect(jsonPath("$.displayName").value("Burgers"))
                .andExpect(jsonPath("$.status").value(ChainStatus.DRAFT.name()))
                .andExpect(header().string("Location",
                        "/api/v1/restaurant-chains/101/menu-groups/42"));

        verify(service).createMenuGroup(101L, "Burgers");
    }

    @Test
    void duplicateName_returns409() throws Exception {
        when(service.createMenuGroup(101L, "Burgers"))
                .thenThrow(new DuplicateMenuGroupException(
                        "Menu group with name: Burgers already exists"));

        mockMvc.perform(post("/api/v1/restaurant-chains/101/menu-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Burgers"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Menu group with name: Burgers already exists"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/101/menu-groups"));
    }

    @Test
    void missingRestaurantChain_returns404() throws Exception {
        when(service.createMenuGroup(999L, "Burgers"))
                .thenThrow(new RestaurantChainNotFoundException(
                        "Restaurant chain with ID: 999 not found"));

        mockMvc.perform(post("/api/v1/restaurant-chains/999/menu-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Burgers"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant chain with ID: 999 not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/999/menu-groups"));

        verify(service).createMenuGroup(999L, "Burgers");
    }

    @Test
    void zeroChainId_returns400() throws Exception {
        when(service.createMenuGroup(0L, "Burgers"))
                .thenThrow(new IllegalArgumentException(
                        "Restaurant chain ID cannot be less than or equal to 0"));

        mockMvc.perform(post("/api/v1/restaurant-chains/0/menu-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Burgers"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void blankDisplayName_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/v1/restaurant-chains/101/menu-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void getMenuGroup_returns200WithCompleteMenuGroup() throws Exception {
        MenuGroup menuGroup = persistedMenuGroup(42L,
                101L,
                "Burgers");

        when(service.findMenuGroupById(101L, 42L)).thenReturn(menuGroup);

        mockMvc.perform(get("/api/v1/restaurant-chains/101/menu-groups/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.restaurantChainId").value(101))
                .andExpect(jsonPath("$.displayName").value("Burgers"))
                .andExpect(jsonPath("$.status").value(ChainStatus.DRAFT.name()));

        verify(service).findMenuGroupById(101L, 42L);
    }

    @Test
    void missingId_returns404() throws Exception {
        when(service.findMenuGroupById(anyLong(),anyLong()))
                .thenThrow(new MenuGroupNotFoundException("Menu group with ID: 901 not found"));

        mockMvc.perform(get("/api/v1/restaurant-chains/101/menu-groups/" + 901))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Menu group with ID: 901 not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/101/menu-groups/" + 901));
    }

    @Test
    void getMenuGroup_whenChainDoesNotOwnGroup_returns404() throws Exception {
        MenuGroup menuGroup = persistedMenuGroup(42L,
                101L,
                "Burgers");

        when(service.findMenuGroupById(101L, 42L)).thenReturn(menuGroup);
        when(service.findMenuGroupById(201L,42L))
                .thenThrow(new MenuGroupNotFoundException("Menu group with ID: 42 not found"));

        mockMvc.perform(get("/api/v1/restaurant-chains/201/menu-groups/" + 42))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Menu group with ID: 42 not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/201/menu-groups/" + 42));
    }

    private MenuGroup persistedMenuGroup(Long id, Long restaurantChainId, String displayName) {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        return new MenuGroup(restaurantChainId, displayName,
                ChainStatus.DRAFT, timestamp, timestamp, 3L,id);
    }
}
