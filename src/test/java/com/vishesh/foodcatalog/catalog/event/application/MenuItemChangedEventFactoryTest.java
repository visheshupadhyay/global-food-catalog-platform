package com.vishesh.foodcatalog.catalog.event.application;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

class MenuItemChangedEventFactoryTest {

    private MenuItemChangedEventFactory factory;

    private static final Long ITEM_ID = 101L;
    private static final Long CHAIN_ID = 20L;
    private static final Long OUTLET_ID = 30L;
    private static final Long VERSION = 4L;

    @BeforeEach
    void setUp() {
        factory = new MenuItemChangedEventFactory();
    }

    @Test
    void create_priceChangeEvent_populatesExpectedFields() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID,
                CHAIN_ID,
                VERSION,
                new BigDecimal("12.50"),
                null,
                null,
                OUTLET_ID,
                "corr-123");

        assertNotNull(event.getEventId());
        assertEquals(OutboxEventType.MENU_ITEM_CHANGED, event.getEventType());
        assertEquals(ITEM_ID, event.getItemId());
        assertEquals(CHAIN_ID, event.getRestaurantChainId());
        assertEquals(OUTLET_ID, event.getOutletId());
        assertEquals(VERSION, event.getAggregateVersion());
        assertEquals(new BigDecimal("12.50"), event.getPrice());
        assertNull(event.getAvailability());
        assertNull(event.getVisibility());
        assertEquals("corr-123", event.getCorrelationId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void create_availabilityChangeEvent_keepsOtherFieldsSparse() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, null, false, null, OUTLET_ID, null);

        assertNull(event.getPrice());
        assertEquals(Boolean.FALSE, event.getAvailability());
        assertNull(event.getVisibility());
    }

    @Test
    void create_visibilityChangeEvent_keepsOtherFieldsSparse() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, null, null, false, OUTLET_ID, null);

        assertNull(event.getPrice());
        assertNull(event.getAvailability());
        assertEquals(Boolean.FALSE, event.getVisibility());
    }

    @Test
    void create_multipleChanges_copiesEveryChangedValue() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("15.00"), true, false,
                OUTLET_ID, "corr-456");

        assertEquals(new BigDecimal("15.00"), event.getPrice());
        assertEquals(Boolean.TRUE, event.getAvailability());
        assertEquals(Boolean.FALSE, event.getVisibility());
    }

    @Test
    void create_withoutOutlet_createsSharedMenuItemEvent() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("10.00"), null, null,
                null, null);

        assertNull(event.getOutletId());
    }

    @Test
    void create_withoutChangedField_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION, null, null, null,
                        OUTLET_ID, null));
    }

    @Test
    void create_withNegativePrice_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("-1.00"),
                        null, null, OUTLET_ID, null));
    }

    @Test
    void create_withInvalidRequiredId_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(null, CHAIN_ID, VERSION, BigDecimal.ONE, null, null,
                        OUTLET_ID, null));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, 0L, VERSION, BigDecimal.ONE, null, null,
                        OUTLET_ID, null));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, -1L, BigDecimal.ONE, null, null,
                        OUTLET_ID, null));
    }

    @Test
    void create_withInvalidOutletId_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, null, null,
                        0L, null));
    }

    @Test
    void create_eachCall_generatesANewEventId() {
        MenuItemChangedEvent first = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, null, null, OUTLET_ID, null);
        MenuItemChangedEvent second = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, null, null, OUTLET_ID, null);

        assertNotEquals(first.getEventId(), second.getEventId());
    }
}
