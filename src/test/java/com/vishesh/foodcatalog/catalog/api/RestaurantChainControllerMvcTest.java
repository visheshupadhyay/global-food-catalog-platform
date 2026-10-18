package com.vishesh.foodcatalog.catalog.api;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.DuplicateBusinessCodeException;
import com.vishesh.foodcatalog.catalog.application.RestaurantChainNotFoundException;
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
        RestaurantChain savedChain = persistedChain(42L,
                "dh-de",
                "Delivery Hero Germany");

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
                        """))
                .andExpect(status().isCreated())
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

    @Test
    void getByIdCompleteResponse() throws Exception {
        RestaurantChain restaurantChain = persistedChain(42L,
                "dh-de",
                "Delivery Hero Germany");

        when(service.findChainById(42L)).thenReturn(restaurantChain);
        mockMvc.perform(get("/api/v1/restaurant-chains/" + restaurantChain.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.businessCode").value("dh-de"))
                .andExpect(jsonPath("$.displayName").value("Delivery Hero Germany"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.status").value(ChainStatus.DRAFT.name()));

        verify(service).findChainById(42L);
    }

    @Test
    void getByBusinessCompleteResponse() throws Exception {
        RestaurantChain restaurantChain = persistedChain(42L,
                "dh-de",
                "Delivery Hero Germany");

        when(service.findChainByBusinessCode("dh-de")).thenReturn(restaurantChain);

        mockMvc.perform(get("/api/v1/restaurant-chains/by-business-code/" + restaurantChain.getBusinessCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.businessCode").value("dh-de"))
                .andExpect(jsonPath("$.displayName").value("Delivery Hero Germany"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.status").value(ChainStatus.DRAFT.name()));

        verify(service).findChainByBusinessCode("dh-de");
    }

    @Test
    void missingId() throws Exception {
        when(service.findChainById(anyLong()))
                .thenThrow(new RestaurantChainNotFoundException("Restaurant chain with ID: 901 not found"));

        mockMvc.perform(get("/api/v1/restaurant-chains/" + 901))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant chain with ID: 901 not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/" + 901));
    }

    @Test
    void missingBusiness() throws Exception {
        when(service.findChainByBusinessCode(anyString()))
                .thenThrow(new RestaurantChainNotFoundException(
                        "Restaurant chain with business code: " + "wrong-business-code" + " not found"));

        mockMvc.perform(get("/api/v1/restaurant-chains/by-business-code/" + "wrong-business-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant chain with business code: " + "wrong-business-code" + " not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/restaurant-chains/by-business-code/" + "wrong-business-code"));
    }

    private RestaurantChain persistedChain(Long id, String businessCode, String displayName) {
        return new RestaurantChain(
                businessCode,
                displayName,
                ChainStatus.DRAFT,
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                id);
    }
}
