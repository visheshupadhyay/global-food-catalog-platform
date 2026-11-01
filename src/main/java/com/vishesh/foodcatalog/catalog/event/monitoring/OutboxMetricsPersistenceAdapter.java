package com.vishesh.foodcatalog.catalog.event.monitoring;

import com.vishesh.foodcatalog.catalog.event.persistence.OutboxEventJpaRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
/** Builds outbox metrics from database counts and timestamps. */
public class OutboxMetricsPersistenceAdapter implements  OutboxMetricsReader{
    private final OutboxEventJpaRepository repository;
    private final Clock clock;

    public OutboxMetricsPersistenceAdapter(OutboxEventJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OutboxMetrics readMetrics() {

        return new OutboxMetrics(
                repository.countByStatus(OutboxEventStatus.PENDING),
                repository.countByStatus(OutboxEventStatus.PROCESSING),
                repository.countByStatus(OutboxEventStatus.PUBLISHED),
                repository.countByStatus(OutboxEventStatus.DLQ),
                repository.sumRetryCount(),
                repository.findOldestCreatedTimestamp(OutboxEventStatus.PENDING)
                        .map(created -> Math.max(0L, Duration.between(created, clock.instant()).getSeconds()))
                        .orElse(0L));
    }
}
