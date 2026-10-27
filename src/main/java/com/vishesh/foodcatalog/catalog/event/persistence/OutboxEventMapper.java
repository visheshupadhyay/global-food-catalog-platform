package com.vishesh.foodcatalog.catalog.event.persistence;

import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;

/** Converts between the immutable domain event and its JPA row representation. */
@Component
public class OutboxEventMapper {

    public OutboxEventEntity toEntity(OutboxEvent domain) {
        if (domain == null) {
            throw new IllegalArgumentException("Outbox event cannot be null");
        }

        return new OutboxEventEntity(
                domain.getId(),
                domain.getEventId(),
                domain.getPayload(),
                domain.getCreatedTimestamp(),
                domain.getPublishedTimestamp(),
                domain.getFailedTimestamp(),
                domain.getOriginalEventId(),
                domain.getEventType(),
                domain.getKafkaKey(),
                domain.getAggregateId(),
                domain.getAggregateVersion(),
                domain.getStatus(),
                domain.getRetryCount(),
                domain.getNextRetryTime(),
                domain.getLastErrorMessage());
    }

    public OutboxEvent toDomain(OutboxEventEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Outbox event entity cannot be null");
        }

        return new OutboxEvent(
                entity.getId(),
                entity.getEventId(),
                entity.getOriginalEventId(),
                entity.getEventType(),
                entity.getKafkaKey(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextRetryTime(),
                entity.getLastErrorMessage(),
                entity.getCreatedTimestamp(),
                entity.getPublishedTimestamp(),
                entity.getFailedTimestamp(),
                entity.getAggregateId(),
                entity.getAggregateVersion());
    }
}
