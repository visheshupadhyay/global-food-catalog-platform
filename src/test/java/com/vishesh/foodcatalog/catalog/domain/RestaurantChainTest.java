package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class RestaurantChainTest {
    @Test
    void createsDraftChainWithRequiredFields() {
        String businessCode = "BURGER-KING-001";
        String displayName = "BURGER-KING";
        RestaurantChain restaurantChain = new RestaurantChain(businessCode, displayName);

        assertNotNull(restaurantChain);
        assertEquals(businessCode, restaurantChain.getBusinessCode());
        assertEquals(displayName, restaurantChain.getDisplayName());
        assertEquals(ChainStatus.DRAFT, restaurantChain.getStatus());
        assertNotNull(restaurantChain.getCreatedAt());
        assertNotNull(restaurantChain.getUpdatedAt());
        assertEquals(restaurantChain.getCreatedAt(), restaurantChain.getUpdatedAt());
    }

    @Test
    void rejectsBlankBusinessCode() {
        String businessCode = "  ";
        String displayName = "BURGER-KING";
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChain(businessCode, displayName));
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChain("BURGER-KING-001", "  "));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChain(null, "BURGER-KING"));
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChain("BURGER-KING-001", null));
    }

    @Test
    void trimsValues() {
        RestaurantChain restaurantChain = new RestaurantChain(
                "  BURGER-KING-001  ", "  BURGER-KING  ");

        assertEquals("BURGER-KING-001", restaurantChain.getBusinessCode());
        assertEquals("BURGER-KING", restaurantChain.getDisplayName());
    }

    @Test
    void updatesTimestampWhenStatusChanges() {
        String businessCode = "BURGER-KING-001";
        String displayName = "BURGER-KING";
        RestaurantChain restaurantChain = new RestaurantChain(businessCode, displayName);
        Instant originalUpdatedAt = restaurantChain.getUpdatedAt();
        restaurantChain.changeStatusTo(ChainStatus.ACTIVE);

        assertEquals(ChainStatus.ACTIVE, restaurantChain.getStatus());
        assertFalse(restaurantChain.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void rejectsNullStatus() {
        RestaurantChain restaurantChain = new RestaurantChain("BURGER-KING-001", "BURGER-KING");

        assertThrows(IllegalArgumentException.class,
                () -> restaurantChain.changeStatusTo(null));
    }

    @Test
    void allowsOnlyForwardLifecycleTransitions() {
        RestaurantChain restaurantChain = new RestaurantChain("BURGER-KING-001", "BURGER-KING");

        restaurantChain.changeStatusTo(ChainStatus.ACTIVE);
        restaurantChain.changeStatusTo(ChainStatus.ARCHIVED);

        assertEquals(ChainStatus.ARCHIVED, restaurantChain.getStatus());
    }

    @Test
    void rejectsSkippingLifecycleStates() {
        RestaurantChain restaurantChain = new RestaurantChain("BURGER-KING-001", "BURGER-KING");

        assertThrows(IllegalStateException.class,
                () -> restaurantChain.changeStatusTo(ChainStatus.ARCHIVED));
    }

    @Test
    void requiresExplicitRestoreForArchivedChain() {
        RestaurantChain restaurantChain = new RestaurantChain("BURGER-KING-001", "BURGER-KING");
        restaurantChain.changeStatusTo(ChainStatus.ACTIVE);
        restaurantChain.changeStatusTo(ChainStatus.ARCHIVED);

        assertThrows(IllegalStateException.class,
                () -> restaurantChain.changeStatusTo(ChainStatus.ACTIVE));

        restaurantChain.restoreFromArchive();

        assertEquals(ChainStatus.ACTIVE, restaurantChain.getStatus());
    }

    @Test
    void restoringNonArchivedChainIsRejected() {
        RestaurantChain restaurantChain = new RestaurantChain("BURGER-KING-001", "BURGER-KING");

        assertThrows(IllegalStateException.class, restaurantChain::restoreFromArchive);
    }

    @Test
    void rehydratesExistingChainWithPersistedFields() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        RestaurantChain rehydrated = new RestaurantChain(
                "  BURGER-KING-001  ",
                "  BURGER-KING  ",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt,
                101L);

        assertEquals(101L, rehydrated.getId());
        assertEquals("BURGER-KING-001", rehydrated.getBusinessCode());
        assertEquals("BURGER-KING", rehydrated.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, rehydrated.getStatus());
        assertEquals(createdAt, rehydrated.getCreatedAt());
        assertEquals(updatedAt, rehydrated.getUpdatedAt());
    }
}
