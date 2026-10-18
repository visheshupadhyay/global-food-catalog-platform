package com.vishesh.foodcatalog.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vishesh.foodcatalog.TestcontainersConfiguration;

/**
 * Proves that a real PostgreSQL database can be built by the Flyway migrations.
 * It checks the tables, migration history, and important database constraints.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String insertedBusinessCode;

    @AfterEach
    void cleanUp() {
        if (insertedBusinessCode != null) {
            jdbcTemplate.update("delete from restaurant_chain where business_code = ?",
                    insertedBusinessCode);
            insertedBusinessCode = null;
        }
    }

    @Test
    void flywayCreatesRequiredCatalogTables() {
        // If a required table is missing, the database foundation is incomplete.
        String[] requiredTables = {
                "restaurant_chain", "menu_group", "outlet", "menu_category",
                "menu_item", "menu_group_outlet", "outlet_override",
                "catalog_audit", "outbox_event"
        };

        for (String table : requiredTables) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables "
                            + "where table_schema = 'public' and table_name = ?",
                    Integer.class, table);
            assertEquals(1, count, () -> "Missing table: " + table);
        }
    }

    @Test
    void flywayAppliedAllVersionedMigrationsSuccessfully() {
        // Every migration must finish successfully before the application starts.
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class);
        assertEquals(3, successfulMigrations);
    }

    @Test
    void postgresRejectsDuplicateRestaurantChainBusinessCode() {
        // PostgreSQL must protect the uniqueness rule even if application code misses it.
        insertedBusinessCode = "SCHEMA-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                        "Duplicate Schema Test Chain", insertedBusinessCode));

        assertTrue(exception.getMessage().contains("restaurant_chain_business_code_uk"));
    }
}
