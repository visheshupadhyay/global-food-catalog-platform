package com.vishesh.foodcatalog.catalog.event.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventNotFoundException;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

class OutboxEventPersistenceAdapterTest {

    @Mock
    private OutboxEventJpaRepository repository;

    private OutboxEventMapper mapper;
    private OutboxEventPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new OutboxEventMapper();
        adapter = new OutboxEventPersistenceAdapter(repository, mapper);
    }

    @Test
    void save_mapsAndReturnsPersistedEvent() {
        OutboxEvent input = event(null, OutboxEventStatus.PENDING, 0L, null);
        OutboxEventEntity savedEntity = entity(9L, input.getEventId(), OutboxEventStatus.PENDING, 0L, null);
        when(repository.save(any(OutboxEventEntity.class))).thenReturn(savedEntity);

        OutboxEvent result = adapter.save(input);

        assertEquals(9L, result.getId());
        assertEquals(input.getEventId(), result.getEventId());
        assertEquals(input.getPayload(), result.getPayload());
        verify(repository).save(any(OutboxEventEntity.class));
    }

    @Test
    void findByEventId_whenFound_returnsMappedDomain() {
        UUID eventId = UUID.randomUUID();
        OutboxEventEntity entity = entity(9L, eventId, OutboxEventStatus.PENDING, 0L, null);
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(entity));

        Optional<OutboxEvent> result = adapter.findByEventId(eventId);

        assertEquals(eventId, result.orElseThrow().getEventId());
        verify(repository).findByEventId(eventId);
    }

    @Test
    void findByEventId_whenMissing_returnsEmpty() {
        UUID eventId = UUID.randomUUID();
        when(repository.findByEventId(eventId)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), adapter.findByEventId(eventId));
        verify(repository).findByEventId(eventId);
    }

    @Test
    void findByEventId_rejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> adapter.findByEventId(null));
        verify(repository, never()).findByEventId(any());
    }

    @Test
    void findPendingEvents_forwardsStatusTimeAndLimit() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        when(repository.findDueBatch(any(), any(), any())).thenReturn(List.of());

        assertEquals(List.of(), adapter.findPendingEvents(now, 25));

        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repository).findDueBatch(
                org.mockito.Mockito.eq(OutboxEventStatus.PENDING),
                org.mockito.Mockito.eq(now),
                pageCaptor.capture());
        assertEquals(25, pageCaptor.getValue().getPageSize());
        assertEquals(0, pageCaptor.getValue().getPageNumber());
    }

    @Test
    void findPendingEvents_rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> adapter.findPendingEvents(null, 10));
        assertThrows(IllegalArgumentException.class, () -> adapter.findPendingEvents(Instant.now(), 0));
        assertThrows(IllegalArgumentException.class, () -> adapter.findPendingEvents(Instant.now(), -1));
        verify(repository, never()).findDueBatch(any(), any(), any());
    }

    @Test
    void markPublished_updatesStatusAndTimestamp() {
        OutboxEventEntity entity = entity(9L, UUID.randomUUID(), OutboxEventStatus.PENDING, 0L, null);
        Instant publishedAt = Instant.parse("2026-01-01T12:00:00Z");
        when(repository.findById(9L)).thenReturn(Optional.of(entity));

        adapter.markPublished(9L, publishedAt);

        assertEquals(OutboxEventStatus.PUBLISHED, entity.getStatus());
        assertEquals(publishedAt, entity.getPublishedTimestamp());
        verify(repository).save(entity);
    }

    @Test
    void markFailed_updatesFailureDetailsAndIncrementsRetryCount() {
        OutboxEventEntity entity = entity(9L, UUID.randomUUID(), OutboxEventStatus.PENDING, 1L, null);
        Instant nextAttemptAt = Instant.parse("2026-01-01T12:01:00Z");
        Instant failedAt = Instant.parse("2026-01-01T12:00:00Z");
        when(repository.findById(9L)).thenReturn(Optional.of(entity));

        adapter.markFailed(9L, "timeout", nextAttemptAt, failedAt);

        assertEquals(OutboxEventStatus.FAILED, entity.getStatus());
        assertEquals(2L, entity.getRetryCount());
        assertEquals("timeout", entity.getLastErrorMessage());
        assertEquals(nextAttemptAt, entity.getNextRetryTime());
        assertEquals(failedAt, entity.getFailedTimestamp());
        verify(repository).save(entity);
    }

    @Test
    void stateUpdates_whenEntityMissing_throwNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(OutboxEventNotFoundException.class,
                () -> adapter.markPublished(9L, Instant.now()));
        assertThrows(OutboxEventNotFoundException.class,
                () -> adapter.markFailed(9L, "timeout", null, Instant.now()));
        verify(repository, never()).save(any());
    }

    @Test
    void stateUpdates_rejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> adapter.markPublished(null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> adapter.markPublished(9L, null));
        assertThrows(IllegalArgumentException.class, () -> adapter.markFailed(null, "timeout", null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> adapter.markFailed(9L, " ", null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> adapter.markFailed(9L, "timeout", null, null));
        verify(repository, never()).findById(any());
    }

    private static OutboxEvent event(Long id, OutboxEventStatus status, Long retryCount,
            UUID originalEventId) {
        return new OutboxEvent(id, UUID.randomUUID(), originalEventId,
                OutboxEventType.MENU_ITEM_CHANGED, "101", "{}", status, retryCount,
                null, null, Instant.parse("2026-01-01T10:00:00Z"), null, null, 101L, 0L);
    }

    private static OutboxEventEntity entity(Long id, UUID eventId, OutboxEventStatus status,
            Long retryCount, UUID originalEventId) {
        return new OutboxEventEntity(id, eventId, "{}",
                Instant.parse("2026-01-01T10:00:00Z"), null, null, originalEventId,
                OutboxEventType.MENU_ITEM_CHANGED, "101", 101L, 0L, status,
                retryCount, null, null);
    }
}
