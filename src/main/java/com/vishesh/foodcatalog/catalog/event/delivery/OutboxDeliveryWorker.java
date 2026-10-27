package com.vishesh.foodcatalog.catalog.event.delivery;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;

/** Loads due outbox events in small batches and delegates each event for delivery. */
@Component
@ConditionalOnBean(OutboxDeliveryService.class)
public class OutboxDeliveryWorker {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxDeliveryService outboxDeliveryService;
    private final Clock clock;
    private final int batchSize;

    public OutboxDeliveryWorker(OutboxEventRepository outboxEventRepository,
            OutboxDeliveryService outboxDeliveryService,
            Clock clock,
            @Value("${catalog.outbox.batch-size:100}") int batchSize) {
        if (outboxEventRepository == null || outboxDeliveryService == null || clock == null) {
            throw new IllegalArgumentException("Worker dependencies cannot be null");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        this.outboxEventRepository = outboxEventRepository;
        this.outboxDeliveryService = outboxDeliveryService;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    /** Processes one bounded batch; scheduling can call this method later. */
    @Scheduled(fixedDelayString = "${catalog.outbox.poll-delay-ms}")
    public void runOnce() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(clock.instant(), batchSize);
        for (OutboxEvent event : events) {
            try {
                outboxDeliveryService.deliver(event);
            } catch (RuntimeException ignored) {
                // One malformed event must not prevent the rest of the batch from being attempted.
            }
        }
    }
}
