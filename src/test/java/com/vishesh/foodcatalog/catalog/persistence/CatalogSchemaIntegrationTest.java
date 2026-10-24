package com.vishesh.foodcatalog.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.RestaurantChain;
import com.vishesh.foodcatalog.catalog.persistence.adapter.RestaurantChainPersistenceAdapter;
import com.vishesh.foodcatalog.catalog.persistence.entity.MenuGroupEntity;
import com.vishesh.foodcatalog.catalog.persistence.repository.MenuGroupJpaRepository;

import jakarta.persistence.EntityManager;

/**
 * Proves that a real PostgreSQL database can be built by the Flyway migrations.
 * It checks the tables, migration history, and important database constraints.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogSchemaIntegrationTest {

    private static final int EXPECTED_MIGRATION_COUNT = 7;
    private static final Long UNKNOWN_CHAIN_ID = 999999999L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String insertedBusinessCode;
    private Long insertedRestaurantChainId;
    private Long secondInsertedRestaurantChainId;
    private String secondInsertedBusinessCode;

    @Autowired
    private RestaurantChainPersistenceAdapter persistenceAdapter;

    @Autowired
    private MenuGroupJpaRepository menuGroupJpaRepository;

    @AfterEach
    void cleanUp() {
        deleteChainData(secondInsertedRestaurantChainId, secondInsertedBusinessCode);
        deleteChainData(insertedRestaurantChainId, insertedBusinessCode);

        secondInsertedRestaurantChainId = null;
        secondInsertedBusinessCode = null;
        insertedRestaurantChainId = null;
        insertedBusinessCode = null;
    }

    private void deleteChainData(Long restaurantChainId, String businessCode) {
        if (restaurantChainId == null && businessCode == null) {
            return;
        }

        if (restaurantChainId != null) {
            deleteOverridesForChain(restaurantChainId);
            jdbcTemplate.update("delete from menu_group_outlet where menu_group_id in "
                    + "(select id from menu_group where restaurant_chain_id = ?)",
                    restaurantChainId);
            jdbcTemplate.update("delete from menu_item where restaurant_chain_id = ?", restaurantChainId);
            jdbcTemplate.update("delete from outlet where restaurant_chain_id = ?", restaurantChainId);
            jdbcTemplate.update("delete from menu_group where restaurant_chain_id = ?", restaurantChainId);
        }

        if (businessCode != null) {
            jdbcTemplate.update("delete from restaurant_chain where business_code = ?", businessCode);
        }
    }

    private void deleteOverridesForChain(Long restaurantChainId) {
        jdbcTemplate.update(
                "delete from outlet_override where outlet_id in "
                        + "(select id from outlet where restaurant_chain_id = ?) "
                        + "or base_menu_item_id in "
                        + "(select id from menu_item where restaurant_chain_id = ?)",
                restaurantChainId, restaurantChainId);
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
        assertEquals(EXPECTED_MIGRATION_COUNT, successfulMigrations);
    }

    @Test
    void postgresRejectsDuplicateRestaurantChainBusinessCode() {
        // PostgreSQL must protect the uniqueness rule even if application code misses
        // it.
        insertedBusinessCode = "SCHEMA-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        DataAccessException exception = assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Duplicate Schema Test Chain", insertedBusinessCode));

        assertTrue(exception.getMessage().contains("restaurant_chain_business_code_uk"));
    }

    @Test
    void postgresRejectsDuplicateWorkingMenuGroupNameIgnoringCaseAndSpaces() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                " Burgers ", insertedRestaurantChainId, ChainStatus.DRAFT.name());

        DataAccessException exception = assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                "BURGERS", insertedRestaurantChainId, ChainStatus.ACTIVE.name()));

        assertTrue(exception.getMessage().contains("menu_group_normalized_display_name_uk"));
    }

    @Test
    void postgresAllowsNameReuseWhenPreviousGroupIsArchived() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                "Burgers", insertedRestaurantChainId, ChainStatus.ARCHIVED.name());

        jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                "BURGERS", insertedRestaurantChainId, ChainStatus.DRAFT.name());
    }

    @Test
    void postgresAllowsSameWorkingNameForDifferentChains() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();
        secondInsertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", secondInsertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        secondInsertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, secondInsertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                "Burgers", insertedRestaurantChainId, ChainStatus.ACTIVE.name());

        jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                "BURGERS", secondInsertedRestaurantChainId, ChainStatus.DRAFT.name());

    }

    @Test
    void postgresGeneratesNormalizedMenuGroupName() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group (display_name, restaurant_chain_id, status) values (?, ?, ?)",
                " Burgers ", insertedRestaurantChainId, ChainStatus.DRAFT.name());

        String normalizedDisplayName = jdbcTemplate.queryForObject(
                "select normalized_display_name from menu_group where restaurant_chain_id = ?",
                String.class, insertedRestaurantChainId);

        assertEquals("burgers", normalizedDisplayName);
    }

    @Test
    void menuGroupVersionColumnIsBigintAndNotNullable() {
        // The version column is the database's revision number for optimistic locking.
        Integer columnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = 'public' and table_name = 'menu_group' "
                        + "and column_name = 'version' and data_type = 'bigint' "
                        + "and is_nullable = 'NO'",
                Integer.class);

        assertEquals(1, columnCount);
    }

    @Test
    void newMenuGroupStartsWithVersionZero() {
        insertedBusinessCode = "MENU-GROUP-VERSION-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Version Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group (display_name, restaurant_chain_id, status) values (?, ?, ?)",
                "Burgers", insertedRestaurantChainId, ChainStatus.DRAFT.name());

        Long version = jdbcTemplate.queryForObject(
                "select version from menu_group where restaurant_chain_id = ?",
                Long.class, insertedRestaurantChainId);

        assertEquals(0L, version);
    }

    @Test
    void repositoryReturnsTrueForWorkingDraftMenuGroup() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        menuGroupJpaRepository.saveAndFlush(
                new MenuGroupEntity(insertedRestaurantChainId, " Burgers "));

        String normalizedName = "BURGERS".trim().toLowerCase(Locale.ROOT);
        boolean exists = menuGroupJpaRepository
                .existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                        insertedRestaurantChainId,
                        normalizedName,
                        List.of(ChainStatus.DRAFT, ChainStatus.ACTIVE));

        assertTrue(exists);
    }

    @Test
    void repositoryReturnsFalseWhenOnlyMatchingMenuGroupIsArchived() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group (display_name, restaurant_chain_id, status) values (?, ?, ?)",
                "Burgers", insertedRestaurantChainId, ChainStatus.ARCHIVED.name());

        String normalizedName = "BURGERS".trim().toLowerCase(Locale.ROOT);
        boolean exists = menuGroupJpaRepository
                .existsByRestaurantChainIdAndNormalizedDisplayNameAndStatusIn(
                        insertedRestaurantChainId,
                        normalizedName,
                        List.of(ChainStatus.DRAFT, ChainStatus.ACTIVE));

        assertFalse(exists);
    }

    @Test
    void restaurantChainRoundTripsThroughPostgres() {
        String businessCode = "INTEGRATION-DE";
        String displayName = "Integration Germany";
        insertedBusinessCode = businessCode;
        RestaurantChain savedRestaurantChain = persistenceAdapter.save(
                new RestaurantChain(businessCode, displayName));

        assertNotNull(savedRestaurantChain.getId());
        assertTrue(savedRestaurantChain.getId() > 0);

        Optional<RestaurantChain> restaurantChainById = persistenceAdapter
                .findById(savedRestaurantChain.getId());
        assertFalse(restaurantChainById.isEmpty());
        assertNotNull(restaurantChainById.get().getCreatedAt());
        assertNotNull(restaurantChainById.get().getUpdatedAt());
        assertEquals(displayName, restaurantChainById.get().getDisplayName());
        assertEquals(businessCode, restaurantChainById.get().getBusinessCode());
        assertNotNull(restaurantChainById.get().getId());
        assertEquals(ChainStatus.DRAFT, restaurantChainById.get().getStatus());

        Optional<RestaurantChain> restaurantChainByCode = persistenceAdapter
                .findByBusinessCode("INTEGRATION-DE");

        assertFalse(restaurantChainByCode.isEmpty());
        assertNotNull(restaurantChainByCode.get().getCreatedAt());
        assertNotNull(restaurantChainByCode.get().getUpdatedAt());
        assertEquals(displayName, restaurantChainByCode.get().getDisplayName());
        assertEquals(businessCode, restaurantChainByCode.get().getBusinessCode());
        assertNotNull(restaurantChainByCode.get().getId());
        assertEquals(ChainStatus.DRAFT, restaurantChainByCode.get().getStatus());
    }

    @Test
    void renameIncrementsVersion() {
        String businessCode = "VERSION-TEST-" + UUID.randomUUID();
        insertedBusinessCode = businessCode;
        String displayName = "Burger-House";
        RestaurantChain savedRestaurantChain = persistenceAdapter
                .save(new RestaurantChain(businessCode, displayName));
        insertedRestaurantChainId = savedRestaurantChain.getId();
        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(savedRestaurantChain.getId(), "Burger-House");
        MenuGroupEntity savedMenuGroupEntity = menuGroupJpaRepository.saveAndFlush(menuGroupEntity);
        assertEquals(0L, savedMenuGroupEntity.getVersion());

        savedMenuGroupEntity.rename("Burger-Land");

        MenuGroupEntity newSavedEntity = menuGroupJpaRepository.saveAndFlush(savedMenuGroupEntity);
        assertEquals("Burger-Land", newSavedEntity.getDisplayName());
        assertEquals(1L, newSavedEntity.getVersion());
    }

    @Test
    void renameTrimsDisplayName() {
        String businessCode = "VERSION-TEST-" + UUID.randomUUID();
        insertedBusinessCode = businessCode;
        RestaurantChain savedRestaurantChain = persistenceAdapter
                .save(new RestaurantChain(businessCode, "Burger-House"));
        insertedRestaurantChainId = savedRestaurantChain.getId();
        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(savedRestaurantChain.getId(), "Burger-House");
        MenuGroupEntity savedMenuGroupEntity = menuGroupJpaRepository.saveAndFlush(menuGroupEntity);
        assertEquals(0L, savedMenuGroupEntity.getVersion());
        savedMenuGroupEntity.rename(" Burger-Land ");
        MenuGroupEntity newSavedEntity = menuGroupJpaRepository.saveAndFlush(savedMenuGroupEntity);
        assertEquals("Burger-Land", savedMenuGroupEntity.getDisplayName());
        assertEquals(1L, newSavedEntity.getVersion());
    }

    @Test
    void renameRejectsBlankDisplayName() {
        String businessCode = "VERSION-TEST-" + UUID.randomUUID();
        insertedBusinessCode = businessCode;
        RestaurantChain savedRestaurantChain = persistenceAdapter
                .save(new RestaurantChain(businessCode, "Burger-House"));
        insertedRestaurantChainId = savedRestaurantChain.getId();
        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(savedRestaurantChain.getId(), "Burger-House");
        MenuGroupEntity savedMenuGroupEntity = menuGroupJpaRepository.saveAndFlush(menuGroupEntity);
        assertEquals(0L, savedMenuGroupEntity.getVersion());

        assertThrows(IllegalArgumentException.class, () -> savedMenuGroupEntity.rename(" "));
    }

    @Test
    void renameRejectsNullDisplayName() {
        String businessCode = "VERSION-TEST-" + UUID.randomUUID();
        insertedBusinessCode = businessCode;
        RestaurantChain savedRestaurantChain = persistenceAdapter
                .save(new RestaurantChain(businessCode, "Burger-House"));
        insertedRestaurantChainId = savedRestaurantChain.getId();
        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(savedRestaurantChain.getId(), "Burger-House");
        MenuGroupEntity savedMenuGroupEntity = menuGroupJpaRepository.saveAndFlush(menuGroupEntity);
        assertEquals(0L, savedMenuGroupEntity.getVersion());
        assertThrows(IllegalArgumentException.class, () -> savedMenuGroupEntity.rename(null));
    }

    @Test
    void optimisticLockRejectsStaleMenuGroupUpdate() {
        String businessCode = "VERSION-TEST-" + UUID.randomUUID();
        insertedBusinessCode = businessCode;
        RestaurantChain savedRestaurantChain = persistenceAdapter
                .save(new RestaurantChain(businessCode, "Burger-House"));
        insertedRestaurantChainId = savedRestaurantChain.getId();
        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(savedRestaurantChain.getId(), "Burger-House");

        MenuGroupEntity saved = menuGroupJpaRepository.saveAndFlush(menuGroupEntity);

        entityManager.clear();

        MenuGroupEntity copyA = menuGroupJpaRepository.findById(saved.getId()).orElseThrow();

        entityManager.clear();

        MenuGroupEntity copyB = menuGroupJpaRepository.findById(saved.getId()).orElseThrow();

        copyA.rename("Copy A Burger House");
        menuGroupJpaRepository.saveAndFlush(copyA);

        copyB.rename("Copy B Burger House");

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> menuGroupJpaRepository.saveAndFlush(copyB));

        assertNotSame(copyA, copyB);
        assertEquals(0L, copyA.getVersion());
        assertEquals(0L, copyB.getVersion());
    }

    @Test
    void findById_returnsEmptyWhenChainDoesNotExist() {
        assertTrue(persistenceAdapter.findById(UNKNOWN_CHAIN_ID).isEmpty());
    }

    @Test
    void findByBusinessCode_returnsEmptyWhenChainDoesNotExist() {
        assertTrue(persistenceAdapter.findByBusinessCode("DOES-NOT-EXIST").isEmpty());
    }

    @Test
    void postgresRejectsMenuItemWithBothOwners() {
        insertedBusinessCode = "MENU-ITEM-OWNER-TEST-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Menu Item Owner Test Chain", insertedBusinessCode);
        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        Long menuGroupId = jdbcTemplate.queryForObject(
                "insert into menu_group (display_name, restaurant_chain_id, status) "
                        + "values (?, ?, ?) returning id",
                Long.class, "Burgers", insertedRestaurantChainId, ChainStatus.DRAFT.name());
        Long outletId = jdbcTemplate.queryForObject(
                "insert into outlet (display_name,currency, outlet_code, restaurant_chain_id, status) "
                        + "values (?,?, ?, ?, ?) returning id",
                Long.class, "Berlin Mitte", "EUR", "BERLIN-" + UUID.randomUUID(),
                insertedRestaurantChainId, ChainStatus.DRAFT.name());

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update(
                        "insert into menu_item (item_name, item_price, created_at, "
                                + "menu_group_id, outlet_id, business_code, status, "
                                + "visibility, availability, version, restaurant_chain_id, updated_at) "
                                + "values (?, ?, current_timestamp, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)",
                        "Invalid Burger", BigDecimal.valueOf(5.99), menuGroupId, outletId,
                        "INVALID-BOTH-" + UUID.randomUUID(), "DRAFT", true, true, 0L,
                        insertedRestaurantChainId));

        assertTrue(exception.getMessage().contains("menu_item_single_owner_ck"));
    }

    @Test
    void postgresRejectsMenuItemWithNoOwner() {
        insertedBusinessCode = "MENU-ITEM-OWNER-TEST-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Menu Item Owner Test Chain", insertedBusinessCode);
        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update(
                        "insert into menu_item (item_name, item_price, created_at, "
                                + "menu_group_id, outlet_id, business_code, status, "
                                + "visibility, availability, version, restaurant_chain_id, updated_at) "
                                + "values (?, ?, current_timestamp, null, null, ?, ?, ?, ?, ?, ?, current_timestamp)",
                        "Orphan Burger", BigDecimal.valueOf(5.99),
                        "INVALID-NONE-" + UUID.randomUUID(), "DRAFT", true, true, 0L,
                        insertedRestaurantChainId));

        assertTrue(exception.getMessage().contains("menu_item_single_owner_ck"));
    }

    @Test
    void postgresAcceptsMenuItemOwnedByMenuGroup() {
        insertedBusinessCode = "SHARED-CHAIN-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Shared Item Chain", insertedBusinessCode);
        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        Long menuGroupId = jdbcTemplate.queryForObject(
                "insert into menu_group (display_name, restaurant_chain_id, status) "
                        + "values (?, ?, ?) returning id",
                Long.class, "Burgers", insertedRestaurantChainId, ChainStatus.DRAFT.name());

        String itemBusinessCode = "SHARED-ITEM-" + UUID.randomUUID();
        Long menuItemId = jdbcTemplate.queryForObject(
                "insert into menu_item (item_name, item_price, created_at, menu_group_id, "
                        + "outlet_id, business_code, status, visibility, availability, version, "
                        + "restaurant_chain_id, updated_at) "
                        + "values (?, ?, current_timestamp, ?, null, ?, ?, ?, ?, ?, ?, current_timestamp) "
                        + "returning id",
                Long.class, "Shared Burger", BigDecimal.valueOf(5.99), menuGroupId,
                itemBusinessCode, "DRAFT", true, true, 0L, insertedRestaurantChainId);

        var row = jdbcTemplate.queryForMap(
                "select menu_group_id, outlet_id, restaurant_chain_id, version "
                        + "from menu_item where id = ?",
                menuItemId);

        assertEquals(menuGroupId, ((Number) row.get("menu_group_id")).longValue());
        assertNull(row.get("outlet_id"));
        assertEquals(insertedRestaurantChainId,
                ((Number) row.get("restaurant_chain_id")).longValue());
        assertEquals(0L, ((Number) row.get("version")).longValue());
    }

    @Test
    void postgresAcceptsMenuItemOwnedByOutlet() {
        insertedBusinessCode = "LOCAL-CHAIN-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Local Item Chain", insertedBusinessCode);
        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        Long outletId = jdbcTemplate.queryForObject(
                "insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
                        + "values (?, ?, ?, ?, ?) returning id",
                Long.class, "Berlin Mitte", "EUR", "BERLIN-" + UUID.randomUUID(),
                insertedRestaurantChainId, ChainStatus.DRAFT.name());

        String itemBusinessCode = "LOCAL-ITEM-" + UUID.randomUUID();
        Long menuItemId = jdbcTemplate.queryForObject(
                "insert into menu_item (item_name, item_price, created_at, menu_group_id, "
                        + "outlet_id, business_code, status, visibility, availability, version, "
                        + "restaurant_chain_id, updated_at) "
                        + "values (?, ?, current_timestamp, null, ?, ?, ?, ?, ?, ?, ?, current_timestamp) "
                        + "returning id",
                Long.class, "Halloween Burger", BigDecimal.valueOf(6.49), outletId,
                itemBusinessCode, "DRAFT", true, true, 0L, insertedRestaurantChainId);

        var row = jdbcTemplate.queryForMap(
                "select menu_group_id, outlet_id, restaurant_chain_id, version "
                        + "from menu_item where id = ?",
                menuItemId);

        assertNull(row.get("menu_group_id"));
        assertEquals(outletId, ((Number) row.get("outlet_id")).longValue());
        assertEquals(insertedRestaurantChainId,
                ((Number) row.get("restaurant_chain_id")).longValue());
        assertEquals(0L, ((Number) row.get("version")).longValue());
    }

    @Test
    void postgresRejectsMenuGroupOwnedByAnotherChain() {
        insertedBusinessCode = "CHAIN-A-" + UUID.randomUUID();
        secondInsertedBusinessCode = "CHAIN-B-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "CHAIN-A", insertedBusinessCode);

        Long chainAId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);
        insertedRestaurantChainId = chainAId;

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "CHAIN-B", secondInsertedBusinessCode);

        Long chainBId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, secondInsertedBusinessCode);
        secondInsertedRestaurantChainId = chainBId;

        Long menuGroupId = jdbcTemplate.queryForObject(
                "insert into menu_group (display_name, restaurant_chain_id, status) "
                        + "values (?, ?, ?) returning id",
                Long.class, "Burgers", chainBId, ChainStatus.DRAFT.name());

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update(
                        "insert into menu_item (item_name, item_price, created_at, "
                                + "menu_group_id, outlet_id, business_code, status, "
                                + "visibility, availability, version, restaurant_chain_id, updated_at) "
                                + "values (?, ?, current_timestamp, ?, null, ?, ?, ?, ?, ?, ?, current_timestamp)",
                        "Invalid Burger", BigDecimal.valueOf(5.99), menuGroupId,
                        "INVALID-BOTH-" + UUID.randomUUID(), "DRAFT", true, true, 0L,
                        chainAId));

        assertTrue(exception.getMessage().contains("menu_item_menu_group_chain_fk"));
    }

    @Test
    void postgresRejectsOutletOwnedByAnotherChain() {
        insertedBusinessCode = "CHAIN-A-" + UUID.randomUUID();
        secondInsertedBusinessCode = "CHAIN-B-" + UUID.randomUUID();

        jdbcTemplate.update("insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "CHAIN-A", insertedBusinessCode);
        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?", Long.class,
                insertedBusinessCode);

        jdbcTemplate.update("insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "CHAIN-B", secondInsertedBusinessCode);
        secondInsertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?", Long.class,
                secondInsertedBusinessCode);

        Long outletId = jdbcTemplate.queryForObject(
                "insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
                        + "values (?, ?, ?, ?, ?) returning id",
                Long.class, "Berlin Mitte", "EUR", "BERLIN-" + UUID.randomUUID(),
                secondInsertedRestaurantChainId, "DRAFT");

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update(
                        "insert into menu_item (item_name, item_price, created_at, "
                                + "menu_group_id, outlet_id, business_code, status, visibility, "
                                + "availability, version, restaurant_chain_id, updated_at) "
                                + "values (?, ?, current_timestamp, null, ?, ?, ?, ?, ?, ?, ?, current_timestamp)",
                        "Invalid Local Burger", BigDecimal.valueOf(5.99), outletId,
                        "INVALID-OUTLET-" + UUID.randomUUID(), "DRAFT", true, true, 0L,
                        insertedRestaurantChainId));

        assertTrue(exception.getMessage().contains("menu_item_outlet_chain_fk"));
    }

    @Test
    void postgresAllowsVisibilityOnlyOutletOverride() {
        insertedBusinessCode = "MENU-GROUP-TEST-" + UUID.randomUUID();

        jdbcTemplate.update(
                "insert into restaurant_chain (display_name, business_code) values (?, ?)",
                "Schema Test Chain", insertedBusinessCode);

        insertedRestaurantChainId = jdbcTemplate.queryForObject(
                "select id from restaurant_chain where business_code = ?",
                Long.class, insertedBusinessCode);

        jdbcTemplate.update(
                "insert into menu_group ( display_name,restaurant_chain_id,status) values (?,?,?)",
                "Burgers", insertedRestaurantChainId, ChainStatus.DRAFT.name());

        Long menuGroupId = jdbcTemplate.queryForObject(
                "select id from menu_group where restaurant_chain_id = ?",
                Long.class, insertedRestaurantChainId);

        Long outletId = jdbcTemplate.queryForObject(
                "insert into outlet (display_name,currency, outlet_code, restaurant_chain_id, status) "
                        + "values (?,?, ?, ?, ?) returning id",
                Long.class, "Berlin Mitte", "EUR", "BERLIN-" + UUID.randomUUID(),
                insertedRestaurantChainId, ChainStatus.DRAFT.name());

        Long baseMenuItemId = jdbcTemplate.queryForObject(
                "insert into menu_item (item_name, item_price, created_at, "
                        + "menu_group_id, outlet_id, business_code, status, "
                        + "visibility, availability, version, restaurant_chain_id, updated_at) "
                        + "values (?, ?, current_timestamp, ?, null, ?, ?, ?, ?, ?, ?, current_timestamp) "
                        + "returning id",
                Long.class, "Shared Burger", BigDecimal.valueOf(5.99), menuGroupId,
                "VISIBILITY-ITEM-" + UUID.randomUUID(), MenuItemStatus.ACTIVE.name(), true, true, 0L,
                insertedRestaurantChainId);

        jdbcTemplate.update(
                "insert into outlet_override (outlet_id, base_menu_item_id, override_visible) values (?, ?, ?)",
                outletId, baseMenuItemId, false);

        Boolean overrideVisible = jdbcTemplate.queryForObject(
                "select override_visible from outlet_override where outlet_id = ? and base_menu_item_id = ?",
                Boolean.class, outletId, baseMenuItemId);

        assertEquals(Boolean.FALSE, overrideVisible);
    }
}
