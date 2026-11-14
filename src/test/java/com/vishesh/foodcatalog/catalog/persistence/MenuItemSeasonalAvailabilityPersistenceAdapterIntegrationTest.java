package com.vishesh.foodcatalog.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSeasonalAvailability;
import com.vishesh.foodcatalog.catalog.repository.MenuItemSeasonalAvailabilityPersistenceAdapter;

/**
 * Proves that PostgreSQL accepts replacing a seasonal rule for the same outlet.
 * The two transactions mirror two separate API requests.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MenuItemSeasonalAvailabilityPersistenceAdapterIntegrationTest {

    @Autowired
    private MenuItemSeasonalAvailabilityPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long chainId;
    private Long outletId;
    private Long menuItemId;

    @AfterEach
    void cleanUp() {
        if (menuItemId != null) {
            jdbcTemplate.update("delete from menu_item_seasonal_availability where menu_item_id = ?", menuItemId);
            jdbcTemplate.update("delete from menu_item where id = ?", menuItemId);
        }
        if (outletId != null) {
            jdbcTemplate.update("delete from outlet where id = ?", outletId);
        }
        if (chainId != null) {
            jdbcTemplate.update("delete from restaurant_chain where id = ?", chainId);
        }
    }

    @Test
    void replacingExistingOutletSeason_removesOldRuleAndIncrementsVersion() {
        createBurgerAtOutlet();

        transactionTemplate.executeWithoutResult(status -> {
            MenuItemSeasonalAvailability activeSeason = adapter.save(new MenuItemSeasonalAvailability(
                    menuItemId, outletId, LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 16), 0L));

            assertEquals(0L, activeSeason.getVersion());
        });

        MenuItemSeasonalAvailability expiredSeason = transactionTemplate.execute(status -> adapter.save(
                new MenuItemSeasonalAvailability(menuItemId, outletId,
                        LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 13), 0L)));

        assertEquals(1L, expiredSeason.getVersion());

        MenuItemSeasonalAvailability stored = adapter.find(menuItemId, outletId).orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 4), stored.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 13), stored.getEndDate());
        assertEquals(1L, stored.getVersion());

        Integer storedRuleCount = jdbcTemplate.queryForObject(
                "select count(*) from menu_item_seasonal_availability where menu_item_id = ? and outlet_id = ?",
                Integer.class, menuItemId, outletId);
        assertEquals(1, storedRuleCount);
    }

    private void createBurgerAtOutlet() {
        chainId = jdbcTemplate.queryForObject(
                "insert into restaurant_chain (display_name, business_code) values (?, ?) returning id",
                Long.class, "Seasonal Test Chain", "SEASON-CHAIN-" + UUID.randomUUID());
        outletId = jdbcTemplate.queryForObject(
                "insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
                        + "values (?, ?, ?, ?, ?) returning id",
                Long.class, "Seasonal Test Outlet", "EUR", "SEASON-OUTLET-" + UUID.randomUUID(),
                chainId, ChainStatus.DRAFT.name());
        menuItemId = jdbcTemplate.queryForObject(
                "insert into menu_item (item_name, item_price, created_at, outlet_id, business_code, status, "
                        + "visibility, availability, version, restaurant_chain_id, updated_at) "
                        + "values (?, ?, current_timestamp, ?, ?, ?, true, true, 0, ?, current_timestamp) returning id",
                Long.class, "Seasonal Test Burger", BigDecimal.TEN, outletId,
                "SEASON-ITEM-" + UUID.randomUUID(), "DRAFT", chainId);
    }
}
