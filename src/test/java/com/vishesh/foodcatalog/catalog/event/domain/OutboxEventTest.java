package com.vishesh.foodcatalog.catalog.event.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OutboxEventTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORIGINAL_EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-01T10:00:05Z");
    private static final Instant FAILED_AT = Instant.parse("2026-01-01T10:00:06Z");
    private static final Instant NEXT_RETRY_AT = Instant.parse("2026-01-01T10:00:30Z");

    @Test
    void validEvent_preservesAllValues() {
        OutboxEvent event = new OutboxEvent(
                7L,
                EVENT_ID,
                ORIGINAL_EVENT_ID,
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                "{\"price\":12.00}",
                OutboxEventStatus.FAILED,
                2L,
                NEXT_RETRY_AT,
                "Kafka temporarily unavailable",
                CREATED_AT,
                PUBLISHED_AT,
                FAILED_AT,
                101L,
                4L);

        assertEquals(7L, event.getId());
        assertEquals(EVENT_ID, event.getEventId());
        assertEquals(ORIGINAL_EVENT_ID, event.getOriginalEventId());
        assertEquals(OutboxEventType.MENU_ITEM_CHANGED, event.getEventType());
        assertEquals("101", event.getKafkaKey());
        assertEquals("{\"price\":12.00}", event.getPayload());
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals(2L, event.getRetryCount());
        assertEquals(NEXT_RETRY_AT, event.getNextRetryTime());
        assertEquals("Kafka temporarily unavailable", event.getLastErrorMessage());
        assertEquals(CREATED_AT, event.getCreatedTimestamp());
        assertEquals(PUBLISHED_AT, event.getPublishedTimestamp());
        assertEquals(FAILED_AT, event.getFailedTimestamp());
        assertEquals(101L, event.getAggregateId());
        assertEquals(4L, event.getAggregateVersion());
    }

    @Test
    void newEvent_canHaveNoDatabaseIdOrReplayId() {
        OutboxEvent event = validEvent(null, null, OutboxEventStatus.PENDING, 0L, null, null, null);

        assertNull(event.getId());
        assertNull(event.getOriginalEventId());
        assertNull(event.getPublishedTimestamp());
        assertNull(event.getFailedTimestamp());
        assertNotNull(event.getCreatedTimestamp());
    }

    @Test
    void event_rejectsNullEventId() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, null, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, 101L, 0L));
    }

    @Test
    void event_rejectsNullEventType() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, null, "101", "{}", OutboxEventStatus.PENDING,
                0L, null, null, CREATED_AT, null, null, 101L, 0L));
    }

    @Test
    void event_rejectsBlankKafkaKey() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, " ", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, 101L, 0L));
    }

    @Test
    void event_rejectsBlankPayload() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", " ",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, 101L, 0L));
    }

    @Test
    void event_rejectsMissingOrNonPositiveAggregateId() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, null, 0L));
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, -1L, 0L));
    }

    @Test
    void event_rejectsMissingOrNegativeAggregateVersion() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, 101L, null));
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, CREATED_AT, null, null, 101L, -1L));
    }

    @Test
    void event_rejectsMissingOrNegativeRetryCount() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, null, null, null, CREATED_AT, null, null, 101L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, -1L, null, null, CREATED_AT, null, null, 101L, 0L));
    }

    @Test
    void event_rejectsNullStatus() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                null, 0L, null, null, CREATED_AT, null, null, 101L, 0L));
    }

    @Test
    void event_rejectsNullCreatedTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxEvent(
                null, EVENT_ID, null, OutboxEventType.MENU_ITEM_CHANGED, "101", "{}",
                OutboxEventStatus.PENDING, 0L, null, null, null, null, null, 101L, 0L));
    }

    private static OutboxEvent validEvent(Long id, UUID originalEventId, OutboxEventStatus status,
            Long retryCount, Instant nextRetryTime, Instant publishedAt, Instant failedAt) {
        return new OutboxEvent(
                id,
                EVENT_ID,
                originalEventId,
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                "{}",
                status,
                retryCount,
                nextRetryTime,
                null,
                CREATED_AT,
                publishedAt,
                failedAt,
                101L,
                0L);
    }
}
