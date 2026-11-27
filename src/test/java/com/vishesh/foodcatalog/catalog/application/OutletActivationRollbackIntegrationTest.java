package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.domain.ChainStatus;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;

/**
 * Proves the activation transaction is atomic even though saving the outlet
 * flushes early to obtain its next optimistic-lock version for the event.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutletActivationRollbackIntegrationTest {
	@Autowired
	private OutletService service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private OutboxEventRepository outboxEvents;

	private Long chainId;
	private Long menuGroupId;
	private Long outletId;

	@AfterEach
	void cleanUp() {
		if (outletId != null) {
			jdbcTemplate.update("delete from outbox_event where aggregate_id = ?", outletId);
			jdbcTemplate.update("delete from menu_group_outlet where outlet_id = ?", outletId);
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
	void activate_whenOutboxSaveFails_rollsBackTheOutletStatusAndVersion() {
		createDraftOutletWithActiveMenuGroup();
		doThrow(new RuntimeException("forced outbox failure")).when(outboxEvents).save(any(OutboxEvent.class));

		assertThrows(RuntimeException.class, () -> service.activateOutlet(outletId, 0L));

		assertEquals(ChainStatus.DRAFT.name(),
				jdbcTemplate.queryForObject("select status from outlet where id = ?", String.class, outletId));
		assertEquals(0L, jdbcTemplate.queryForObject("select version from outlet where id = ?", Long.class, outletId));
		assertEquals(0, jdbcTemplate.queryForObject("select count(*) from outbox_event where aggregate_id = ?",
				Integer.class, outletId));
	}

	private void createDraftOutletWithActiveMenuGroup() {
		chainId = jdbcTemplate.queryForObject(
				"insert into restaurant_chain (display_name, business_code, status) values (?, ?, ?) returning id",
				Long.class, "Outlet Rollback Chain", "OUTLET-ROLLBACK-" + UUID.randomUUID(), ChainStatus.ACTIVE.name());
		menuGroupId = jdbcTemplate.queryForObject(
				"insert into menu_group (display_name, restaurant_chain_id, status) values (?, ?, ?) returning id",
				Long.class, "Main Menu", chainId, ChainStatus.ACTIVE.name());
		outletId = jdbcTemplate.queryForObject(
				"insert into outlet (display_name, currency, outlet_code, restaurant_chain_id, status, timezone) "
						+ "values (?, ?, ?, ?, ?, ?) returning id",
				Long.class, "Indiranagar", "INR", "OUTLET-ROLLBACK-" + UUID.randomUUID(), chainId,
				ChainStatus.DRAFT.name(), "Asia/Kolkata");
		jdbcTemplate.update("insert into menu_group_outlet (menu_group_id, outlet_id) values (?, ?)", menuGroupId,
				outletId);
	}
}
