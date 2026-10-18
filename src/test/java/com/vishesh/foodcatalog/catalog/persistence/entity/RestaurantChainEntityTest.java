package com.vishesh.foodcatalog.catalog.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

public class RestaurantChainEntityTest {
    @Test
    void createsDraftRestaurantChainWithRequiredFields() {
        String displayName = "BURGER-KING";
        String businessCode = "BERLIN-KING-001";
        RestaurantChainEntity entity = new RestaurantChainEntity(businessCode, displayName);

        assertEquals(displayName, entity.getDisplayName());
        assertEquals(businessCode, entity.getBusinessCode());
        assertEquals(ChainStatus.DRAFT, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());

    }

    @Test
    void rejectsNegativeId() {
        String displayName = "BURGER-KING";
        String businessCode = "BERLIN-KING-001";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(
                        -1L,
                        businessCode,
                        displayName,
                        ChainStatus.ACTIVE,
                        createdAt,
                        updatedAt));
    }

    @Test
    void rejectsBlankInputs() {
        String displayName = "BURGER-KING";
        String businessCode = "BERLIN-KING-001";

        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity("     ", displayName));
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(businessCode, "      "));
    }

    @Test
    void rejectsZeroInputsInId() {
        String displayName = "BURGER-KING";
        String businessCode = "BERLIN-KING-001";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(
                        0L,
                        businessCode, displayName,
                        ChainStatus.ACTIVE,
                        createdAt,
                        updatedAt));
    }

    @Test
    void rejectsNullInputs() {
        String displayName = "BURGER-KING";
        String businessCode = "BERLIN-KING-001";
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(
                        101L,
                        "  BURGER-KING-001  ",
                        "  BURGER-KING  ",
                        null,
                        createdAt,
                        updatedAt));

        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(
                        101L,
                        "  BURGER-KING-001  ",
                        "  BURGER-KING  ",
                        ChainStatus.ACTIVE,
                        null,
                        updatedAt));

        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(
                        101L,
                        "  BURGER-KING-001  ",
                        "  BURGER-KING  ",
                        ChainStatus.ACTIVE,
                        createdAt,
                        null));

        assertDoesNotThrow(
                () -> new RestaurantChainEntity(
                        null,
                        "  BURGER-KING-001  ",
                        "  BURGER-KING  ",
                        ChainStatus.ACTIVE,
                        createdAt,
                        updatedAt));

        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(null, displayName));
        assertThrows(IllegalArgumentException.class,
                () -> new RestaurantChainEntity(businessCode, null));
    }

    @Test
    void trimsRequiredTextFields() {
        RestaurantChainEntity entity = new RestaurantChainEntity("  BK-001  ", "  Burger King  ");

        assertEquals("BK-001", entity.getBusinessCode());
        assertEquals("Burger King", entity.getDisplayName());
    }

    @Test
    void rehydratingAllPersistedFields() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        RestaurantChainEntity rehydrated = new RestaurantChainEntity(
                101L,
                "  BURGER-KING-001  ",
                "  BURGER-KING  ",
                ChainStatus.ACTIVE,
                createdAt,
                updatedAt);

        assertEquals(101L, rehydrated.getId());
        assertEquals("BURGER-KING-001", rehydrated.getBusinessCode());
        assertEquals("BURGER-KING", rehydrated.getDisplayName());
        assertEquals(ChainStatus.ACTIVE, rehydrated.getStatus());
        assertEquals(createdAt, rehydrated.getCreatedAt());
        assertEquals(updatedAt, rehydrated.getUpdatedAt());
    }
}
