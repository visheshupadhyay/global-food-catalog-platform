package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BundleChildTest {

	@Test
	void validChild_keepsQuantityAndDisplayOrder() {
		BundleChild child = new BundleChild(10L, 20L, 2, 0);

		assertEquals(10L, child.getBundleMenuItemId());
		assertEquals(20L, child.getChildMenuItemId());
		assertEquals(2, child.getQuantity());
		assertEquals(0, child.getDisplayOrder());
		assertNull(child.getId());
	}

	@Test
	void rejectsZeroQuantity() {
		assertThrows(IllegalArgumentException.class, () -> new BundleChild(10L, 20L, 0, 0));
	}

	@Test
	void rejectsDirectSelfReference() {
		assertThrows(IllegalArgumentException.class, () -> new BundleChild(10L, 10L, 1, 0));
	}
}
