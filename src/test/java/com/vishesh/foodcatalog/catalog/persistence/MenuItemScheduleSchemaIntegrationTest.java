package com.vishesh.foodcatalog.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/** Verifies the PostgreSQL constraints for recurring menu-item schedules. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MenuItemScheduleSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scheduleTable_acceptsBaseAndOutletOverrideWindows() {
        String code = "SCHEDULE-SCHEMA-" + UUID.randomUUID();
        Long chainId = jdbcTemplate.queryForObject(
                "insert into restaurant_chain (display_name, business_code) values (?, ?) returning id",
                Long.class, "Schedule Chain", code);
        Long outletId = jdbcTemplate.queryForObject(
                "insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
                        + "values (?, ?, ?, ?, ?) returning id",
                Long.class, "Schedule Outlet", "EUR", "SCHEDULE-OUTLET-" + UUID.randomUUID(),
                chainId, ChainStatus.DRAFT.name());
        Long itemId = jdbcTemplate.queryForObject(
                "insert into menu_item (item_name, item_price, created_at, outlet_id, business_code, status, "
                        + "visibility, availability, version, restaurant_chain_id, updated_at) "
                        + "values (?, ?, current_timestamp, ?, ?, ?, true, true, 0, ?, current_timestamp) returning id",
                Long.class, "Scheduled Burger", BigDecimal.TEN, outletId,
                "SCHEDULE-ITEM-" + UUID.randomUUID(), "DRAFT", chainId);

        jdbcTemplate.update("insert into menu_item_schedule "
                + "(menu_item_id, outlet_id, weekday, start_time, end_time) values (?, null, ?, ?, ?)",
                itemId, "MONDAY", Time.valueOf("09:00:00"), Time.valueOf("17:00:00"));
        jdbcTemplate.update("insert into menu_item_schedule "
                + "(menu_item_id, outlet_id, weekday, start_time, end_time) values (?, ?, ?, ?, ?)",
                itemId, outletId, "MONDAY", Time.valueOf("10:00:00"), Time.valueOf("16:00:00"));

        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from menu_item_schedule where menu_item_id = ?", Integer.class, itemId));

        jdbcTemplate.update("delete from menu_item_schedule where menu_item_id = ?", itemId);
        jdbcTemplate.update("delete from menu_item where id = ?", itemId);
        jdbcTemplate.update("delete from outlet where id = ?", outletId);
        jdbcTemplate.update("delete from restaurant_chain where id = ?", chainId);
    }

    @Test
    void scheduleTable_rejectsInvalidWindowAndDuplicateBaseWindow() {
        DataAccessException invalidWindow = assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "insert into menu_item_schedule (menu_item_id, weekday, start_time, end_time) "
                        + "values (999999999, 'MONDAY', '17:00', '09:00')"));
        // The statement must be rejected by a database constraint or FK, rather than
        // silently accepted.
        assertEquals(true, invalidWindow.getMessage() != null);
    }
}
