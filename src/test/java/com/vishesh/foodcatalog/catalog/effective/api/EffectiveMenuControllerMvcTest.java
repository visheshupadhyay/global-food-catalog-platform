package com.vishesh.foodcatalog.catalog.effective.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.OutletNotFoundException;
import com.vishesh.foodcatalog.catalog.effective.application.EffectiveMenuService;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuItem;

@WebMvcTest(EffectiveMenuController.class)
class EffectiveMenuControllerMvcTest {
    @MockitoBean
    private EffectiveMenuService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMenu_whenItemsExist_returnsItemsAndFields() throws Exception {

        List<EffectiveMenuItem> effectiveMenuItems = createFakeEffectiveMenuItemList();

        when(service.getEffectiveMenu(201L)).thenReturn(effectiveMenuItems.stream()
                .filter(item -> item.getOutletId().equals(201L))
                .toList());

        mockMvc.perform(get("/api/v1/outlets/{outletId}/menu", 201L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].menuItemId").value(101))
                .andExpect(jsonPath("$.[0].outletId").value(201))
                .andExpect(jsonPath("$.[0].itemName").value("Mc Veg Burger"))
                .andExpect(jsonPath("$.[0].itemPrice").value(1.5))
                .andExpect(jsonPath("$.[0].available").value(true))
                .andExpect(jsonPath("$.[0].visible").value(true))
                .andExpect(jsonPath("$.[1].menuItemId").value(301));

        verify(service).getEffectiveMenu(201L);
    }

    @Test
    void getMenu_whenNoItems_returnsEmptyArray() throws Exception {
        when(service.getEffectiveMenu(201L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/outlets/{outletId}/menu", 201L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).getEffectiveMenu(201L);
    }

    @Test
    void getMenu_whenOutletIsUnknown_returnsNotFoundError() throws Exception {
        when(service.getEffectiveMenu(201L)).thenThrow(new OutletNotFoundException("Outlet with ID: "+201+ " not found"));

        mockMvc.perform(get("/api/v1/outlets/{outletId}/menu", 201L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Outlet with ID: "+201+ " not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/outlets/" + 201L + "/menu"));
    }

    @Test
    void getMenu_whenOutletIdIsInvalid_returnsBadRequestError() throws Exception {
        when(service.getEffectiveMenu(201L)).thenThrow(new IllegalArgumentException("Invalid Outlet id"));

        mockMvc.perform(get("/api/v1/outlets/{outletId}/menu", 201L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid Outlet id"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/outlets/" + 201L + "/menu"));
    }

    private List<EffectiveMenuItem> createFakeEffectiveMenuItemList() {
        return List.of(
                new EffectiveMenuItem(
                        101L,
                        201L,
                        "Mc Veg Burger",
                        BigDecimal.valueOf(1.5),
                        true,
                        true),

                new EffectiveMenuItem(
                        301L,
                        201L,
                        "Veg Whooper Burger",
                        BigDecimal.valueOf(1.4),
                        true,
                        true),
                new EffectiveMenuItem(
                        501L,
                        101L,
                        "Classic Burger",
                        BigDecimal.valueOf(1.3),
                        true,
                        true));
    }
}
