package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertNull(menuGroup.getId());
    }

    @Test
    void rejectsBlankDisplayName() {
        String displayName = "  ";
        Long restaurantChainId = 10L;
        Instant expectedCreatedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant expectedUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");
        
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId, displayName));
        
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        ChainStatus.ACTIVE,
                        expectedCreatedAt,
                        expectedUpdatedAt,
                        3L,
                        201L));
    }

    @Test
    void rejectsNullInputs() {
        Long restaurantChainId = 10L;
        Instant expectedCreatedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant expectedUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");
        String displayName = "Burgers";
        ChainStatus status = ChainStatus.ACTIVE;
        Long menuGroupId = 201L;
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(null, "BURGER-KING"));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        null,
                        expectedCreatedAt,
                        expectedUpdatedAt,
                        3L,
                        menuGroupId));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        status,
                        null,
                        expectedUpdatedAt,
                        3L,
                        menuGroupId));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        status,
                        expectedCreatedAt,
                        null,
                        3L,
                        menuGroupId));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        status,
                        expectedCreatedAt,
                        expectedUpdatedAt,
                        3L,
                        null));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        null,
                        status,
                        expectedCreatedAt,
                        expectedUpdatedAt,
                        3L,
                        -1L));
    }

    @Test
    void rejectsZeroOrNegativeChainIdInputs() {
        Long restaurantChainId = 10L;
        Instant expectedCreatedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant expectedUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");
        String displayName = "Burgers";
        ChainStatus status = ChainStatus.ACTIVE;

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(0L, "BURGER-KING"));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(-1L, "BURGER-KING"));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        status,
                        expectedCreatedAt,
                        expectedUpdatedAt,
                        3L,
                        0L));

        assertThrows(IllegalArgumentException.class,
                () -> new MenuGroup(restaurantChainId,
                        displayName,
                        status,
                        expectedCreatedAt,
                        expectedUpdatedAt,
                        3L,
                        -1L));
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

    @Test
    void rehydratesExistingMenuGroupWithPersistedFields() {
        Instant expectedCreatedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant expectedUpdatedAt = Instant.parse("2026-01-02T10:00:00Z");

        MenuGroup menuGroup = new MenuGroup(42L,
                "  Burgers  ",
                ChainStatus.ACTIVE,
                expectedCreatedAt,
                expectedUpdatedAt,
                3L,
                201L);
        assertEquals(201L, menuGroup.getId());
        assertEquals("Burgers", menuGroup.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, menuGroup.getStatus());
        assertEquals(expectedCreatedAt, menuGroup.getCreatedAt());
        assertEquals(expectedUpdatedAt, menuGroup.getUpdatedAt());
        assertEquals(42L, menuGroup.getRestaurantChainId());
    }
}
