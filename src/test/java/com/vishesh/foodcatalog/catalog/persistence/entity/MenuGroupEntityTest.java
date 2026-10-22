package com.vishesh.foodcatalog.catalog.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

public class MenuGroupEntityTest {
        @Test
        void createsDraftMenuGroupWithRequiredFields() {
                String displayName = "BURGER-KING";
                Long restaurantChainId = 101L;
                MenuGroupEntity entity = new MenuGroupEntity(restaurantChainId, displayName);

                assertEquals(displayName, entity.getDisplayName());
                assertEquals(ChainStatus.DRAFT, entity.getStatus());
                assertNotNull(entity.getCreatedAt());
                assertNotNull(entity.getUpdatedAt());
                assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
                assertNull(entity.getId());
                assertEquals(restaurantChainId, entity.getRestaurantChainId());
        }

        @Test
        void rejectsNegativeId() {
                String displayName = "BURGER-KING";
                Long restaurantChainId = 101L;
                Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
                Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");
                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                displayName,
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                -1L));
        }

        @Test
        void rejectsBlankInputs() {
                Long restaurantChainId = 101L;
                Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
                Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(restaurantChainId, "     "));

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                "   ",
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                -1L));
        }

        @Test
        void rejectsZeroInputsInId() {
                String displayName = "BURGER-KING";
                Long restaurantChainId = 101L;
                Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
                Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                displayName,
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                0L));
        }

        @Test
        void rejectsNullInputs() {
                String displayName = "BURGER-KING";
                Long restaurantChainId = 101L;
                Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
                Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                null,
                                                displayName,
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                101L));

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                null,
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                101L));
                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                displayName,
                                                null,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                101L));

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                displayName,
                                                ChainStatus.ACTIVE,
                                                null,
                                                updatedAt,
                                                3L,
                                                101L));

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                displayName,
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                null,
                                                3L,
                                                101L));

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(
                                                restaurantChainId,
                                                displayName,
                                                ChainStatus.ACTIVE,
                                                createdAt,
                                                updatedAt,
                                                3L,
                                                null));

                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(null, displayName));
                assertThrows(IllegalArgumentException.class,
                                () -> new MenuGroupEntity(restaurantChainId, null));
        }

        @Test
        void trimsRequiredTextFields() {
                Long restaurantChainId = 101L;
                MenuGroupEntity entity = new MenuGroupEntity(restaurantChainId, "  Burger King  ");
                assertEquals("Burger King", entity.getDisplayName());
        }

        @Test
        void rehydratingAllPersistedFields() {
                String displayName = "BURGER-KING";
                Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
                Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");
                Long restaurantChainId = 101L;
                MenuGroupEntity rehydrated = new MenuGroupEntity(
                                restaurantChainId,
                                displayName,
                                ChainStatus.ACTIVE,
                                createdAt,
                                updatedAt,
                                3L,
                                101L);

                assertEquals(101L, rehydrated.getId());
                assertEquals("BURGER-KING", rehydrated.getDisplayName());
                assertEquals(ChainStatus.ACTIVE, rehydrated.getStatus());
                assertEquals(createdAt, rehydrated.getCreatedAt());
                assertEquals(updatedAt, rehydrated.getUpdatedAt());
                assertEquals(restaurantChainId, rehydrated.getRestaurantChainId());
        }

        @Test
        void newMenuGroupEntityStartsWithVersionZero() {
                MenuGroupEntity entity = new MenuGroupEntity(201L, "BURGER-KING");

                assertNull(entity.getId());
                assertEquals(0L, entity.getVersion());
                assertEquals(ChainStatus.DRAFT, entity.getStatus());
        }
}
