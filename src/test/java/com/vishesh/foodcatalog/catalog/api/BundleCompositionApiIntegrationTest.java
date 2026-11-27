package com.vishesh.foodcatalog.catalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * Proves the real HTTP API reaches PostgreSQL and creates the corresponding
 * durable bundle outbox event in one transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BundleCompositionApiIntegrationTest {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long chainId;
	private Long outletId;
	private Long bundleId;
	private Long burgerId;
	private Long friesId;

	@AfterEach
	void cleanUp() {
		if (bundleId != null) {
			jdbcTemplate.update("delete from outbox_event where aggregate_id = ?", bundleId);
			jdbcTemplate.update("delete from bundle_child where bundle_menu_item_id = ?", bundleId);
		}
		if (burgerId != null || friesId != null) {
			jdbcTemplate.update("delete from bundle_child where child_menu_item_id in (?, ?)", burgerId, friesId);
		}
		deleteMenuItem(bundleId);
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
	void replaceChildren_throughHttpApi_persistsRelationshipsAndBundleOutboxEvent() throws Exception {
		createOutletAndItems();

		// A real administrator creates the meal root as BUNDLE before
		// composing it. This proves the kind survives the complete HTTP/JPA
		// path, not merely a controller mock.
		mockMvc.perform(post("/api/v1/restaurant-chains/{chainId}/outlets/{outletId}/menu-items", chainId, outletId)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "businessCode": "BURGER-MEAL-API",
						  "itemName": "Burger Meal",
						  "itemPrice": 299,
						  "menuItemKind": "BUNDLE"
						}
						""")).andExpect(status().isCreated()).andExpect(jsonPath("$.menuItemKind").value("BUNDLE"));

		bundleId = jdbcTemplate.queryForObject(
				"select id from menu_item where business_code = ? and restaurant_chain_id = ?", Long.class,
				"BURGER-MEAL-API", chainId);

		mockMvc.perform(
				put("/api/v1/restaurant-chains/{chainId}/menu-items/{bundleId}/bundle-children", chainId, bundleId)
						.contentType(MediaType.APPLICATION_JSON).content("""
								{
								  "expectedVersion": 0,
								  "children": [
								    {"childMenuItemId": %d, "quantity": 1, "displayOrder": 0},
								    {"childMenuItemId": %d, "quantity": 1, "displayOrder": 1}
								  ]
								}
								""".formatted(burgerId, friesId)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.bundleMenuItemId").value(bundleId))
				.andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.children.length()").value(2));

		mockMvc.perform(
				get("/api/v1/restaurant-chains/{chainId}/menu-items/{bundleId}/bundle-children", chainId, bundleId))
				.andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.children[0].childMenuItemId").value(burgerId))
				.andExpect(jsonPath("$.children[1].childMenuItemId").value(friesId));

		Integer childCount = jdbcTemplate.queryForObject(
				"select count(*) from bundle_child where bundle_menu_item_id = ?", Integer.class, bundleId);
		assertEquals(2, childCount);

		Integer eventCount = jdbcTemplate.queryForObject(
				"select count(*) from outbox_event where aggregate_id = ? and event_type = ?", Integer.class, bundleId,
				"BUNDLE_CONFIGURATION_CHANGED");
		assertEquals(1, eventCount);
	}

	private void createOutletAndItems() {
		chainId = jdbcTemplate.queryForObject(
				"insert into restaurant_chain (display_name, business_code) values (?, ?) returning id", Long.class,
				"Bundle API Chain", "BUNDLE-API-" + UUID.randomUUID());
		outletId = jdbcTemplate.queryForObject(
				"insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
						+ "values (?, ?, ?, ?, ?) returning id",
				Long.class, "Bundle API Outlet", "EUR", "BUNDLE-API-OUTLET-" + UUID.randomUUID(), chainId,
				ChainStatus.DRAFT.name());
		burgerId = insertOutletMenuItem("Burger", "PRODUCT");
		friesId = insertOutletMenuItem("Fries", "PRODUCT");
	}

	private Long insertOutletMenuItem(String itemName, String kind) {
		return jdbcTemplate.queryForObject(
				"insert into menu_item (item_name, item_price, created_at, outlet_id, business_code, status, "
						+ "visibility, availability, version, restaurant_chain_id, updated_at, menu_item_kind) "
						+ "values (?, ?, current_timestamp, ?, ?, ?, true, true, 0, ?, current_timestamp, ?) "
						+ "returning id",
				Long.class, itemName, BigDecimal.TEN, outletId, "BUNDLE-API-ITEM-" + UUID.randomUUID(), "ACTIVE",
				chainId, kind);
	}

	private void deleteMenuItem(Long menuItemId) {
		if (menuItemId != null) {
			jdbcTemplate.update("delete from menu_item where id = ?", menuItemId);
		}
	}
}
