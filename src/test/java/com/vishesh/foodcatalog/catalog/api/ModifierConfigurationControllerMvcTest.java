package com.vishesh.foodcatalog.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.MenuItemNotFoundException;
import com.vishesh.foodcatalog.catalog.application.MenuItemVersionConflictException;
import com.vishesh.foodcatalog.catalog.application.ModifierConfigurationService;
import com.vishesh.foodcatalog.catalog.application.ReplaceModifierConfigurationCommand;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;

/** Verifies the public HTTP contract, validation, and DTO-to-command mapping. */
@WebMvcTest(ModifierConfigurationController.class)
class ModifierConfigurationControllerMvcTest {
    private static final String URL = "/api/v1/restaurant-chains/1/menu-items/10/modifier-configuration";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModifierConfigurationService service;

    @Test
    void replace_mapsCompleteRequestAndReturnsMenuItemVersion() throws Exception {
        MenuItem savedMenuItem = savedMenuItem(10L, 6L);
        when(service.replaceConfiguration(any())).thenReturn(savedMenuItem);

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "expectedVersion": 5,
                          "groups": [{
                            "groupId": 50,
                            "groupName": "Choose extras",
                            "minimumSelections": 0,
                            "maximumSelections": 2,
                            "displayOrder": 0,
                            "options": [{
                              "optionId": 60,
                              "optionName": "Cheese",
                              "priceDelta": 20.00,
                              "displayOrder": 0
                            }]
                          }]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuItemId").value(10))
                .andExpect(jsonPath("$.version").value(6));

        ArgumentCaptor<ReplaceModifierConfigurationCommand> command = ArgumentCaptor.forClass(
                ReplaceModifierConfigurationCommand.class);
        verify(service).replaceConfiguration(command.capture());
        ReplaceModifierConfigurationCommand captured = command.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(10L, captured.ownerMenuItemId());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captured.restaurantChainId());
        org.junit.jupiter.api.Assertions.assertEquals(5L, captured.expectedVersion());
        org.junit.jupiter.api.Assertions.assertEquals(50L, captured.groups().getFirst().groupId());
        org.junit.jupiter.api.Assertions.assertEquals(60L, captured.groups().getFirst().options().getFirst().optionId());
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("20.00").compareTo(
                captured.groups().getFirst().options().getFirst().priceDelta()));
    }

    @Test
    void replace_allowsAnEmptyGroupListToRemoveAllGroups() throws Exception {
        // Build this mock before stubbing the service. Mockito cannot begin a
        // second stubbing operation while the first `when(...).thenReturn(...)`
        // call is still being assembled.
        MenuItem savedMenuItem = savedMenuItem(10L, 6L);
        when(service.replaceConfiguration(any())).thenReturn(savedMenuItem);

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":5,\"groups\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(6));

        ArgumentCaptor<ReplaceModifierConfigurationCommand> command = ArgumentCaptor.forClass(
                ReplaceModifierConfigurationCommand.class);
        verify(service).replaceConfiguration(command.capture());
        org.junit.jupiter.api.Assertions.assertTrue(command.getValue().groups().isEmpty());
    }

    @Test
    void replace_missingExpectedVersion_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groups\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void replace_groupWithoutOptions_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"expectedVersion":5,"groups":[{
                          "groupName":"Choose extras", "minimumSelections":0,
                          "maximumSelections":1, "displayOrder":0, "options":[]
                        }]}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void replace_negativePriceDelta_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"expectedVersion":5,"groups":[{
                          "groupName":"Choose extras", "minimumSelections":0,
                          "maximumSelections":1, "displayOrder":0,
                          "options":[{"optionName":"Cheese", "priceDelta":-1, "displayOrder":0}]
                        }]}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void replace_staleVersion_returns409() throws Exception {
        when(service.replaceConfiguration(any()))
                .thenThrow(new MenuItemVersionConflictException("Menu item version is stale"));

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":5,\"groups\":[]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void replace_missingMenuItem_returns404() throws Exception {
        when(service.replaceConfiguration(any()))
                .thenThrow(new MenuItemNotFoundException("Menu item was not found"));

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":5,\"groups\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private MenuItem savedMenuItem(Long id, Long version) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getId()).thenReturn(id);
        when(menuItem.getVersion()).thenReturn(version);
        return menuItem;
    }
}
