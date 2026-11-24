package com.vishesh.foodcatalog.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.CreateOutletCommand;
import com.vishesh.foodcatalog.catalog.application.OutletService;
import com.vishesh.foodcatalog.catalog.application.OutletNotFoundException;
import com.vishesh.foodcatalog.catalog.application.OutletVersionConflictException;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.Outlet;

/** Verifies the small HTTP contracts around creating and activating outlets. */
@WebMvcTest(OutletController.class)
class OutletControllerMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutletService service;

    @Test
    void create_returnsDraftOutletAndLocation() throws Exception {
        when(service.createDraftOutlet(any(), any())).thenReturn(outlet(ChainStatus.DRAFT, 0L));

        mockMvc.perform(post("/api/v1/restaurant-chains/10/outlets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Indiranagar",
                          "outletCode": "BLR-IND-01",
                          "currencyCode": "INR",
                          "timezone": "Asia/Kolkata",
                          "menuGroupId": 20
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/restaurant-chains/10/outlets/30"))
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0));

        verify(service).createDraftOutlet(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(new CreateOutletCommand(
                        "Indiranagar", "BLR-IND-01", "INR", "Asia/Kolkata", 20L)));
    }

    @Test
    void activate_returnsActiveOutletWithTheNextVersion() throws Exception {
        when(service.activateOutlet(30L, 3L)).thenReturn(outlet(ChainStatus.ACTIVE, 4L));

        mockMvc.perform(post("/api/v1/outlets/30/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(4));

        verify(service).activateOutlet(30L, 3L);
    }

    @Test
    void activate_staleVersion_returns409() throws Exception {
        when(service.activateOutlet(30L, 2L))
                .thenThrow(new OutletVersionConflictException("Outlet version is stale"));

        mockMvc.perform(post("/api/v1/outlets/30/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void activate_missingOutlet_returns404() throws Exception {
        when(service.activateOutlet(999L, 0L))
                .thenThrow(new OutletNotFoundException("Outlet with ID: 999 not found"));

        mockMvc.perform(post("/api/v1/outlets/999/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void activate_whenAssignedGroupIsNotActive_returns409UsingTheExistingStateConvention() throws Exception {
        when(service.activateOutlet(30L, 0L))
                .thenThrow(new IllegalStateException("Assigned menu group must be ACTIVE before outlet activation"));

        mockMvc.perform(post("/api/v1/outlets/30/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void activate_whenAlreadyActive_remainsSuccessful() throws Exception {
        // The service makes this request idempotent. HTTP simply returns the
        // already-active representation rather than treating a retry as an error.
        when(service.activateOutlet(30L, 0L)).thenReturn(outlet(ChainStatus.ACTIVE, 4L));

        mockMvc.perform(post("/api/v1/outlets/30/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(4));

        verify(service).activateOutlet(30L, 0L);
    }

    @Test
    void activate_missingExpectedVersion_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/outlets/30/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoMoreInteractions(service);
    }

    private static Outlet outlet(ChainStatus status, Long version) {
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        return new Outlet(30L, 10L, "Indiranagar", "BLR-IND-01", Currency.getInstance("INR"),
                status, now, now, "Asia/Kolkata", version);
    }
}
