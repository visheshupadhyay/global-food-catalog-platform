package com.vishesh.foodcatalog.catalog.event.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.persistence.OutboxEventJpaRepository;

@ExtendWith(MockitoExtension.class)
class OutboxMetricsPersistenceAdapterTest {
    @Mock
    private OutboxEventJpaRepository repository;

    @Test
    void readMetrics_combinesAllRepositoryValues() {
        when(repository.countByStatus(OutboxEventStatus.PENDING)).thenReturn(2L);
        when(repository.countByStatus(OutboxEventStatus.PROCESSING)).thenReturn(1L);
        when(repository.countByStatus(OutboxEventStatus.PUBLISHED)).thenReturn(50L);
        when(repository.countByStatus(OutboxEventStatus.DLQ)).thenReturn(3L);
        when(repository.sumRetryCount()).thenReturn(8L);
        when(repository.findOldestCreatedTimestamp(OutboxEventStatus.PENDING))
                .thenReturn(Optional.of(Instant.parse("2026-01-01T00:00:00Z")));

        OutboxMetrics metrics = new OutboxMetricsPersistenceAdapter(repository,
                Clock.fixed(Instant.parse("2026-01-01T00:05:00Z"), ZoneOffset.UTC)).readMetrics();

        assertEquals(2L, metrics.getPending());
        assertEquals(1L, metrics.getProcessing());
        assertEquals(50L, metrics.getPublished());
        assertEquals(3L, metrics.getDlq());
        assertEquals(8L, metrics.getRetryCount());
        assertEquals(300L, metrics.getOldestPendingAgeSeconds());
    }

    @Test
    void metrics_rejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new OutboxMetrics(-1L, 0L, 0L, 0L, 0L, 0L));
    }

    @Test
    void readMetrics_whenNoPendingEvent_returnsZeroAge() {
        when(repository.findOldestCreatedTimestamp(OutboxEventStatus.PENDING)).thenReturn(Optional.empty());
        when(repository.sumRetryCount()).thenReturn(0L);

        OutboxMetrics metrics = new OutboxMetricsPersistenceAdapter(repository,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)).readMetrics();

        assertEquals(0L, metrics.getOldestPendingAgeSeconds());
    }
}
