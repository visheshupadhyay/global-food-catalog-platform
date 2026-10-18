package com.vishesh.foodcatalog.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

public class OutletTest {
    @Test
    void createsDraftOutletWithRequiredFields() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);
        assertNotNull(outlet);
        assertEquals(restaurantChainId, outlet.getRestaurantChainId());
        assertEquals(displayName, outlet.getDisplayName());
        assertEquals(ChainStatus.DRAFT, outlet.getStatus());
        assertNotNull(outlet.getCreatedAt());
        assertNotNull(outlet.getUpdatedAt());
        assertEquals(outlet.getCreatedAt(), outlet.getUpdatedAt());
        assertEquals(outletCode, outlet.getOutletCode());
        assertEquals(currency, outlet.getCurrency());
    }

    @Test
    void rejectsBlankInputs() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(restaurantChainId, "  ", outletCode, currency));
        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(restaurantChainId, displayName, "  ", currency));
    }

    @Test
    void rejectsNullInputs() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(null, displayName, outletCode, currency));
        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(restaurantChainId, null, outletCode, currency));
        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(restaurantChainId, displayName, null, currency));
        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(restaurantChainId, displayName, outletCode, null));
    }

    @Test
    void rejectsZeroOrNegativeChainIdInputs() {
        String displayName = "BURGER-KING";
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(-1L, displayName, outletCode, currency));

        assertThrows(IllegalArgumentException.class,
                () -> new Outlet(0L, displayName, outletCode, currency));
    }

    @Test
    void trimsValues() {
        String displayName = "  BURGER-KING ";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "  BERLIN-MITTE ";
        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);
        assertEquals("BERLIN-MITTE", outlet.getOutletCode());
        assertEquals("BURGER-KING", outlet.getDisplayName());
    }

    @Test
    void updatesTimestampWhenStatusChanges() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);

        Instant originalUpdatedAt = outlet.getUpdatedAt();
        outlet.changeStatusTo(ChainStatus.ACTIVE);
        assertEquals(ChainStatus.ACTIVE, outlet.getStatus());
        assertFalse(outlet.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void rejectsNullStatus() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);

        assertThrows(IllegalArgumentException.class,
                () -> outlet.changeStatusTo(null));
    }

    @Test
    void allowsOnlyForwardLifecycleTransitions() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);

        outlet.changeStatusTo(ChainStatus.ACTIVE);
        outlet.changeStatusTo(ChainStatus.ARCHIVED);

        assertEquals(ChainStatus.ARCHIVED, outlet.getStatus());
    }

    @Test
    void rejectsSkippingLifecycleStates() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);
        assertThrows(IllegalStateException.class,
                () -> outlet.changeStatusTo(ChainStatus.ARCHIVED));
    }

    @Test
    void requiresExplicitRestoreForArchivedOutlet() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);

        outlet.changeStatusTo(ChainStatus.ACTIVE);
        outlet.changeStatusTo(ChainStatus.ARCHIVED);

        assertThrows(IllegalStateException.class,
                () -> outlet.changeStatusTo(ChainStatus.ACTIVE));

        outlet.restoreFromArchive();

        assertEquals(ChainStatus.ACTIVE, outlet.getStatus());
    }

    @Test
    void restoringNonArchivedOutletIsRejected() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);

        assertThrows(IllegalStateException.class, outlet::restoreFromArchive);
    }

    @Test
    void changingToCurrentStatusDoesNotUpdateTimestamp() {
        String displayName = "BURGER-KING";
        Long restaurantChainId = 10L;
        Currency currency = Currency.getInstance("EUR");
        String outletCode = "BERLIN-MITTE";

        Outlet outlet = new Outlet(restaurantChainId, displayName, outletCode, currency);
        Instant lastUpdated = outlet.getUpdatedAt();
        outlet.changeStatusTo(ChainStatus.DRAFT);
        Instant currentUpdated = outlet.getUpdatedAt();

        assertEquals(lastUpdated, currentUpdated);

    }
}