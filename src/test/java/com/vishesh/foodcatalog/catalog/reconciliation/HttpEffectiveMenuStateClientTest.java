package com.vishesh.foodcatalog.catalog.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpEffectiveMenuStateClientTest {
    private MockRestServiceServer server;
    private HttpEffectiveMenuStateClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpEffectiveMenuStateClient(builder, "http://effective-menu.test");
    }

    @Test
    void validResponse_isMappedToSnapshotState() {
        server.expect(requestTo("http://effective-menu.test/internal/effective-menu/items/101/outlets/201"))
                .andRespond(withSuccess("""
                            {
                                "menuItemId":101,
                                "outletId":201,
                                "price":10.00,
                                "availability":true,
                                "visibility":true,
                                "version":1
                            }
                        """, MediaType.APPLICATION_JSON));

        EffectiveMenuSnapshotState state = client.findSnapshotState(101L, 201L).orElseThrow();

        assertEquals(101L, state.getMenuItemId());
        assertEquals(201L, state.getOutletId());
        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(state.getPrice()));
        assertEquals(1L, state.getVersion());
        server.verify();
    }

    @Test
    void notFoundResponse_returnsEmpty() {
        server.expect(requestTo("http://effective-menu.test/internal/effective-menu/items/101/outlets/201"))
                .andRespond(withResourceNotFound());

        assertTrue(client.findSnapshotState(101L, 201L).isEmpty());
        server.verify();
    }

    @Test
    void serviceUnavailableResponse_returnsEmptyBecauseSnapshotIsNotReady() {
        server.expect(requestTo("http://effective-menu.test/internal/effective-menu/items/101/outlets/201"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertTrue(client.findSnapshotState(101L, 201L).isEmpty());
        server.verify();
    }

    @Test
    void invalidIds_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> client.findSnapshotState(null, 201L));
        assertThrows(IllegalArgumentException.class, () -> client.findSnapshotState(0L, 201L));
        assertThrows(IllegalArgumentException.class, () -> client.findSnapshotState(101L, null));
        assertThrows(IllegalArgumentException.class, () -> client.findSnapshotState(101L, 0L));
    }
}
