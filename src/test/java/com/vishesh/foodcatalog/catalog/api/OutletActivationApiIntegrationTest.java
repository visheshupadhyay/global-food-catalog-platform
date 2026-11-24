package com.vishesh.foodcatalog.catalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

/**
 * Proves the real HTTP onboarding path: draft outlet, assignment, activation,
 * and one durable initialization event in PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OutletActivationApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long chainId;
    private Long menuGroupId;
    private Long outletId;
    private Long sharedBurgerId;

    @AfterEach
    void cleanUp() {
        if (outletId != null) {
            jdbcTemplate.update("delete from outbox_event where aggregate_id = ?", outletId);
            jdbcTemplate.update("delete from outlet_override where outlet_id = ?", outletId);
            jdbcTemplate.update("delete from menu_group_outlet where outlet_id = ?", outletId);
        }
        if (sharedBurgerId != null) {
            jdbcTemplate.update("delete from outlet_override where base_menu_item_id = ?", sharedBurgerId);
            jdbcTemplate.update("delete from menu_item where id = ?", sharedBurgerId);
        }
        if (outletId != null) {
            jdbcTemplate.update("delete from outlet where id = ?", outletId);
        }
        if (menuGroupId != null) {
            jdbcTemplate.update("delete from menu_group where id = ?", menuGroupId);
        }
        if (chainId != null) {
            jdbcTemplate.update("delete from restaurant_chain where id = ?", chainId);
        }
    }

    @Test
    void createThenActivate_persistsAssignmentAndOneCompleteInitializationEvent() throws Exception {
        createActiveChainAndMenuGroup();
        createDraftOutletThroughApi();
        createSharedBurgerWithOutletPriceOverride();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/outlets/{outletId}/activation", outletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id")
                        .value(outletId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.version")
                        .value(1));

        assertEquals(ChainStatus.ACTIVE.name(), jdbcTemplate.queryForObject(
                "select status from outlet where id = ?", String.class, outletId));
        assertEquals(menuGroupId, jdbcTemplate.queryForObject(
                "select menu_group_id from menu_group_outlet where outlet_id = ?", Long.class, outletId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and event_type = ?",
                Integer.class, outletId, "OUTLET_MENU_INITIALIZED"));
        assertEquals(String.valueOf(outletId), jdbcTemplate.queryForObject(
                "select payload ->> 'outletId' from outbox_event where aggregate_id = ? and event_type = ?",
                String.class, outletId, "OUTLET_MENU_INITIALIZED"));
        BigDecimal resolvedPrice = jdbcTemplate.queryForObject(
                "select cast(payload #>> '{menuItems,0,price}' as numeric) from outbox_event "
                        + "where aggregate_id = ? and event_type = ?",
                BigDecimal.class, outletId, "OUTLET_MENU_INITIALIZED");
        assertEquals(0, BigDecimal.valueOf(219).compareTo(resolvedPrice));

        // An HTTP retry may carry the original version. The service returns the
        // active outlet before checking it and does not create a second event.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/outlets/{outletId}/activation", outletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("ACTIVE"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and event_type = ?",
                Integer.class, outletId, "OUTLET_MENU_INITIALIZED"));
    }

    @Test
    void activation_staleVersionReturns409AndLeavesOutletDraft() throws Exception {
        createActiveChainAndMenuGroup();
        createDraftOutletThroughApi();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/outlets/{outletId}/activation", outletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":7}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value(409));

        assertEquals(ChainStatus.DRAFT.name(), jdbcTemplate.queryForObject(
                "select status from outlet where id = ?", String.class, outletId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ?", Integer.class, outletId));
    }

    private void createActiveChainAndMenuGroup() {
        chainId = jdbcTemplate.queryForObject(
                "insert into restaurant_chain (display_name, business_code, status) values (?, ?, ?) returning id",
                Long.class, "Outlet Activation Chain", "OUTLET-ACTIVATION-" + UUID.randomUUID(),
                ChainStatus.ACTIVE.name());
        menuGroupId = jdbcTemplate.queryForObject(
                "insert into menu_group (display_name, restaurant_chain_id, status) values (?, ?, ?) returning id",
                Long.class, "Main Menu", chainId, ChainStatus.ACTIVE.name());
    }

    private void createDraftOutletThroughApi() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/restaurant-chains/{chainId}/outlets", chainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Indiranagar",
                          "outletCode": "OUTLET-%s",
                          "currencyCode": "INR",
                          "timezone": "Asia/Kolkata",
                          "menuGroupId": %d
                        }
                        """.formatted(UUID.randomUUID(), menuGroupId)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("DRAFT"));

        outletId = jdbcTemplate.queryForObject(
                "select id from outlet where restaurant_chain_id = ? order by id desc limit 1",
                Long.class, chainId);
    }

    private void createSharedBurgerWithOutletPriceOverride() {
        sharedBurgerId = jdbcTemplate.queryForObject(
                "insert into menu_item (item_name, item_price, created_at, menu_group_id, outlet_id, "
                        + "business_code, status, visibility, availability, version, restaurant_chain_id, updated_at) "
                        + "values (?, ?, current_timestamp, ?, null, ?, ?, true, true, 0, ?, current_timestamp) "
                        + "returning id",
                Long.class, "Burger", BigDecimal.valueOf(199), menuGroupId,
                "OUTLET-ACTIVATION-BURGER-" + UUID.randomUUID(), MenuItemStatus.ACTIVE.name(), chainId);
        jdbcTemplate.update(
                "insert into outlet_override (outlet_id, base_menu_item_id, override_price) values (?, ?, ?)",
                outletId, sharedBurgerId, BigDecimal.valueOf(219));
    }
}
