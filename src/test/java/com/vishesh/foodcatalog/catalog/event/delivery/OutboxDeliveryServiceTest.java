package com.vishesh.foodcatalog.catalog.event.delivery;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventPublisher;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

@ExtendWith(MockitoExtension.class)
class OutboxDeliveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private OutboxEventPublisher publisher;

    private OutboxDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new OutboxDeliveryService(
                repository,
                publisher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                5L);
    }

    @Test
    void deliver_whenPublishSucceeds_marksEventPublished() {
        OutboxEvent event = event(0L);

        service.deliver(event);

        verify(publisher).publish(event);
        verify(repository).markPublished(event.getId(), NOW);
        verify(repository, never()).recordFailure(
                eq(event.getId()), eq(OutboxEventStatus.PENDING), eq(1L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deliver_whenPublishFailsBeforeLimit_recordsPendingRetry() {
        OutboxEvent event = event(0L);
        doThrow(new RuntimeException("Kafka unavailable")).when(publisher).publish(event);

        service.deliver(event);

        verify(repository).recordFailure(
                event.getId(), OutboxEventStatus.PENDING, 1L,
                "Kafka unavailable", NOW.plusSeconds(60), NOW);
        verify(repository, never()).markPublished(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deliver_whenLastRetryFails_movesEventToDlq() {
        OutboxEvent event = event(4L);
        doThrow(new RuntimeException("Kafka unavailable")).when(publisher).publish(event);

        service.deliver(event);

        verify(repository).recordFailure(
                event.getId(), OutboxEventStatus.DLQ, 5L,
                "Kafka unavailable", null, NOW);
    }

    @Test
    void deliver_whenPublisherExceptionHasNoMessage_usesExceptionTypeAsDetails() {
        OutboxEvent event = event(0L);
        doThrow(new IllegalStateException()).when(publisher).publish(event);

        service.deliver(event);

        verify(repository).recordFailure(
                eq(event.getId()), eq(OutboxEventStatus.PENDING), eq(1L),
                eq("IllegalStateException"), eq(NOW.plusSeconds(60)), eq(NOW));
    }

    @Test
    void deliver_whenEventIsNull_rejectsInput() {
        assertThrows(IllegalArgumentException.class, () -> service.deliver(null));

        verifyNoInteractions(repository, publisher);
    }

    private OutboxEvent event(long retryCount) {
        return new OutboxEvent(
                101L,
                UUID.randomUUID(),
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                "menu-item-701",
                "{\"itemId\":701}",
                OutboxEventStatus.PENDING,
                retryCount,
                NOW.plusSeconds(60),
                null,
                NOW,
                null,
                null,
                701L,
                1L);
    }
}
