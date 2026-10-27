package com.vishesh.foodcatalog.catalog.event.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

/** Creates a fresh pending delivery attempt without changing the original event. */
@Service
public class OutboxEventReplayService {

    private final OutboxEventRepository repository;

    public OutboxEventReplayService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OutboxEvent replay(UUID originalEventId) {
        if (originalEventId == null) {
            throw new IllegalArgumentException("Original event id cannot be null");
        }

        OutboxEvent original = repository.findByEventId(originalEventId)
                .orElseThrow(() -> new OutboxEventNotFoundException(originalEventId.toString()));

        OutboxEvent replay = new OutboxEvent(
                null,
                UUID.randomUUID(),
                original.getEventId(),
                original.getEventType(),
                original.getKafkaKey(),
                original.getPayload(),
                OutboxEventStatus.PENDING,
                0L,
                null,
                null,
                Instant.now(),
                null,
                null,
                original.getAggregateId(),
                original.getAggregateVersion());

        return repository.save(replay);
    }
}
