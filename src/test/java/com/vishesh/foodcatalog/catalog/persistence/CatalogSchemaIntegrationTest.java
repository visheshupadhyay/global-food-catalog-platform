package com.vishesh.foodcatalog.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
        if (secondInsertedRestaurantChainId != null) {
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
        assertEquals(5, successfulMigrations);
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
    public void renameIncrementsVersion() {
        String businessCode = "VERSION-TEST-" + UUID.randomUUID();
        insertedBusinessCode = businessCode;
        String displayName = "Burger-House";
        RestaurantChain savedRestaurantChain = persistenceAdapter
                .save(new RestaurantChain(businessCode, displayName));
        insertedRestaurantChainId = savedRestaurantChain.getId();
        MenuGroupEntity menuGroupEntity = new MenuGroupEntity(savedRestaurantChain.getId(), "Burger-House");
        MenuGroupEntity savedMenuGroupEntity = menuGroupJpaRepository.saveAndFlush(menuGroupEntity);
        assertEquals(0L, savedMenuGroupEntity.getVersion());
        Instant oldUpdatedAt = savedMenuGroupEntity.getUpdatedAt();

        savedMenuGroupEntity.rename("Burger-Land");

        assertTrue(savedMenuGroupEntity.getUpdatedAt().isAfter(oldUpdatedAt));

        MenuGroupEntity newSavedEntity = menuGroupJpaRepository.saveAndFlush(savedMenuGroupEntity);
        assertEquals("Burger-Land", newSavedEntity.getDisplayName());
        assertEquals(1L, newSavedEntity.getVersion());
    }

    @Test
    public void renameTrimsDisplayName() {
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
    public void renameRejectsBlankDisplayName() {
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
    public void renameRejectsNullDisplayName() {
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
    public void optimisticLockRejectsStaleMenuGroupUpdate() {
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
        assertTrue(persistenceAdapter.findById(999999999L).isEmpty());
    }

    @Test
    void findByBusinessCode_returnsEmptyWhenChainDoesNotExist() {
        assertTrue(persistenceAdapter.findByBusinessCode("DOES-NOT-EXIST").isEmpty());
    }
}
