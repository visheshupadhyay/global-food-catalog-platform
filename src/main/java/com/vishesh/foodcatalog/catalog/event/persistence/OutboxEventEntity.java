package com.vishesh.foodcatalog.catalog.event.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Database-shaped representation of an outbox event. */
@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* V1 requires this legacy column; populate it from the event type temporarily. */
    @Column(name = "name", nullable = false)
    private String name;

    /* Legacy ownership columns remain nullable during the transition. */
    @Column(name = "menu_group_id")
    private Long menuGroupId;

    @Column(name = "outlet_id")
    private Long outletId;

    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private OutboxEventType eventType;

    @Column(name = "kafka_key")
    private String kafkaKey;

    @Column(name = "aggregate_id")
    private Long aggregateId;

    @Column(name = "aggregate_version")
    private Long aggregateVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OutboxEventStatus status;

    @Column(name = "retry_count")
    private Long retryCount;

    @Column(name = "next_attempt_at")
    private Instant nextRetryTime;

    @Column(name = "last_error")
    private String lastErrorMessage;

    @Column(name = "created_at")
    private Instant createdTimestamp;

    @Column(name = "published_at")
    private Instant publishedTimestamp;

    @Column(name = "failed_at")
    private Instant failedTimestamp;

    @Column(name = "original_event_id")
    private UUID originalEventId;

    protected OutboxEventEntity() {
        // Required by JPA.
    }

    public OutboxEventEntity(Long id, UUID eventId, String payload, Instant createdTimestamp,
            Instant publishedTimestamp, Instant failedTimestamp, UUID originalEventId,
            OutboxEventType eventType, String kafkaKey, Long aggregateId, Long aggregateVersion,
            OutboxEventStatus status, Long retryCount, Instant nextRetryTime, String lastErrorMessage) {
        this.id = id;
        this.name = eventType == null ? null : eventType.name();
        this.eventId = eventId;
        this.payload = payload;
        this.createdTimestamp = createdTimestamp;
        this.publishedTimestamp = publishedTimestamp;
        this.failedTimestamp = failedTimestamp;
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.kafkaKey = kafkaKey;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryTime = nextRetryTime;
        this.lastErrorMessage = lastErrorMessage;
    }



    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getMenuGroupId() {
        return menuGroupId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public String getKafkaKey() {
        return kafkaKey;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public String getPayload() {
        return payload;
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

    public UUID getOriginalEventId() {
        return originalEventId;
    }

    public void setStatus(OutboxEventStatus status) {
        this.status = status;
    }

    public void setRetryCount(Long retryCount) {
        this.retryCount = retryCount;
    }

    public void setNextRetryTime(Instant nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public void setPublishedTimestamp(Instant publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
    }

    public void setFailedTimestamp(Instant failedTimestamp) {
        this.failedTimestamp = failedTimestamp;
    }
}
