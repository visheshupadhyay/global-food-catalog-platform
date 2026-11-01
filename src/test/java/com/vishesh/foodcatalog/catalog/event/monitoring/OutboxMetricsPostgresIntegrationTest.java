package com.vishesh.foodcatalog.catalog.event.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;
import com.vishesh.foodcatalog.catalog.event.persistence.OutboxEventEntity;
import com.vishesh.foodcatalog.catalog.event.persistence.OutboxEventJpaRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutboxMetricsPostgresIntegrationTest {

    @Autowired
    private OutboxEventJpaRepository repository;

    @Autowired
    private OutboxMetricsReader metricsReader;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void readMetrics_readsCountsRetryTotalAndOldestPendingAge() {
        Instant now = Instant.now();
        repository.save(event(OutboxEventStatus.PENDING, now.minusSeconds(300), 2L));
        repository.save(event(OutboxEventStatus.PENDING, now.minusSeconds(120), 1L));
        repository.save(event(OutboxEventStatus.PROCESSING, now.minusSeconds(90), 0L));
        repository.save(event(OutboxEventStatus.PUBLISHED, now.minusSeconds(80), 3L));
        repository.save(event(OutboxEventStatus.PUBLISHED, now.minusSeconds(70), 1L));
        repository.save(event(OutboxEventStatus.PUBLISHED, now.minusSeconds(60), 0L));
        repository.save(event(OutboxEventStatus.DLQ, now.minusSeconds(50), 1L));

        OutboxMetrics metrics = metricsReader.readMetrics();

        assertEquals(2L, metrics.getPending());
        assertEquals(1L, metrics.getProcessing());
        assertEquals(3L, metrics.getPublished());
        assertEquals(1L, metrics.getDlq());
        assertEquals(8L, metrics.getRetryCount());
        assertTrue(metrics.getOldestPendingAgeSeconds() >= 299L);
        assertTrue(metrics.getOldestPendingAgeSeconds() <= 305L);
    }

    private OutboxEventEntity event(OutboxEventStatus status, Instant createdAt, Long retryCount) {
        return new OutboxEventEntity(
                null,
                UUID.randomUUID(),
                "{}",
                createdAt,
                null,
                null,
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                101L,
                1L,
                status,
                retryCount,
                null,
                null);
    }
}
