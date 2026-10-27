package com.vishesh.foodcatalog.catalog.event.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

class OutboxEventMapperTest {

    private final OutboxEventMapper mapper = new OutboxEventMapper();

    @Test
    void toEntity_copiesDomainValues() {
        OutboxEvent domain = domainEvent();

        OutboxEventEntity entity = mapper.toEntity(domain);

        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getEventId(), entity.getEventId());
        assertEquals(domain.getOriginalEventId(), entity.getOriginalEventId());
        assertEquals(domain.getEventType(), entity.getEventType());
        assertEquals(domain.getKafkaKey(), entity.getKafkaKey());
        assertEquals(domain.getPayload(), entity.getPayload());
        assertEquals(domain.getAggregateId(), entity.getAggregateId());
        assertEquals(domain.getAggregateVersion(), entity.getAggregateVersion());
        assertEquals(domain.getStatus(), entity.getStatus());
        assertEquals(domain.getRetryCount(), entity.getRetryCount());
        assertEquals(domain.getNextRetryTime(), entity.getNextRetryTime());
        assertEquals(domain.getLastErrorMessage(), entity.getLastErrorMessage());
        assertEquals(domain.getCreatedTimestamp(), entity.getCreatedTimestamp());
        assertEquals(domain.getPublishedTimestamp(), entity.getPublishedTimestamp());
        assertEquals(domain.getFailedTimestamp(), entity.getFailedTimestamp());
    }

    @Test
    void toDomain_copiesEntityValues() {
        OutboxEventEntity entity = new OutboxEventEntity(
                7L,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "{}",
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                Instant.parse("2026-01-01T10:00:05Z"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                101L,
                4L,
                OutboxEventStatus.FAILED,
                2L,
                Instant.parse("2026-01-01T10:00:30Z"),
                "timeout");

        OutboxEvent domain = mapper.toDomain(entity);

        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getEventId(), domain.getEventId());
        assertEquals(entity.getOriginalEventId(), domain.getOriginalEventId());
        assertEquals(entity.getEventType(), domain.getEventType());
        assertEquals(entity.getKafkaKey(), domain.getKafkaKey());
        assertEquals(entity.getPayload(), domain.getPayload());
        assertEquals(entity.getAggregateId(), domain.getAggregateId());
        assertEquals(entity.getAggregateVersion(), domain.getAggregateVersion());
        assertEquals(entity.getStatus(), domain.getStatus());
        assertEquals(entity.getRetryCount(), domain.getRetryCount());
        assertEquals(entity.getNextRetryTime(), domain.getNextRetryTime());
        assertEquals(entity.getLastErrorMessage(), domain.getLastErrorMessage());
        assertEquals(entity.getCreatedTimestamp(), domain.getCreatedTimestamp());
        assertEquals(entity.getPublishedTimestamp(), domain.getPublishedTimestamp());
        assertEquals(entity.getFailedTimestamp(), domain.getFailedTimestamp());
    }

    @Test
    void toEntity_rejectsNullDomain() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(null));
    }

    @Test
    void toDomain_rejectsNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(null));
    }

    private static OutboxEvent domainEvent() {
        return new OutboxEvent(
                7L,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                "{}",
                OutboxEventStatus.FAILED,
                2L,
                Instant.parse("2026-01-01T10:00:30Z"),
                "timeout",
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                Instant.parse("2026-01-01T10:00:05Z"),
                101L,
                4L);
    }
}
