package com.vishesh.foodcatalog.catalog.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

@ExtendWith(MockitoExtension.class)
class OutboxEventReplayServiceTest {

    @Mock
    private OutboxEventRepository repository;

    private OutboxEventReplayService service;

    @BeforeEach
    void setUp() {
        service = new OutboxEventReplayService(repository);
    }

    @Test
    void replay_withNullEventId_rejectsRequestWithoutRepositoryCall() {
        assertThrows(IllegalArgumentException.class, () -> service.replay(null));

        verifyNoInteractions(repository);
    }

    @Test
    void replay_whenOriginalEventIsMissing_throwsNotFoundAndDoesNotSave() {
        UUID eventId = UUID.randomUUID();
        when(repository.findByEventId(eventId)).thenReturn(Optional.empty());

        assertThrows(OutboxEventNotFoundException.class, () -> service.replay(eventId));

        verify(repository).findByEventId(eventId);
        verify(repository, never()).save(any());
    }

    @Test
    void replay_createsNewPendingEventWithOriginalEventReference() {
        OutboxEvent original = existingFailedEvent();
        when(repository.findByEventId(original.getEventId())).thenReturn(Optional.of(original));
        when(repository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent replay = service.replay(original.getEventId());

        assertNotNull(replay);
        assertNotEquals(original.getEventId(), replay.getEventId());
        assertEquals(original.getEventId(), replay.getOriginalEventId());
        assertEquals(OutboxEventStatus.PENDING, replay.getStatus());
        assertEquals(0L, replay.getRetryCount());
        assertNull(replay.getId());
        assertNotNull(replay.getCreatedTimestamp());
    }

    @Test
    void replay_copiesEventAndAggregateData() {
        OutboxEvent original = existingFailedEvent();
        when(repository.findByEventId(original.getEventId())).thenReturn(Optional.of(original));
        when(repository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent replay = service.replay(original.getEventId());

        assertEquals(original.getEventType(), replay.getEventType());
        assertEquals(original.getKafkaKey(), replay.getKafkaKey());
        assertEquals(original.getPayload(), replay.getPayload());
        assertEquals(original.getAggregateId(), replay.getAggregateId());
        assertEquals(original.getAggregateVersion(), replay.getAggregateVersion());
    }

    @Test
    void replay_clearsPreviousDeliveryFailureState() {
        OutboxEvent original = existingFailedEvent();
        when(repository.findByEventId(original.getEventId())).thenReturn(Optional.of(original));
        when(repository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent replay = service.replay(original.getEventId());

        assertNull(replay.getNextRetryTime());
        assertNull(replay.getLastErrorMessage());
        assertNull(replay.getPublishedTimestamp());
        assertNull(replay.getFailedTimestamp());
    }

    @Test
    void replay_doesNotChangeOriginalEvent() {
        OutboxEvent original = existingFailedEvent();
        when(repository.findByEventId(original.getEventId())).thenReturn(Optional.of(original));
        when(repository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.replay(original.getEventId());

        assertEquals(OutboxEventStatus.FAILED, original.getStatus());
        assertEquals(2L, original.getRetryCount());
        assertEquals("broker unavailable", original.getLastErrorMessage());
    }

    @Test
    void replay_loadsOriginalBeforeSavingReplay() {
        OutboxEvent original = existingFailedEvent();
        when(repository.findByEventId(original.getEventId())).thenReturn(Optional.of(original));
        when(repository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.replay(original.getEventId());

        InOrder order = inOrder(repository);
        order.verify(repository).findByEventId(original.getEventId());
        order.verify(repository).save(any(OutboxEvent.class));
    }

    @Test
    void replay_savesTheNewEvent() {
        OutboxEvent original = existingFailedEvent();
        when(repository.findByEventId(original.getEventId())).thenReturn(Optional.of(original));
        when(repository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.replay(original.getEventId());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertEquals(OutboxEventStatus.PENDING, captor.getValue().getStatus());
    }

    private OutboxEvent existingFailedEvent() {
        return new OutboxEvent(
                55L,
                UUID.randomUUID(),
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                "101",
                "{\"itemId\":101,\"price\":12.50}",
                OutboxEventStatus.FAILED,
                2L,
                Instant.parse("2026-01-01T10:00:00Z"),
                "broker unavailable",
                Instant.parse("2026-01-01T09:00:00Z"),
                null,
                Instant.parse("2026-01-01T09:30:00Z"),
                101L,
                4L);
    }
}
