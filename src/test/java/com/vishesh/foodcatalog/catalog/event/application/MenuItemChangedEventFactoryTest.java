package com.vishesh.foodcatalog.catalog.event.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vishesh.foodcatalog.catalog.event.domain.MenuItemChangedEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierGroup;
import com.vishesh.foodcatalog.catalog.event.domain.EventModifierOption;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleChild;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;

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
                "corr-123",
                MenuItemStatus.ACTIVE,
                "Veg Burger");

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
        assertEquals(MenuItemStatus.ACTIVE, event.getStatus());
        assertEquals("Veg Burger", event.getItemName());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void create_availabilityChangeEvent_keepsOtherFieldsSparse() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID,
                CHAIN_ID,
                VERSION,
                null,
                false,
                null,
                OUTLET_ID,
                null,
                MenuItemStatus.ACTIVE,
                "Veg Burger");

        assertNull(event.getPrice());
        assertEquals(Boolean.FALSE, event.getAvailability());
        assertNull(event.getVisibility());
    }

    @Test
    void create_visibilityChangeEvent_keepsOtherFieldsSparse() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, null, null, false, OUTLET_ID, null, MenuItemStatus.HIDDEN,"Veg Burger");

        assertNull(event.getPrice());
        assertNull(event.getAvailability());
        assertEquals(Boolean.FALSE, event.getVisibility());
    }

    @Test
    void create_multipleChanges_copiesEveryChangedValue() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("15.00"), true, false,
                OUTLET_ID, "corr-456", MenuItemStatus.ACTIVE,"Veg Burger");

        assertEquals(new BigDecimal("15.00"), event.getPrice());
        assertEquals(Boolean.TRUE, event.getAvailability());
        assertEquals(Boolean.FALSE, event.getVisibility());
    }

    @Test
    void createMenuItemActivated_createsCompleteInitialItemSnapshot() {
        MenuItemChangedEvent event = factory.createMenuItemActivated(
                ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("149.00"), true, true,
                OUTLET_ID, "corr-activation", MenuItemStatus.ACTIVE, "Veg Burger");

        assertEquals(OutboxEventType.MENU_ITEM_ACTIVATED, event.getEventType());
        assertEquals(ITEM_ID, event.getItemId());
        assertEquals(CHAIN_ID, event.getRestaurantChainId());
        assertEquals(OUTLET_ID, event.getOutletId());
        assertEquals(VERSION, event.getAggregateVersion());
        assertEquals("Veg Burger", event.getItemName());
        assertEquals(new BigDecimal("149.00"), event.getPrice());
        assertEquals(Boolean.TRUE, event.getAvailability());
        assertEquals(Boolean.TRUE, event.getVisibility());
        assertEquals(MenuItemStatus.ACTIVE, event.getStatus());
        assertEquals("corr-activation", event.getCorrelationId());
    }

    @Test
    void createMenuItemActivated_rejectsIncompleteOrNonActivePayload() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.createMenuItemActivated(
                        ITEM_ID, CHAIN_ID, VERSION, null, true, true,
                        OUTLET_ID, null, MenuItemStatus.ACTIVE, "Veg Burger"));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createMenuItemActivated(
                        ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, true, true,
                        OUTLET_ID, null, MenuItemStatus.HIDDEN, "Veg Burger"));
    }

    @Test
    void create_withoutOutlet_createsSharedMenuItemEvent() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("10.00"), null, null,
                null, null, MenuItemStatus.ACTIVE,"Veg Burger");

        assertNull(event.getOutletId());
    }

    @Test
    void create_withoutChangedField_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION, null, null, null,
                        OUTLET_ID, null, null,"Veg Burger"));
    }

    @Test
    void create_withNegativePrice_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION, new BigDecimal("-1.00"),
                        null, null, OUTLET_ID, null, MenuItemStatus.ACTIVE,"Veg Burger"));
    }

    @Test
    void create_withInvalidRequiredId_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(null, CHAIN_ID, VERSION, BigDecimal.ONE, null, null,
                        OUTLET_ID, null, MenuItemStatus.ACTIVE,"Veg Burger"));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, 0L, VERSION, BigDecimal.ONE, null, null,
                        OUTLET_ID, null, MenuItemStatus.ACTIVE,"Veg Burger"));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, -1L, BigDecimal.ONE, null, null,
                        OUTLET_ID, null, MenuItemStatus.ACTIVE,"Veg Burger"));
    }

    @Test
    void create_withInvalidOutletId_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, null, null,
                        0L, null, MenuItemStatus.ACTIVE,"Veg Burger"));
    }

    @Test
    void create_withoutStatus_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION,
                        BigDecimal.ONE, null, null, OUTLET_ID, null, null,"Veg Burger"));
    }

    @Test
    void create_withBlankItemName_rejectsRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ITEM_ID, CHAIN_ID, VERSION,
                        BigDecimal.ONE, null, null, OUTLET_ID, null,
                        MenuItemStatus.ACTIVE, "   "));
    }

    @Test
    void create_eachCall_generatesANewEventId() {
        MenuItemChangedEvent first = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, null, null, OUTLET_ID, null, MenuItemStatus.ACTIVE,"Veg Burger");
        MenuItemChangedEvent second = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, BigDecimal.ONE, null, null, OUTLET_ID, null, MenuItemStatus.ACTIVE,"Veg Burger");

        assertNotEquals(first.getEventId(), second.getEventId());
    }

    @Test
    void create_scheduleChange_marksScheduleAndVersion() {
        MenuItemChangedEvent event = factory.create(
                ITEM_ID, CHAIN_ID, VERSION, null, null, null, OUTLET_ID,
                "corr-schedule", MenuItemStatus.ACTIVE, null, true, 2L, "UTC", Map.of());

        assertEquals(Boolean.TRUE, event.getScheduleChanged());
        assertEquals(2L, event.getScheduleVersion());
        assertEquals(OutboxEventType.SCHEDULE_CHANGED, event.getEventType());
        assertEquals(ITEM_ID + ":" + OUTLET_ID, event.getScheduleId());
        assertNull(event.getPrice());
        assertNull(event.getAvailability());
    }

    @Test
    void createModifierConfigurationChanged_createsFullModifierSnapshot() {
        EventModifierGroup extras = new EventModifierGroup(201L, "Choose extras", 0, 2, 0,
                List.of(new EventModifierOption(301L, "Extra cheese", BigDecimal.valueOf(20), 0),
                        new EventModifierOption(302L, "Bacon", BigDecimal.valueOf(50), 1)));

        MenuItemChangedEvent event = factory.createModifierConfigurationChanged(
                ITEM_ID, CHAIN_ID, VERSION, OUTLET_ID, "corr-modifiers", MenuItemStatus.ACTIVE,
                "Veg Burger", List.of(extras));

        assertEquals(OutboxEventType.MODIFIER_CONFIGURATION_CHANGED, event.getEventType());
        assertEquals(Boolean.TRUE, event.getModifierConfigurationChanged());
        assertEquals(1, event.getModifierGroups().size());
        assertEquals("Choose extras", event.getModifierGroups().getFirst().groupName());
        assertEquals(2, event.getModifierGroups().getFirst().options().size());
        assertNull(event.getPrice());
        assertNull(event.getAvailability());
        assertNull(event.getVisibility());
        assertEquals(Boolean.FALSE, event.getScheduleChanged());
        assertEquals(Boolean.FALSE, event.getSeasonalAvailabilityChanged());
    }

    @Test
    void createModifierConfigurationChanged_allowsEmptyGroupSnapshot() {
        MenuItemChangedEvent event = factory.createModifierConfigurationChanged(
                ITEM_ID, CHAIN_ID, VERSION, OUTLET_ID, null, MenuItemStatus.ACTIVE,
                "Veg Burger", List.of());

        assertEquals(Boolean.TRUE, event.getModifierConfigurationChanged());
        assertEquals(List.of(), event.getModifierGroups());
    }

    @Test
    void createModifierConfigurationChanged_rejectsNullGroupSnapshot() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.createModifierConfigurationChanged(
                        ITEM_ID, CHAIN_ID, VERSION, OUTLET_ID, null, MenuItemStatus.ACTIVE,
                        "Veg Burger", null));
    }

    @Test
    void createBundleConfigurationChanged_createsCompleteBundleSnapshot() {
        EventBundleSnapshot bundleSnapshot = new EventBundleSnapshot(ITEM_ID, "Burger Meal",
                BigDecimal.valueOf(299), List.of(
                        new EventBundleChild(201L, "Burger", MenuItemKind.PRODUCT, 1, 0,
                                MenuItemStatus.ACTIVE, true, true, List.of()),
                        new EventBundleChild(202L, "Fries", MenuItemKind.PRODUCT, 1, 1,
                                MenuItemStatus.ACTIVE, true, true, List.of())));

        MenuItemChangedEvent event = factory.createBundleConfigurationChanged(
                ITEM_ID, CHAIN_ID, VERSION, OUTLET_ID, "corr-bundle", MenuItemStatus.ACTIVE,
                "Burger Meal", bundleSnapshot);

        assertEquals(OutboxEventType.BUNDLE_CONFIGURATION_CHANGED, event.getEventType());
        assertEquals(Boolean.TRUE, event.getBundleConfigurationChanged());
        assertEquals(bundleSnapshot, event.getBundleSnapshot());
        assertEquals(Boolean.FALSE, event.getModifierConfigurationChanged());
        assertNull(event.getPrice());
    }

    @Test
    void createBundleConfigurationChanged_rejectsSnapshotForAnotherItem() {
        EventBundleSnapshot otherBundle = new EventBundleSnapshot(999L, "Other Meal", BigDecimal.ONE, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> factory.createBundleConfigurationChanged(
                        ITEM_ID, CHAIN_ID, VERSION, OUTLET_ID, null, MenuItemStatus.ACTIVE,
                        "Burger Meal", otherBundle));
    }
}
