package com.vishesh.foodcatalog.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.BundleCompositionResult;
import com.vishesh.foodcatalog.catalog.application.BundleCompositionService;
import com.vishesh.foodcatalog.catalog.application.MenuItemNotFoundException;
import com.vishesh.foodcatalog.catalog.application.MenuItemVersionConflictException;
import com.vishesh.foodcatalog.catalog.domain.BundleChild;

/** Verifies the public bundle-replacement HTTP contract and DTO mapping. */
@WebMvcTest(BundleCompositionController.class)
class BundleCompositionControllerMvcTest {
    private static final String URL = "/api/v1/restaurant-chains/1/menu-items/20/bundle-children";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BundleCompositionService service;

    @Test
    void get_returnsCurrentChildrenAndVersion() throws Exception {
        BundleCompositionResult result = new BundleCompositionResult(20L, 6L, List.of(
                new BundleChild(101L, 20L, 10L, 1, 0)));
        when(service.getComposition(20L, 1L)).thenReturn(result);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleMenuItemId").value(20))
                .andExpect(jsonPath("$.version").value(6))
                .andExpect(jsonPath("$.children[0].childMenuItemId").value(10));

        verify(service).getComposition(20L, 1L);
    }

    @Test
    void replace_mapsCompleteChildListAndReturnsNextVersion() throws Exception {
        BundleCompositionResult result = new BundleCompositionResult(20L, 6L, List.of(
                new BundleChild(101L, 20L, 10L, 1, 0),
                new BundleChild(102L, 20L, 11L, 2, 1)));
        when(service.replaceChildrenWithResult(any(), any(), any(), any())).thenReturn(result);

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "expectedVersion": 5,
                          "children": [
                            {"childMenuItemId": 10, "quantity": 1, "displayOrder": 0},
                            {"childMenuItemId": 11, "quantity": 2, "displayOrder": 1}
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleMenuItemId").value(20))
                .andExpect(jsonPath("$.version").value(6))
                .andExpect(jsonPath("$.children[0].childMenuItemId").value(10))
                .andExpect(jsonPath("$.children[1].quantity").value(2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BundleChild>> childrenCaptor = ArgumentCaptor.forClass(List.class);
        verify(service).replaceChildrenWithResult(
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(5L),
                childrenCaptor.capture());
        List<BundleChild> children = childrenCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(List.of(10L, 11L),
                children.stream().map(BundleChild::getChildMenuItemId).toList());
        org.junit.jupiter.api.Assertions.assertEquals(2, children.get(1).getQuantity());
    }

    @Test
    void replace_missingExpectedVersion_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"children\":[{\"childMenuItemId\":10,\"quantity\":1,\"displayOrder\":0}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void replace_emptyChildren_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":5,\"children\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void replace_invalidChildQuantity_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"expectedVersion":5,"children":[
                          {"childMenuItemId":10,"quantity":0,"displayOrder":0}
                        ]}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void replace_staleVersion_returns409() throws Exception {
        when(service.replaceChildrenWithResult(any(), any(), any(), any()))
                .thenThrow(new MenuItemVersionConflictException("Bundle version is stale"));

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":5,\"children\":[{\"childMenuItemId\":10,\"quantity\":1,\"displayOrder\":0}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void replace_missingBundle_returns404() throws Exception {
        when(service.replaceChildrenWithResult(any(), any(), any(), any()))
                .thenThrow(new MenuItemNotFoundException("Bundle was not found"));

        mockMvc.perform(put(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":5,\"children\":[{\"childMenuItemId\":10,\"quantity\":1,\"displayOrder\":0}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void get_missingBundle_returns404() throws Exception {
        when(service.getComposition(20L, 1L))
                .thenThrow(new MenuItemNotFoundException("Bundle was not found"));

        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
