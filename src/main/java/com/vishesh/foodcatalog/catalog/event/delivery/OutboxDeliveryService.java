package com.vishesh.foodcatalog.catalog.event.delivery;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventPublisher;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

/** Tries delivery and records success, retry, or final DLQ failure. */
@Service
@ConditionalOnBean(OutboxEventPublisher.class)
public class OutboxDeliveryService {
	private final OutboxEventRepository outboxEventRepository;
	private final OutboxEventPublisher outboxEventPublisher;
	private final Clock clock;
	private final Long maxRetries;

	public OutboxDeliveryService(OutboxEventRepository outboxEventRepository,
			@Qualifier("kafkaOutboxEventPublisher") OutboxEventPublisher outboxEventPublisher, Clock clock,
			@Value("${catalog.outbox.max-retries}") Long maxRetries) {

		if (outboxEventRepository == null || outboxEventPublisher == null || clock == null) {
			throw new IllegalArgumentException("Delivery dependencies cannot be null");
		}
		if (maxRetries == null || maxRetries <= 0) {
			throw new IllegalArgumentException("Maximum retries must be positive");
		}
		this.outboxEventRepository = outboxEventRepository;
		this.outboxEventPublisher = outboxEventPublisher;
		this.clock = clock;
		this.maxRetries = maxRetries;
	}

	public void deliver(OutboxEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("Outbox event cannot be null");
		}

		try {
			outboxEventPublisher.publish(event);
			outboxEventRepository.markPublished(event.getId(), clock.instant());
		} catch (RuntimeException exception) {
			recordDeliveryFailure(event, exception);
		}
	}

	/**
	 * Records the same retry-or-DLQ transition that delivery used before this
	 * extraction.
	 */
	private void recordDeliveryFailure(OutboxEvent event, RuntimeException exception) {
		long retryCount = event.getRetryCount() + 1;
		boolean shouldRetry = retryCount < maxRetries;
		OutboxEventStatus status = shouldRetry ? OutboxEventStatus.PENDING : OutboxEventStatus.DLQ;
		Instant nextRetryAt = shouldRetry ? clock.instant().plus(retryDelay(retryCount)) : null;
		String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();

		outboxEventRepository.recordFailure(event.getId(), status, retryCount, message, nextRetryAt, clock.instant());
	}

	private Duration retryDelay(long retryCount) {
		long minutes = 1L << Math.min(retryCount - 1, 10);
		return Duration.ofMinutes(minutes);
	}
}
