package com.vishesh.foodcatalog.catalog.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.vishesh.foodcatalog.catalog.domain.MenuItemStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

class OutboxEventFactoryTest {
    @Test
    void create_scheduleEvent_createsPendingOutboxRow() {
        var event = new MenuItemChangedEventFactory().create(
                101L, 20L, 4L, null, null, null, 30L,
                "corr-1", MenuItemStatus.ACTIVE, null, true, 2L, "UTC", Map.of());

        var outbox = new OutboxEventFactory().create(event, "{\"scheduleChanged\":true}");

        assertNotNull(outbox.getEventId());
        assertEquals(101L, outbox.getAggregateId());
        assertEquals(4L, outbox.getAggregateVersion());
        assertEquals(OutboxEventStatus.PENDING, outbox.getStatus());
        assertEquals("{\"scheduleChanged\":true}", outbox.getPayload());
    }
}
