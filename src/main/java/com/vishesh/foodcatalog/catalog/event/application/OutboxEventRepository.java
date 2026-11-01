package com.vishesh.foodcatalog.catalog.event.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

/** The application’s small storage contract for outbox events. */
/** Application port for durable outbox reads and delivery-state changes. */
public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findByEventId(UUID eventId);

    List<OutboxEvent> findPendingEvents(Instant now, int limit);

    void markPublished(Long id, Instant publishedAt);

    void markFailed(Long id, String error, Instant nextAttemptAt, Instant failedAt);

    Optional<OutboxEvent> claimNextPendingEvent(Instant now);

    int recoverStaleProcessingEvents(Instant cutoffTime);

    OutboxEvent recordFailure(Long id, OutboxEventStatus status, Long retryCount, String failureDetails, Instant nextAttemptAt, Instant failedTimestamp);
}
