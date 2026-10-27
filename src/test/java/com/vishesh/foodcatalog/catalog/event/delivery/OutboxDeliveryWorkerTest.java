package com.vishesh.foodcatalog.catalog.event.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

@ExtendWith(MockitoExtension.class)
class OutboxDeliveryWorkerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private OutboxDeliveryService deliveryService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void runOnce_loadsPendingEventsUsingCurrentTimeAndBatchSize() {
        OutboxDeliveryWorker worker = worker(25);
        when(repository.findPendingEvents(NOW, 25)).thenReturn(List.of());

        worker.runOnce();

        verify(repository).findPendingEvents(NOW, 25);
        verifyNoInteractions(deliveryService);
    }

    @Test
    void runOnce_deliversEveryLoadedEvent() {
        OutboxDeliveryWorker worker = worker(100);
        OutboxEvent first = event(1L);
        OutboxEvent second = event(2L);
        when(repository.findPendingEvents(NOW, 100)).thenReturn(List.of(first, second));

        worker.runOnce();

        verify(deliveryService).deliver(first);
        verify(deliveryService).deliver(second);
    }

    @Test
    void runOnce_whenNoPendingEvents_doesNotDeliverAnything() {
        OutboxDeliveryWorker worker = worker(100);
        when(repository.findPendingEvents(NOW, 100)).thenReturn(List.of());

        worker.runOnce();

        verifyNoInteractions(deliveryService);
    }

    @Test
    void runOnce_whenOneEventFails_continuesWithRemainingEvents() {
        OutboxDeliveryWorker worker = worker(100);
        OutboxEvent first = event(1L);
        OutboxEvent second = event(2L);
        when(repository.findPendingEvents(NOW, 100)).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("unexpected failure"))
                .when(deliveryService).deliver(first);

        worker.runOnce();

        verify(deliveryService).deliver(first);
        verify(deliveryService).deliver(second);
    }

    @Test
    void runOnce_isConfiguredForFixedDelayScheduling() throws NoSuchMethodException {
        Scheduled scheduled = OutboxDeliveryWorker.class
                .getMethod("runOnce")
                .getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("${catalog.outbox.poll-delay-ms}", scheduled.fixedDelayString());
    }

    @Test
    void constructor_rejectsZeroBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> worker(0));
    }

    @Test
    void constructor_rejectsNegativeBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> worker(-1));
    }

    @Test
    void constructor_rejectsNullRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> new OutboxDeliveryWorker(null, deliveryService, clock, 100));
    }

    @Test
    void constructor_rejectsNullDeliveryService() {
        assertThrows(IllegalArgumentException.class,
                () -> new OutboxDeliveryWorker(repository, null, clock, 100));
    }

    @Test
    void constructor_rejectsNullClock() {
        assertThrows(IllegalArgumentException.class,
                () -> new OutboxDeliveryWorker(repository, deliveryService, null, 100));
    }

    private OutboxDeliveryWorker worker(int batchSize) {
        return new OutboxDeliveryWorker(repository, deliveryService, clock, batchSize);
    }

    private OutboxEvent event(Long id) {
        return new OutboxEvent(
                id,
                UUID.randomUUID(),
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                "menu-item-" + id,
                "{\"itemId\":" + id + "}",
                OutboxEventStatus.PENDING,
                0L,
                null,
                null,
                NOW,
                null,
                null,
                id,
                1L);
    }
}
