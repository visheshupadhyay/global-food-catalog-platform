package com.vishesh.foodcatalog.catalog.reconciliation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReconciliationEndpoint.class)
class ReconciliationEndpointMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconciliationStateReader stateReader;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Test
    void compare_matchingStates_returnsMatch() throws Exception {
        CatalogMenuItemState catalog = new CatalogMenuItemState(
                101L, 201L, new BigDecimal("12.00"), true, true, 5L);
        EffectiveMenuSnapshotState snapshot = new EffectiveMenuSnapshotState(
                101L, 201L, new BigDecimal("12.00"), true, true, 5L);
        when(stateReader.find(101L, 201L))
                .thenReturn(Optional.of(new ReconciliationStates(catalog, snapshot)));
        when(reconciliationService.compare(catalog, snapshot))
                .thenReturn(new ReconciliationResult(101L, 201L,
                        ReconciliationResultType.MATCH, 5L, 5L, "States match"));

        mockMvc.perform(get("/api/v1/reconciliation/menu-items/101/outlets/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("MATCH"))
                .andExpect(jsonPath("$.catalogVersion").value(5))
                .andExpect(jsonPath("$.effectiveVersion").value(5));
    }

    @Test
    void compare_whenStateIsMissing_returns400() throws Exception {
        when(stateReader.find(101L, 201L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/reconciliation/menu-items/101/outlets/201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compare_whenIdIsInvalid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reconciliation/menu-items/0/outlets/201"))
                .andExpect(status().isBadRequest());
    }
}
