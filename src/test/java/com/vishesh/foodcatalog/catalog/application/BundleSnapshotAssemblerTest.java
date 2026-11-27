package com.vishesh.foodcatalog.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.domain.BundleChild;
import com.vishesh.foodcatalog.catalog.domain.MenuItem;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.repository.BundleChildRepository;
import com.vishesh.foodcatalog.catalog.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class BundleSnapshotAssemblerTest {
	private static final Long CHAIN_ID = 100L;

	@Mock
	private MenuItemRepository menuItems;

	@Mock
	private BundleChildRepository bundleChildren;

	private BundleSnapshotAssembler assembler;

	@BeforeEach
	void setUp() {
		assembler = new BundleSnapshotAssembler(menuItems, bundleChildren);
	}

	@Test
	void assemble_flatBundleMapsChildrenInRelationshipDisplayOrder() {
		MenuItem meal = item(1L, "Burger Meal", MenuItemKind.BUNDLE);
		MenuItem burger = item(2L, "Burger", MenuItemKind.PRODUCT);
		MenuItem fries = item(3L, "Fries", MenuItemKind.PRODUCT);
		whenItemIsLoaded(meal, burger, fries);
		when(bundleChildren.findByBundleMenuItemId(1L))
				.thenReturn(List.of(new BundleChild(1L, 2L, 1, 0), new BundleChild(1L, 3L, 2, 1)));

		EventBundleSnapshot snapshot = assembler.assemble(1L, CHAIN_ID);

		assertEquals("Burger Meal", snapshot.name());
		assertEquals(BigDecimal.TEN, snapshot.price());
		assertEquals(List.of("Burger", "Fries"), snapshot.children().stream().map(child -> child.name()).toList());
		assertEquals(2, snapshot.children().get(1).quantity());
		assertEquals(1, snapshot.children().get(1).displayOrder());
	}

	@Test
	void assemble_nestedBundleKeepsTheNestedTree() {
		MenuItem familyMeal = item(1L, "Family Meal", MenuItemKind.BUNDLE);
		MenuItem burgerMeal = item(2L, "Burger Meal", MenuItemKind.BUNDLE);
		MenuItem fries = item(3L, "Fries", MenuItemKind.PRODUCT);
		whenItemIsLoaded(familyMeal, burgerMeal, fries);
		when(bundleChildren.findByBundleMenuItemId(1L)).thenReturn(List.of(new BundleChild(1L, 2L, 1, 0)));
		when(bundleChildren.findByBundleMenuItemId(2L)).thenReturn(List.of(new BundleChild(2L, 3L, 1, 0)));

		EventBundleSnapshot snapshot = assembler.assemble(1L, CHAIN_ID);

		assertEquals("Burger Meal", snapshot.children().getFirst().name());
		assertEquals("Fries", snapshot.children().getFirst().children().getFirst().name());
	}

	@Test
	void assemble_productChildDoesNotTryToLoadNestedRelationships() {
		MenuItem meal = item(1L, "Burger Meal", MenuItemKind.BUNDLE);
		MenuItem fries = item(2L, "Fries", MenuItemKind.PRODUCT);
		whenItemIsLoaded(meal, fries);
		when(bundleChildren.findByBundleMenuItemId(1L)).thenReturn(List.of(new BundleChild(1L, 2L, 1, 0)));

		EventBundleSnapshot snapshot = assembler.assemble(1L, CHAIN_ID);

		assertEquals(List.of(), snapshot.children().getFirst().children());
		verify(bundleChildren, never()).findByBundleMenuItemId(2L);
	}

	@Test
	void assemble_rejectsMissingRootBundle() {
		when(menuItems.findByIdAndRestaurantChainId(1L, CHAIN_ID)).thenReturn(Optional.empty());

		assertThrows(MenuItemNotFoundException.class, () -> assembler.assemble(1L, CHAIN_ID));
	}

	@Test
	void assemble_rejectsRootThatIsNotABundle() {
		whenItemIsLoaded(item(1L, "Fries", MenuItemKind.PRODUCT));

		assertThrows(IllegalArgumentException.class, () -> assembler.assemble(1L, CHAIN_ID));
	}

	@Test
	void assemble_rejectsMissingChild() {
		whenItemIsLoaded(item(1L, "Burger Meal", MenuItemKind.BUNDLE));
		when(bundleChildren.findByBundleMenuItemId(1L)).thenReturn(List.of(new BundleChild(1L, 999L, 1, 0)));
		when(menuItems.findByIdAndRestaurantChainId(999L, CHAIN_ID)).thenReturn(Optional.empty());

		assertThrows(MenuItemNotFoundException.class, () -> assembler.assemble(1L, CHAIN_ID));
	}

	@Test
	void assemble_rejectsAStoredStructureDeeperThanThreeHops() {
		MenuItem root = item(1L, "Root", MenuItemKind.BUNDLE);
		MenuItem levelOne = item(2L, "Level one", MenuItemKind.BUNDLE);
		MenuItem levelTwo = item(3L, "Level two", MenuItemKind.BUNDLE);
		MenuItem levelThree = item(4L, "Level three", MenuItemKind.BUNDLE);
		// The assembler rejects the fourth hop before loading its target.
		// Therefore only the first three child items need repository stubs.
		whenItemIsLoaded(root, levelOne, levelTwo, levelThree);
		when(bundleChildren.findByBundleMenuItemId(1L)).thenReturn(List.of(new BundleChild(1L, 2L, 1, 0)));
		when(bundleChildren.findByBundleMenuItemId(2L)).thenReturn(List.of(new BundleChild(2L, 3L, 1, 0)));
		when(bundleChildren.findByBundleMenuItemId(3L)).thenReturn(List.of(new BundleChild(3L, 4L, 1, 0)));
		when(bundleChildren.findByBundleMenuItemId(4L)).thenReturn(List.of(new BundleChild(4L, 5L, 1, 0)));

		assertThrows(IllegalStateException.class, () -> assembler.assemble(1L, CHAIN_ID));
	}

	private void whenItemIsLoaded(MenuItem... items) {
		for (MenuItem item : items) {
			when(menuItems.findByIdAndRestaurantChainId(item.getId(), CHAIN_ID)).thenReturn(Optional.of(item));
		}
	}

	private MenuItem item(Long id, String name, MenuItemKind kind) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new MenuItem(id, name, BigDecimal.TEN, now, 10L, 20L, null, "ITEM-" + id, MenuItemStatus.ACTIVE, true,
				true, 4L, CHAIN_ID, now, kind);
	}
}
