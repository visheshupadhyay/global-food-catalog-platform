package com.vishesh.foodcatalog.catalog.event.delivery;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

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

    @Value("${catalog.outbox.processing-timeout-seconds:60}")
    private long processingTimeoutSeconds = 60L;

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
        Instant now = clock.instant();
        outboxEventRepository.recoverStaleProcessingEvents(now.minusSeconds(processingTimeoutSeconds));
        Optional<OutboxEvent> event = outboxEventRepository.claimNextPendingEvent(now);
        // List<OutboxEvent> events = outboxEventRepository.findPendingEvents(clock.instant(), batchSize);
        if(event.isPresent()) {
            try {
                outboxDeliveryService.deliver(event.get());
            } catch (RuntimeException ignored) {
                // One malformed event must not prevent the rest of the batch from being attempted.
            }
        }
    }
}
