package com.vishesh.foodcatalog.catalog.event.monitoring;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OutboxMetricsEndpointTest {
    private MockMvc mockMvc;

    @Mock
    private OutboxMetricsReader reader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new OutboxMetricsEndpoint(reader)).build();
    }

    @Test
    void getOutboxMetrics_returnsAllValues() throws Exception {
        when(reader.readMetrics()).thenReturn(new OutboxMetrics(2L, 1L, 50L, 3L, 8L, 300L));

        mockMvc.perform(get("/actuator/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending", is(2)))
                .andExpect(jsonPath("$.processing", is(1)))
                .andExpect(jsonPath("$.published", is(50)))
                .andExpect(jsonPath("$.dlq", is(3)))
                .andExpect(jsonPath("$.retryCount", is(8)))
                .andExpect(jsonPath("$.oldestPendingAgeSeconds", is(300)));
    }
}
