package com.vishesh.foodcatalog.catalog.api;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.DuplicateBusinessCodeException;
import com.vishesh.foodcatalog.catalog.application.RestaurantChainService;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;

@WebMvcTest(RestaurantChainController.class)
public class RestaurantChainControllerMvcTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RestaurantChainService service;

    @Test
    void successfulCreation_returns201WithLocation() throws Exception {
        // Pretend PostgreSQL has saved the chain and assigned ID 42.
        RestaurantChain savedChain = new RestaurantChain(
                "DH-DE",
                "Delivery Hero Germany",
                ChainStatus.DRAFT,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                42L);

        when(service.createChain(anyString(), anyString()))
                .thenReturn(savedChain);

        // Send a real-looking HTTP request and check the public API response.
        mockMvc.perform(post("/api/v1/restaurant-chains")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "businessCode": "dh-de",
                                "displayName": "Delivery Hero Germany"
                            }
                        """)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value(ChainStatus.DRAFT.name()))
                .andExpect(header().string(
                        "Location",
                        "/api/v1/restaurant-chains/42"));
    }

    @Test
    void blankBusinessCode_returns400() throws Exception {
        // Validation must reject this request before the service is called.
        mockMvc.perform(post("/api/v1/restaurant-chains")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "businessCode": "",
                                "displayName": "Delivery Hero Germany"
                            }
                        """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains"));
        verifyNoInteractions(service);
    }

    @Test
    void blankDisplayName_returns400() throws Exception {
        // A display name is required for a customer-friendly chain record.
        mockMvc.perform(post("/api/v1/restaurant-chains")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "businessCode": "dh-de",
                                "displayName": ""
                            }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains"));
        verifyNoInteractions(service);
    }

    @Test
    void duplicateBusinessCode_returns409() throws Exception {
        // Pretend the service found an existing chain with the same business code.
        when(service.createChain(anyString(), anyString()))
                .thenThrow(new DuplicateBusinessCodeException(
                        "Restaurant Chain already exists!"));

        mockMvc.perform(post("/api/v1/restaurant-chains")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "businessCode": "dh-de",
                                "displayName": "Delivery Hero Germany"
                            }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant Chain already exists!"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains"));
    }
}
