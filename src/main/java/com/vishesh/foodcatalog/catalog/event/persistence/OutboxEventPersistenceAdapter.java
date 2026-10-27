package com.vishesh.foodcatalog.catalog.event.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventNotFoundException;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

/** PostgreSQL adapter for the application-facing outbox repository port. */
/** Translates the application outbox port into PostgreSQL operations. */
@Repository
public class OutboxEventPersistenceAdapter implements OutboxEventRepository {
    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;

    public OutboxEventPersistenceAdapter(OutboxEventJpaRepository repository, OutboxEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        return mapper.toDomain(repository.save(mapper.toEntity(event)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findByEventId(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id cannot be null");
        }
        return repository.findByEventId(eventId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(Instant now, int limit) {
        if (now == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdTimestamp"));
        return repository.findDueBatch(OutboxEventStatus.PENDING, now, page)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(Long id, Instant publishedAt) {
        if (publishedAt == null) {
            throw new IllegalArgumentException("Published timestamp cannot be null");
        }

        OutboxEventEntity entity = findEntity(id);
        entity.setStatus(OutboxEventStatus.PUBLISHED);
        entity.setPublishedTimestamp(publishedAt);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void markFailed(Long id, String error, Instant nextAttemptAt, Instant failedAt) {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("Failure message cannot be blank");
        }
        if (failedAt == null) {
            throw new IllegalArgumentException("Failed timestamp cannot be null");
        }

        OutboxEventEntity entity = findEntity(id);
        entity.setStatus(OutboxEventStatus.FAILED);
        entity.setRetryCount(entity.getRetryCount() + 1);
        entity.setLastErrorMessage(error);
        entity.setNextRetryTime(nextAttemptAt);
        entity.setFailedTimestamp(failedAt);
        repository.save(entity);
    }

    private OutboxEventEntity findEntity(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Outbox database id must be positive");
        }
        return repository.findById(id).orElseThrow(() -> new OutboxEventNotFoundException(id));
    }

    @Override
    @Transactional
    public OutboxEvent recordFailure(Long id, OutboxEventStatus status, Long retryCount, String failureDetails,
            Instant nextAttemptAt, Instant failedTimestamp) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Outbox database id must be positive");
        }
        if (status != OutboxEventStatus.PENDING && status != OutboxEventStatus.DLQ) {
            throw new IllegalArgumentException("Failure status must be PENDING or DLQ");
        }
        if (failureDetails == null || failureDetails.isBlank()) {
            throw new IllegalArgumentException("Failure details cannot be blank");
        }
        if (retryCount == null || retryCount <= 0) {
            throw new IllegalArgumentException("Retry count must be positive");
        }
        if (failedTimestamp == null) {
            throw new IllegalArgumentException("Failed timestamp cannot be null");
        }
        if (status == OutboxEventStatus.DLQ && nextAttemptAt != null) {
            throw new IllegalArgumentException("DLQ events cannot have a next retry time");
        }
        OutboxEventEntity entity = findEntity(id);
        entity.setStatus(status);
        entity.setLastErrorMessage(failureDetails);
        entity.setNextRetryTime(nextAttemptAt);
        entity.setRetryCount(retryCount);
        entity.setFailedTimestamp(failedTimestamp);

        repository.save(entity);

        return mapper.toDomain(entity);
    }
}
