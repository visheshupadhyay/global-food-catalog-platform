package com.vishesh.foodcatalog.catalog.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.vishesh.foodcatalog.catalog.domain.MenuItemKind;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleChild;
import com.vishesh.foodcatalog.catalog.event.domain.EventBundleSnapshot;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;
import com.vishesh.foodcatalog.catalog.event.domain.OutletMenuInitializedEvent;

class OutboxEventFactoryTest {
    @Test
    void create_outletMenuInitializedEvent_usesOutletAsAggregate() {
        OutletMenuInitializedEvent event = new OutletMenuInitializedEventFactory().create(
                30L, 10L, 20L, 4L, "Asia/Kolkata", "corr-outlet", List.of());

        var outbox = new OutboxEventFactory().create(event, "{\"eventType\":\"OUTLET_MENU_INITIALIZED\"}");

        assertEquals(OutboxEventType.OUTLET_MENU_INITIALIZED, outbox.getEventType());
        assertEquals("30", outbox.getKafkaKey());
        assertEquals(30L, outbox.getAggregateId());
        assertEquals(4L, outbox.getAggregateVersion());
        assertEquals(OutboxEventStatus.PENDING, outbox.getStatus());
    }

    @Test
    void create_activationEvent_createsPendingOutboxRow() {
        var event = new MenuItemChangedEventFactory().createMenuItemActivated(
                101L, 20L, 4L, BigDecimal.valueOf(149), true, true,
                30L, "corr-activation", MenuItemStatus.ACTIVE, "Burger");

        var outbox = new OutboxEventFactory().create(event, "{\"eventType\":\"MENU_ITEM_ACTIVATED\"}");

        assertEquals(OutboxEventType.MENU_ITEM_ACTIVATED, outbox.getEventType());
        assertEquals("30", outbox.getKafkaKey());
        assertEquals(OutboxEventStatus.PENDING, outbox.getStatus());
        assertEquals(101L, outbox.getAggregateId());
        assertEquals(4L, outbox.getAggregateVersion());
    }

    @Test
    void create_scheduleEvent_createsPendingOutboxRow() {
        var event = new MenuItemChangedEventFactory().create(
                101L, 20L, 4L, null, null, null, 30L,
                "corr-1", MenuItemStatus.ACTIVE, null, true, 2L, "UTC", Map.of());

        var outbox = new OutboxEventFactory().create(event, "{\"scheduleChanged\":true}");

        assertNotNull(outbox.getEventId());
        assertEquals(101L, outbox.getAggregateId());
        assertEquals("30", outbox.getKafkaKey());
        assertEquals(4L, outbox.getAggregateVersion());
        assertEquals(OutboxEventStatus.PENDING, outbox.getStatus());
        assertEquals("{\"scheduleChanged\":true}", outbox.getPayload());
    }

    @Test
    void create_bundleEvent_createsPendingOutboxRowWithBundlePayload() {
        EventBundleSnapshot snapshot = new EventBundleSnapshot(101L, "Burger Meal", BigDecimal.valueOf(299),
                List.of(new EventBundleChild(102L, "Burger", MenuItemKind.PRODUCT, 1, 0,
                        MenuItemStatus.ACTIVE, true, true, List.of())));
        var event = new MenuItemChangedEventFactory().createBundleConfigurationChanged(
                101L, 20L, 4L, 30L, "corr-bundle", MenuItemStatus.ACTIVE, "Burger Meal", snapshot);
        String payload = "{\"bundleConfigurationChanged\":true,\"bundleSnapshot\":{\"menuItemId\":101}}";

        var outbox = new OutboxEventFactory().create(event, payload);

        assertEquals(OutboxEventStatus.PENDING, outbox.getStatus());
        assertEquals(event.getEventId(), outbox.getEventId());
        assertEquals(101L, outbox.getAggregateId());
        assertEquals("30", outbox.getKafkaKey());
        assertEquals(4L, outbox.getAggregateVersion());
        assertEquals(payload, outbox.getPayload());
    }
}
