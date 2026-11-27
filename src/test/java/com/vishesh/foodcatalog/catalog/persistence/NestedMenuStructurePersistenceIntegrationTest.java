package com.vishesh.foodcatalog.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.vishesh.foodcatalog.catalog.domain.ModifierGroup;
import com.vishesh.foodcatalog.catalog.domain.ModifierOption;
import com.vishesh.foodcatalog.catalog.repository.BundleChildPersistenceAdapter;
import com.vishesh.foodcatalog.catalog.repository.ModifierGroupPersistenceAdapter;
import com.vishesh.foodcatalog.catalog.repository.ModifierOptionPersistenceAdapter;

/** Proves the new nested-menu rows survive a real PostgreSQL round trip. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NestedMenuStructurePersistenceIntegrationTest {

	@Autowired
	private BundleChildPersistenceAdapter bundleChildren;
	@Autowired
	private ModifierGroupPersistenceAdapter modifierGroups;
	@Autowired
	private ModifierOptionPersistenceAdapter modifierOptions;
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
			jdbcTemplate.update("delete from modifier_option where modifier_group_id in "
					+ "(select id from modifier_group where owner_menu_item_id = ?)", burgerMealId);
			jdbcTemplate.update("delete from modifier_group where owner_menu_item_id = ?", burgerMealId);
			jdbcTemplate.update("delete from bundle_child where bundle_menu_item_id = ?", burgerMealId);
		}
		if (burgerId != null || friesId != null) {
			jdbcTemplate.update("delete from bundle_child where child_menu_item_id in (?, ?)", burgerId, friesId);
		}
		if (burgerMealId != null)
			jdbcTemplate.update("delete from menu_item where id = ?", burgerMealId);
		if (burgerId != null)
			jdbcTemplate.update("delete from menu_item where id = ?", burgerId);
		if (friesId != null)
			jdbcTemplate.update("delete from menu_item where id = ?", friesId);
		if (outletId != null)
			jdbcTemplate.update("delete from outlet where id = ?", outletId);
		if (chainId != null)
			jdbcTemplate.update("delete from restaurant_chain where id = ?", chainId);
	}

	@Test
	void nestedMenuRows_roundTripWithGeneratedIdsAndRestaurantOrder() {
		createMealWithBurgerAndFries();

		List<BundleChild> savedChildren = bundleChildren.replaceForBundle(burgerMealId,
				List.of(new BundleChild(burgerMealId, burgerId, 1, 1), new BundleChild(burgerMealId, friesId, 2, 0)));

		assertEquals(2, savedChildren.size());
		assertTrue(savedChildren.stream().allMatch(child -> child.getId() != null));

		List<BundleChild> loadedChildren = bundleChildren.findByBundleMenuItemId(burgerMealId);
		assertEquals(List.of(friesId, burgerId), loadedChildren.stream().map(BundleChild::getChildMenuItemId).toList());
		assertEquals(List.of(burgerMealId), bundleChildren.findParentBundleIdsByChildMenuItemId(burgerId));

		ModifierGroup savedGroup = modifierGroups.save(new ModifierGroup(burgerMealId, "Choose extras", 0, 2, 0));
		assertNotNull(savedGroup.getId());

		ModifierOption savedOption = modifierOptions
				.save(new ModifierOption(savedGroup.getId(), "Extra cheese", BigDecimal.valueOf(20), 0));
		assertNotNull(savedOption.getId());

		assertEquals("Choose extras", modifierGroups.findByOwnerMenuItemId(burgerMealId).getFirst().getGroupName());
		assertEquals(BigDecimal.valueOf(20).setScale(2),
				modifierOptions.findByModifierGroupId(savedGroup.getId()).getFirst().getPriceDelta());
	}

	private void createMealWithBurgerAndFries() {
		chainId = jdbcTemplate.queryForObject(
				"insert into restaurant_chain (display_name, business_code) values (?, ?) returning id", Long.class,
				"Nested Structure Chain", "NESTED-CHAIN-" + UUID.randomUUID());
		outletId = jdbcTemplate.queryForObject(
				"insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status) "
						+ "values (?, ?, ?, ?, ?) returning id",
				Long.class, "Nested Structure Outlet", "EUR", "NESTED-OUTLET-" + UUID.randomUUID(), chainId,
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
				Long.class, itemName, BigDecimal.TEN, outletId, "NESTED-ITEM-" + UUID.randomUUID(), "DRAFT", chainId,
				kind);
	}
}
