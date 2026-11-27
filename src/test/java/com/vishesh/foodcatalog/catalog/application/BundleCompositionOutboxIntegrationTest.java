package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

/**
 * Proves a real PostgreSQL transaction persists both a changed bundle tree and
 * the durable event which the existing outbox worker will later publish.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BundleCompositionOutboxIntegrationTest {
	@Autowired
	private BundleCompositionService service;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long chainId;
	private Long outletId;
	private Long burgerMealId;
	private Long burgerId;
	private Long friesId;

	@AfterEach
	void cleanUp() {
		if (burgerMealId != null) {
			jdbcTemplate.update("delete from outbox_event where aggregate_id = ?", burgerMealId);
			jdbcTemplate.update("delete from bundle_child where bundle_menu_item_id = ?", burgerMealId);
		}
		if (burgerId != null || friesId != null) {
			jdbcTemplate.update("delete from bundle_child where child_menu_item_id in (?, ?)", burgerId, friesId);
		}
		deleteMenuItem(burgerMealId);
		deleteMenuItem(burgerId);
		deleteMenuItem(friesId);
		if (outletId != null) {
			jdbcTemplate.update("delete from outlet where id = ?", outletId);
		}
		if (chainId != null) {
			jdbcTemplate.update("delete from restaurant_chain where id = ?", chainId);
		}
	}

	@Test
	void replaceChildren_persistsRelationshipsAndOneBundleOutboxEventTogether() {
		createOutletAndItems();

		service.replaceChildren(burgerMealId, chainId, 0L,
				List.of(new BundleChild(burgerMealId, burgerId, 1, 0), new BundleChild(burgerMealId, friesId, 1, 1)));

		Integer childCount = jdbcTemplate.queryForObject(
				"select count(*) from bundle_child where bundle_menu_item_id = ?", Integer.class, burgerMealId);
		assertEquals(2, childCount);

		// Ask PostgreSQL about JSON fields instead of comparing its formatted
		// jsonb text. PostgreSQL may insert spaces while rendering JSON text.
		String bundleChanged = jdbcTemplate.queryForObject(
				"select payload ->> 'bundleConfigurationChanged' from outbox_event "
						+ "where aggregate_id = ? and event_type = ?",
				String.class, burgerMealId, "BUNDLE_CONFIGURATION_CHANGED");
		assertEquals("true", bundleChanged);

		String snapshotMenuItemId = jdbcTemplate.queryForObject(
				"select payload #>> '{bundleSnapshot,menuItemId}' from outbox_event "
						+ "where aggregate_id = ? and event_type = ?",
				String.class, burgerMealId, "BUNDLE_CONFIGURATION_CHANGED");
		assertEquals(String.valueOf(burgerMealId), snapshotMenuItemId);

		Integer friesPresentInSnapshot = jdbcTemplate.queryForObject(
				"select count(*) from outbox_event where aggregate_id = ? and event_type = ? "
						+ "and payload @> cast(? as jsonb)",
				Integer.class, burgerMealId, "BUNDLE_CONFIGURATION_CHANGED",
				"{\"bundleSnapshot\":{\"children\":[{\"menuItemId\":" + friesId + "}]}}");
		assertEquals(1, friesPresentInSnapshot);
	}

	private void createOutletAndItems() {
		chainId = jdbcTemplate.queryForObject(
				"insert into restaurant_chain (display_name, business_code) values (?, ?) returning id", Long.class,
				"Bundle Outbox Chain", "BUNDLE-OUTBOX-" + UUID.randomUUID());
		outletId = jdbcTemplate.queryForObject(
				"insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
						+ "values (?, ?, ?, ?, ?) returning id",
				Long.class, "Bundle Outbox Outlet", "EUR", "BUNDLE-OUTLET-" + UUID.randomUUID(), chainId,
				ChainStatus.DRAFT.name());
		burgerMealId = insertMenuItem("Burger Meal", "BUNDLE");
		burgerId = insertMenuItem("Burger", "PRODUCT");
		friesId = insertMenuItem("Fries", "PRODUCT");
	}

	private Long insertMenuItem(String itemName, String kind) {
		return jdbcTemplate.queryForObject(
				"insert into menu_item (item_name, item_price, created_at, outlet_id, business_code, status, "
						+ "visibility, availability, version, restaurant_chain_id, updated_at, menu_item_kind) "
						+ "values (?, ?, current_timestamp, ?, ?, ?, true, true, 0, ?, current_timestamp, ?) "
						+ "returning id",
				Long.class, itemName, BigDecimal.TEN, outletId, "BUNDLE-ITEM-" + UUID.randomUUID(), "DRAFT", chainId,
				kind);
	}

	private void deleteMenuItem(Long menuItemId) {
		if (menuItemId != null) {
			jdbcTemplate.update("delete from menu_item where id = ?", menuItemId);
		}
	}
}
