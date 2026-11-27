package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MenuItemSeasonalAvailabilityTest {
	@Test
	void acceptsAnIncreasingDateRange() {
		MenuItemSeasonalAvailability season = new MenuItemSeasonalAvailability(1L, null, LocalDate.of(2026, 10, 15),
				LocalDate.of(2026, 11, 5), 0L);
		assertTrue(season.isRestricted());
	}
	@Test
	void acceptsEmptyRangeToClearRestriction() {
		assertFalse(new MenuItemSeasonalAvailability(1L, 2L, null, null, 1L).isRestricted());
	}
	@Test
	void rejectsIncompleteOrNonIncreasingDateRanges() {
		assertThrows(IllegalArgumentException.class,
				() -> new MenuItemSeasonalAvailability(1L, null, LocalDate.of(2026, 10, 15), null, 0L));
		assertThrows(IllegalArgumentException.class, () -> new MenuItemSeasonalAvailability(1L, null,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), 0L));
	}
}
