package com.vishesh.foodcatalog.catalog.event.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;

import tools.jackson.databind.ObjectMapper;

class EventBundleSnapshotTest {

    @Test
    void nestedBundleChild_preservesTheCompleteTree() {
        EventBundleChild fries = child(12L, "Fries", MenuItemKind.PRODUCT, List.of());
        EventBundleChild burgerMeal = child(11L, "Burger Meal", MenuItemKind.BUNDLE, List.of(fries));

        EventBundleSnapshot familyMeal = new EventBundleSnapshot(10L, "Family Meal",
                BigDecimal.valueOf(499), List.of(burgerMeal));

        assertEquals("Fries", familyMeal.children().getFirst().children().getFirst().name());
    }

    @Test
    void childCopiesItsChildrenSoTheEventCannotBeChangedLater() {
        List<EventBundleChild> mutableChildren = new ArrayList<>();
        EventBundleChild burgerMeal = child(11L, "Burger Meal", MenuItemKind.BUNDLE, mutableChildren);
        mutableChildren.add(child(12L, "Fries", MenuItemKind.PRODUCT, List.of()));

        assertEquals(List.of(), burgerMeal.children());
    }

    @Test
    void child_rejectsAnInvalidQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventBundleChild(11L, "Fries", MenuItemKind.PRODUCT, 0, 0,
                        MenuItemStatus.ACTIVE, true, true, List.of()));
    }

    @Test
    void snapshot_rejectsAMissingChildList() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventBundleSnapshot(10L, "Burger Meal", BigDecimal.valueOf(299), null));
    }

    @Test
    void snapshot_roundTripsThroughJsonWithoutLosingNestedChildren() throws Exception {
        EventBundleSnapshot original = new EventBundleSnapshot(10L, "Family Meal", BigDecimal.valueOf(499),
                List.of(child(11L, "Burger Meal", MenuItemKind.BUNDLE,
                        List.of(child(12L, "Fries", MenuItemKind.PRODUCT, List.of())))));
        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(original);
        EventBundleSnapshot restored = objectMapper.readValue(json, EventBundleSnapshot.class);

        assertEquals(original, restored);
        assertEquals("Fries", restored.children().getFirst().children().getFirst().name());
    }

    private static EventBundleChild child(Long id, String name, MenuItemKind kind, List<EventBundleChild> children) {
        return new EventBundleChild(id, name, kind, 1, 0,
                MenuItemStatus.ACTIVE, true, true, children);
    }
}
