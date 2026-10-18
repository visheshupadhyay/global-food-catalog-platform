package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class MenuGroupTest {
    @Test
    void createsDraftMenuGroupWithRequiredFields() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, displayName);

        assertNotNull(menuGroup);
        assertEquals(restaurantChainId, menuGroup.getRestaurantChainId());
        assertEquals(displayName, menuGroup.getDisplayName());
        assertEquals(ChainStatus.DRAFT, menuGroup.getStatus());
        assertNotNull(menuGroup.getCreatedAt());
        assertNotNull(menuGroup.getUpdatedAt());
        assertEquals(menuGroup.getCreatedAt(), menuGroup.getUpdatedAt());
    }

    @Test
    void rejectsBlankDisplayName() {
        String displayName = "  ";
        Long restaurantChainId = 10L;
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId, displayName));
    }

    @Test
    void rejectsNullInputs() {
        Long restaurantChainId = 10L;
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(null, "BURGER-KING"));
    }

    @Test
    void rejectsZeroOrNegativeChainIdInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(0L, "BURGER-KING"));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(-1L, "BURGER-KING"));
    }

    @Test
    void trimsValues() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId,
                "  BURGER-KING  ");

        assertEquals("BURGER-KING", menuGroup.getDisplayName());
    }

    @Test
    void updatesTimestampWhenStatusChanges() {
        Long restaurantChainId = 10L;
        String displayName = "BURGER-KING";
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, displayName);
        Instant originalUpdatedAt = menuGroup.getUpdatedAt();
        menuGroup.changeStatusTo(ChainStatus.ACTIVE);

        assertEquals(ChainStatus.ACTIVE, menuGroup.getStatus());
        assertFalse(menuGroup.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void rejectsNullStatus() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, "BURGER-KING");

        assertThrows(IllegalArgumentException.class,
                () -> menuGroup.changeStatusTo(null));
    }

    @Test
    void allowsOnlyForwardLifecycleTransitions() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, "BURGER-KING");

        menuGroup.changeStatusTo(ChainStatus.ACTIVE);
        menuGroup.changeStatusTo(ChainStatus.ARCHIVED);

        assertEquals(ChainStatus.ARCHIVED, menuGroup.getStatus());
    }

    @Test
    void rejectsSkippingLifecycleStates() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, "BURGER-KING");

        assertThrows(IllegalStateException.class,
                () -> menuGroup.changeStatusTo(ChainStatus.ARCHIVED));
    }

    @Test
    void requiresExplicitRestoreForArchivedChain() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, "BURGER-KING");

        menuGroup.changeStatusTo(ChainStatus.ACTIVE);
        menuGroup.changeStatusTo(ChainStatus.ARCHIVED);

        assertThrows(IllegalStateException.class,
                () -> menuGroup.changeStatusTo(ChainStatus.ACTIVE));

        menuGroup.restoreFromArchive();

        assertEquals(ChainStatus.ACTIVE, menuGroup.getStatus());
    }

    @Test
    void restoringNonArchivedMenuGroupIsRejected() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, "BURGER-KING");

        assertThrows(IllegalStateException.class, menuGroup::restoreFromArchive);
    }

    @Test
    void changingToCurrentStatusDoesNotUpdateTimestamp() {
        Long restaurantChainId = 10L;
        MenuGroup menuGroup = new MenuGroup(restaurantChainId, "BURGER-KING");
        Instant lastUpdated = menuGroup.getUpdatedAt();
        menuGroup.changeStatusTo(ChainStatus.DRAFT);
        Instant currentUpdated = menuGroup.getUpdatedAt();

        assertEquals(lastUpdated, currentUpdated);

    }
}
