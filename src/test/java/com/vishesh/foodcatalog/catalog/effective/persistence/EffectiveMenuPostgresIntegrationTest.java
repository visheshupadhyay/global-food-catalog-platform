package com.vishesh.foodcatalog.catalog.effective.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vishesh.foodcatalog.catalog.persistence.adapter.RestaurantChainPersistenceAdapter;
import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.effective.domain.EffectiveMenuSourceData;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EffectiveMenuPostgresIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String insertedBusinessCode;
    private Long insertedRestaurantChainId;
    private Long secondInsertedRestaurantChainId;
    private String secondInsertedBusinessCode;

    @Autowired
    private RestaurantChainPersistenceAdapter persistenceAdapter;

    @Autowired
    private PostgresEffectiveMenuDataProvider provider;

    @Test
    void provider_readsActiveSharedAndLocalItems() {
        Long chainId = createChain();
        Long outletId = createOutlet(chainId, "EUR");
        Long groupId = createMenuGroup(chainId, "Standard Menu", "ACTIVE");
        assignGroupToOutlet(groupId, outletId);
        Long sharedItemId = createSharedItem(chainId, groupId, "Classic Burger", "10.00");
        Long localItemId = createLocalItem(chainId, outletId, "Berlin Special", "9.00");

        EffectiveMenuSourceData source = provider.findByOutletId(outletId).orElseThrow();

        assertEquals(ChainStatus.ACTIVE, source.outletStatus());
        assertEquals(sharedItemId, source.baseItems().get(0).getMenuItemId());
        assertEquals(localItemId, source.localItems().get(0).getMenuItemId());
        assertTrue(source.overrides().isEmpty());
    }

    @Test
    void provider_hidesBaseItemsWhenAssignedGroupIsDraft() {
        Long chainId = createChain();
        Long outletId = createOutlet(chainId, "EUR");
        Long groupId = createMenuGroup(chainId, "Unfinished Menu", "DRAFT");
        assignGroupToOutlet(groupId, outletId);
        createSharedItem(chainId, groupId, "Hidden Base Burger", "10.00");
        Long localItemId = createLocalItem(chainId, outletId, "Berlin Special", "9.00");

        EffectiveMenuSourceData source = provider.findByOutletId(outletId).orElseThrow();

        assertTrue(source.baseItems().isEmpty());
        assertEquals(localItemId, source.localItems().get(0).getMenuItemId());
    }

    @Test
    void provider_readsSparseOutletOverride() {
        Long chainId = createChain();
        Long outletId = createOutlet(chainId, "EUR");
        Long groupId = createMenuGroup(chainId, "Standard Menu", "ACTIVE");
        assignGroupToOutlet(groupId, outletId);
        Long sharedItemId = createSharedItem(chainId, groupId, "Classic Burger", "10.00");

        jdbcTemplate.update("""
                INSERT INTO outlet_override
                    (outlet_id, base_menu_item_id, override_price)
                VALUES (?, ?, ?)
                """, outletId, sharedItemId, new BigDecimal("12.00"));

        EffectiveMenuSourceData source = provider.findByOutletId(outletId).orElseThrow();

        assertEquals(1, source.overrides().size());
        assertEquals(outletId, source.overrides().get(0).getOutletId());
        assertEquals(sharedItemId, source.overrides().get(0).getMenuItemId());
        assertEquals(new BigDecimal("12.00"), source.overrides().get(0).getPriceOverride());
        assertNull(source.overrides().get(0).getAvailableOverride());
        assertNull(source.overrides().get(0).getVisibleOverride());
    }

    @Test
    void provider_readsVisibilityOnlyOverride() {
        Long chainId = createChain();
        Long outletId = createOutlet(chainId, "EUR");
        Long groupId = createMenuGroup(chainId, "Standard Menu", "ACTIVE");
        assignGroupToOutlet(groupId, outletId);
        Long sharedItemId = createSharedItem(chainId, groupId, "Classic Burger", "10.00");

        jdbcTemplate.update("""
                INSERT INTO outlet_override
                    (outlet_id, base_menu_item_id, override_visible)
                VALUES (?, ?, ?)
                """, outletId, sharedItemId, false);

        EffectiveMenuSourceData source = provider.findByOutletId(outletId).orElseThrow();

        assertEquals(Boolean.FALSE, source.overrides().get(0).getVisibleOverride());
        assertNull(source.overrides().get(0).getPriceOverride());
        assertNull(source.overrides().get(0).getAvailableOverride());
    }

    @Test
    void provider_afterOverrideIsDeleted_returnsNoOverride() {
        // Removing the sparse row must make the provider return no outlet override.
        Long chainId = createChain();
        Long outletId = createOutlet(chainId, "EUR");
        Long groupId = createMenuGroup(chainId, "Standard Menu", "ACTIVE");
        assignGroupToOutlet(groupId, outletId);
        Long sharedItemId = createSharedItem(chainId, groupId, "Classic Burger", "10.00");

        jdbcTemplate.update("""
                INSERT INTO outlet_override
                    (outlet_id, base_menu_item_id, override_price)
                VALUES (?, ?, ?)
                """, outletId, sharedItemId, new BigDecimal("12.00"));

        EffectiveMenuSourceData beforeRemoval = provider.findByOutletId(outletId).orElseThrow();
        assertEquals(1, beforeRemoval.overrides().size());
        assertEquals(new BigDecimal("12.00"),
                beforeRemoval.overrides().get(0).getPriceOverride());

        jdbcTemplate.update("""
                DELETE FROM outlet_override
                WHERE outlet_id = ? AND base_menu_item_id = ?
                """, outletId, sharedItemId);

        EffectiveMenuSourceData afterRemoval = provider.findByOutletId(outletId).orElseThrow();
        assertTrue(afterRemoval.overrides().isEmpty());
    }

    @Test
    void provider_returnsEmptyForUnknownOutlet() {
        Optional<EffectiveMenuSourceData> result = provider.findByOutletId(999999999L);

        assertTrue(result.isEmpty());
    }

    private Long createChain() {
        String businessCode = "EFFECTIVE-" + UUID.randomUUID();
        RestaurantChain chain = persistenceAdapter.save(new RestaurantChain(businessCode, "Burger House"));
        insertedRestaurantChainId = chain.getId();
        insertedBusinessCode = businessCode;
        return chain.getId();
    }

    private Long createOutlet(Long chainId, String currency) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO outlet
                    (display_name, outlet_code, restaurant_chain_id, currency, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                RETURNING id
                """, Long.class, "Berlin Mitte", "BERLIN-" + UUID.randomUUID(), chainId, currency);
    }

    private Long createMenuGroup(Long chainId, String displayName, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO menu_group
                    (display_name, restaurant_chain_id, status)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, displayName, chainId, status);
    }

    private void assignGroupToOutlet(Long groupId, Long outletId) {
        jdbcTemplate.update("""
                INSERT INTO menu_group_outlet (menu_group_id, outlet_id)
                VALUES (?, ?)
                """, groupId, outletId);
    }

    private Long createSharedItem(Long chainId, Long groupId, String name, String price) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO menu_item
                    (item_name, item_price, created_at, menu_group_id,
                     business_code, status, visibility, availability,
                     version, restaurant_chain_id)
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, 'ACTIVE', true, true, 0, ?)
                RETURNING id
                """, Long.class, name, new BigDecimal(price), groupId,
                "BASE-" + UUID.randomUUID(), chainId);
    }

    private Long createLocalItem(Long chainId, Long outletId, String name, String price) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO menu_item
                    (item_name, item_price, created_at, outlet_id,
                     business_code, status, visibility, availability,
                     version, restaurant_chain_id)
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, 'ACTIVE', true, true, 0, ?)
                RETURNING id
                """, Long.class, name, new BigDecimal(price), outletId,
                "LOCAL-" + UUID.randomUUID(), chainId);
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (secondInsertedRestaurantChainId != null) {
            deleteOverridesForChain(secondInsertedRestaurantChainId);
            jdbcTemplate.update("delete from menu_item where restaurant_chain_id = ?",
                    secondInsertedRestaurantChainId);
            jdbcTemplate.update("delete from menu_group_outlet where menu_group_id in "
                    + "(select id from menu_group where restaurant_chain_id = ?)",
                    secondInsertedRestaurantChainId);
            jdbcTemplate.update("delete from outlet where restaurant_chain_id = ?",
                    secondInsertedRestaurantChainId);
            jdbcTemplate.update("delete from menu_group where restaurant_chain_id = ?",
                    secondInsertedRestaurantChainId);
            secondInsertedRestaurantChainId = null;
        }

        if (secondInsertedBusinessCode != null) {
            jdbcTemplate.update("delete from restaurant_chain where business_code = ?",
                    secondInsertedBusinessCode);
            secondInsertedBusinessCode = null;
        }
        if (insertedRestaurantChainId != null) {
            deleteOverridesForChain(insertedRestaurantChainId);
            jdbcTemplate.update("delete from menu_item where restaurant_chain_id = ?",
                    insertedRestaurantChainId);
            jdbcTemplate.update("delete from menu_group_outlet where menu_group_id in "
                    + "(select id from menu_group where restaurant_chain_id = ?)",
                    insertedRestaurantChainId);
            jdbcTemplate.update("delete from outlet where restaurant_chain_id = ?",
                    insertedRestaurantChainId);
            jdbcTemplate.update("delete from menu_group where restaurant_chain_id = ?",
                    insertedRestaurantChainId);
            insertedRestaurantChainId = null;
        }
        if (insertedBusinessCode != null) {
            jdbcTemplate.update("delete from restaurant_chain where business_code = ?",
                    insertedBusinessCode);
            insertedBusinessCode = null;
        }
    }

    private void deleteOverridesForChain(Long restaurantChainId) throws Exception {
        jdbcTemplate.update(
                "delete from outlet_override where outlet_id in "
                        + "(select id from outlet where restaurant_chain_id = ?) "
                        + "or base_menu_item_id in "
                        + "(select id from menu_item where restaurant_chain_id = ?)",
                restaurantChainId, restaurantChainId);
    }
}
