package com.vishesh.foodcatalog.catalog.event.domain;

import java.time.Instant;
import java.util.UUID;

/** Immutable delivery record kept until Kafka accepts the event. */
public final class OutboxEvent {
    private final Long id;
    private final UUID eventId;
    private final UUID originalEventId;
    private final OutboxEventType eventType;
    private final String kafkaKey;
    private final String payload;
    private final Long aggregateId;
    private final Long aggregateVersion;
    private final OutboxEventStatus status;
    private final Long retryCount;
    private final Instant nextRetryTime;
    private final String lastErrorMessage;
    private final Instant createdTimestamp;
    private final Instant publishedTimestamp;
    private final Instant failedTimestamp;

    public OutboxEvent(Long id, UUID eventId, UUID originalEventId, OutboxEventType eventType, String kafkaKey,
            String payload, OutboxEventStatus status, Long retryCount, Instant nextRetryTime,
            String lastErrorMessage, Instant createdTimestamp, Instant publishedTimestamp,
            Instant failedTimestamp, Long aggregateId, Long aggregateVersion) {

        if (eventId == null) {
            throw new IllegalArgumentException("Event Id cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (kafkaKey == null || kafkaKey.isBlank()) {
            throw new IllegalArgumentException("Kafka key cannot be blank");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be blank");
        }
        if (aggregateId == null || aggregateId <= 0) {
            throw new IllegalArgumentException("Aggregate id must be positive");
        }
        if (aggregateVersion == null || aggregateVersion < 0) {
            throw new IllegalArgumentException("Aggregate version must be non-negative");
        }
        if (retryCount == null || retryCount < 0) {
            throw new IllegalArgumentException("Retry count must be non-negative");
        }
        if (status == null || createdTimestamp == null) {
            throw new IllegalArgumentException("Status and created timestamp cannot be null");
        }
        this.id = id;
        this.eventId = eventId;
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.kafkaKey = kafkaKey;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryTime = nextRetryTime;
        this.lastErrorMessage = lastErrorMessage;
        this.createdTimestamp = createdTimestamp;
        this.publishedTimestamp = publishedTimestamp;
        this.failedTimestamp = failedTimestamp;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOriginalEventId() {
        return originalEventId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public String getKafkaKey() {
        return kafkaKey;
    }

    public String getPayload() {
        return payload;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryTime() {
        return nextRetryTime;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Instant getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public Instant getFailedTimestamp() {
        return failedTimestamp;
    }

    
}
