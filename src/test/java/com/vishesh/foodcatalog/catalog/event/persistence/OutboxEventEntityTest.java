package com.vishesh.foodcatalog.catalog.event.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

class OutboxEventEntityTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORIGINAL_EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-01T10:00:05Z");
    private static final Instant FAILED_AT = Instant.parse("2026-01-01T10:00:06Z");
    private static final Instant NEXT_RETRY_AT = Instant.parse("2026-01-01T10:00:30Z");

    @Test
    void constructor_copiesEventValuesToEntity() {
        OutboxEventEntity entity = new OutboxEventEntity(
                7L,
                EVENT_ID,
                "{\"price\":12.00}",
                CREATED_AT,
                PUBLISHED_AT,
                FAILED_AT,
                ORIGINAL_EVENT_ID,
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                101L,
                4L,
                OutboxEventStatus.FAILED,
                2L,
                NEXT_RETRY_AT,
                "Kafka temporarily unavailable");

        assertEquals(7L, entity.getId());
        assertEquals("MENU_ITEM_CHANGED", entity.getName());
        assertEquals(EVENT_ID, entity.getEventId());
        assertEquals(ORIGINAL_EVENT_ID, entity.getOriginalEventId());
        assertEquals(OutboxEventType.MENU_ITEM_CHANGED, entity.getEventType());
        assertEquals("101", entity.getKafkaKey());
        assertEquals(101L, entity.getAggregateId());
        assertEquals(4L, entity.getAggregateVersion());
        assertEquals("{\"price\":12.00}", entity.getPayload());
        assertEquals(OutboxEventStatus.FAILED, entity.getStatus());
        assertEquals(2L, entity.getRetryCount());
        assertEquals(NEXT_RETRY_AT, entity.getNextRetryTime());
        assertEquals("Kafka temporarily unavailable", entity.getLastErrorMessage());
        assertEquals(CREATED_AT, entity.getCreatedTimestamp());
        assertEquals(PUBLISHED_AT, entity.getPublishedTimestamp());
        assertEquals(FAILED_AT, entity.getFailedTimestamp());
    }

    @Test
    void newEntity_canStartWithoutGeneratedDatabaseId() {
        OutboxEventEntity entity = validEntity(null, OutboxEventStatus.PENDING, 0L, null, null, null);

        assertNull(entity.getId());
        assertEquals("MENU_ITEM_CHANGED", entity.getName());
    }

    @Test
    void deliverySetters_updateMutablePersistenceState() {
        OutboxEventEntity entity = validEntity(null, OutboxEventStatus.PENDING, 0L, null, null, null);
        Instant publishedAt = Instant.parse("2026-01-01T10:01:00Z");
        Instant failedAt = Instant.parse("2026-01-01T10:01:05Z");

        entity.setStatus(OutboxEventStatus.FAILED);
        entity.setRetryCount(3L);
        entity.setNextRetryTime(NEXT_RETRY_AT);
        entity.setLastErrorMessage("timeout");
        entity.setPublishedTimestamp(publishedAt);
        entity.setFailedTimestamp(failedAt);

        assertEquals(OutboxEventStatus.FAILED, entity.getStatus());
        assertEquals(3L, entity.getRetryCount());
        assertEquals(NEXT_RETRY_AT, entity.getNextRetryTime());
        assertEquals("timeout", entity.getLastErrorMessage());
        assertEquals(publishedAt, entity.getPublishedTimestamp());
        assertEquals(failedAt, entity.getFailedTimestamp());
    }

    @Test
    void eventType_populatesLegacyNameColumn() {
        OutboxEventEntity entity = validEntity(null, OutboxEventStatus.PENDING, 0L, null, null, null);

        assertEquals(OutboxEventType.MENU_ITEM_CHANGED.name(), entity.getName());
    }

    private static OutboxEventEntity validEntity(Long id, OutboxEventStatus status, Long retryCount,
            Instant nextRetryTime, Instant publishedAt, Instant failedAt) {
        return new OutboxEventEntity(
                id,
                EVENT_ID,
                "{}",
                CREATED_AT,
                publishedAt,
                failedAt,
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                101L,
                0L,
                status,
                retryCount,
                nextRetryTime,
                null);
    }
}
