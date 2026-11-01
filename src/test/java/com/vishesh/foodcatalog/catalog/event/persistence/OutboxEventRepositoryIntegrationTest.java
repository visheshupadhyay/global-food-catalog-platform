package com.vishesh.foodcatalog.catalog.event.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.vishesh.foodcatalog.TestcontainersConfiguration;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventReplayService;
import com.vishesh.foodcatalog.catalog.event.application.OutboxEventRepository;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutboxEventRepositoryIntegrationTest {

        @Autowired
        private OutboxEventJpaRepository repository;

        @Autowired
        private OutboxEventRepository outboxEventRepository;

        @Autowired
        private OutboxEventReplayService service;

        @AfterEach
        void cleanUp() {
                repository.deleteAll();
        }

        @Test
        void saveAndFindByEventId_returnsPersistedEvent() {
                OutboxEventEntity saved = repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING, Instant.now(), null));

                OutboxEventEntity found = repository.findByEventId(saved.getEventId()).orElseThrow();

                assertEquals(saved.getId(), found.getId());
                assertEquals(saved.getEventId(), found.getEventId());
                assertEquals(saved.getEventType(), found.getEventType());
                assertEquals(saved.getPayload(), found.getPayload());
                assertEquals(saved.getAggregateId(), found.getAggregateId());
        }

        @Test
        void findByEventId_whenUnknown_returnsEmpty() {
                assertTrue(repository.findByEventId(UUID.randomUUID()).isEmpty());
        }

        @Test
        void existsByEventId_reflectsDatabasePresence() {
                UUID eventId = UUID.randomUUID();
                repository.save(event(eventId, OutboxEventStatus.PENDING, Instant.now(), null));

                assertTrue(repository.existsByEventId(eventId));
                assertFalse(repository.existsByEventId(UUID.randomUUID()));
        }

        @Test
        @Transactional
        void findDueBatch_returnsOnlyDueEventsInOldestFirstOrderWithinLimit() {
                Instant now = Instant.parse("2026-01-01T12:00:00Z");
                OutboxEventEntity oldest = repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING,
                                Instant.parse("2026-01-01T10:00:00Z"), null));
                OutboxEventEntity dueWithRetry = repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING,
                                Instant.parse("2026-01-01T11:00:00Z"), Instant.parse("2026-01-01T11:59:00Z")));
                repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING,
                                Instant.parse("2026-01-01T11:30:00Z"), Instant.parse("2026-01-01T12:01:00Z")));
                repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PUBLISHED,
                                Instant.parse("2026-01-01T09:00:00Z"), null));
                repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.FAILED,
                                Instant.parse("2026-01-01T09:30:00Z"), null));

                List<OutboxEventEntity> due = repository.findDueBatch(
                                OutboxEventStatus.PENDING,
                                now,
                                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "createdTimestamp")));

                assertEquals(2, due.size());
                assertEquals(oldest.getEventId(), due.get(0).getEventId());
                assertEquals(dueWithRetry.getEventId(), due.get(1).getEventId());
        }

        @Test
        void originalEventId_isPersistedAndReadBack() {
                UUID originalEventId = UUID.randomUUID();
                OutboxEventEntity saved = repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING, Instant.now(), null, originalEventId));

                OutboxEventEntity found = repository.findByEventId(saved.getEventId()).orElseThrow();

                assertEquals(originalEventId, found.getOriginalEventId());
        }

        @Test
        void replay_persistsFreshPendingEventLinkedToOriginal() {
                OutboxEvent original = existingFailedEvent();
                OutboxEventEntity originalEntity = repository.save(new OutboxEventEntity(
                                null,
                                original.getEventId(),
                                original.getPayload(),
                                original.getCreatedTimestamp(),
                                original.getPublishedTimestamp(),
                                original.getFailedTimestamp(),
                                original.getOriginalEventId(),
                                original.getEventType(),
                                original.getKafkaKey(),
                                original.getAggregateId(),
                                original.getAggregateVersion(),
                                original.getStatus(),
                                original.getRetryCount(),
                                original.getNextRetryTime(),
                                original.getLastErrorMessage()));

                OutboxEvent replayed = service.replay(original.getEventId());

                assertNotNull(replayed.getId());
                assertNotEquals(original.getEventId(), replayed.getEventId());
                assertEquals(original.getEventId(), replayed.getOriginalEventId());
                assertEquals(OutboxEventStatus.PENDING, replayed.getStatus());
                assertEquals(0L, replayed.getRetryCount());
                assertNull(replayed.getNextRetryTime());
                assertNull(replayed.getLastErrorMessage());
                assertNull(replayed.getPublishedTimestamp());
                assertNull(replayed.getFailedTimestamp());

                OutboxEventEntity persistedReplay = repository.findByEventId(replayed.getEventId()).orElseThrow();
                assertEquals(replayed.getId(), persistedReplay.getId());
                assertEquals(originalEntity.getEventId(), persistedReplay.getOriginalEventId());
                assertEquals(OutboxEventStatus.PENDING, persistedReplay.getStatus());
                assertEquals(0L, persistedReplay.getRetryCount());

                OutboxEventEntity persistedOriginal = repository.findByEventId(original.getEventId()).orElseThrow();
                assertEquals(OutboxEventStatus.FAILED, persistedOriginal.getStatus());
                assertEquals(2L, persistedOriginal.getRetryCount());
                assertEquals("broker unavailable", persistedOriginal.getLastErrorMessage());
        }

        @Test
        @Transactional
        void recoverStaleProcessingEvents_reclaimsOnlyOldProcessingEvents() {
                Instant cutoff = Instant.parse("2026-01-01T12:00:00Z");
                OutboxEventEntity stale = event(UUID.randomUUID(), OutboxEventStatus.PROCESSING,
                                Instant.parse("2026-01-01T10:00:00Z"), null);
                stale.setProcessingStartedAt(Instant.parse("2026-01-01T11:00:00Z"));
                stale.setRetryCount(3L);
                stale = repository.save(stale);

                OutboxEventEntity recent = event(UUID.randomUUID(), OutboxEventStatus.PROCESSING,
                                Instant.parse("2026-01-01T10:01:00Z"), null);
                recent.setProcessingStartedAt(Instant.parse("2026-01-01T12:01:00Z"));
                recent = repository.save(recent);

                int recovered = repository.recoverStaleProcessingEvents(cutoff);

                assertEquals(1, recovered);
                OutboxEventEntity recoveredEvent = repository.findById(stale.getId()).orElseThrow();
                assertEquals(OutboxEventStatus.PENDING, recoveredEvent.getStatus());
                assertNull(recoveredEvent.getProcessingStartedAt());
                assertEquals(3L, recoveredEvent.getRetryCount());
                assertEquals(OutboxEventStatus.PROCESSING,
                                repository.findById(recent.getId()).orElseThrow().getStatus());
        }

        @Test
        void recordFailure_pendingRetryPersistsRetryMetadata() {
                OutboxEventEntity saved = repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING, Instant.now(), null));

                OutboxEventPersistenceAdapter adapter = new OutboxEventPersistenceAdapter(repository,
                                new OutboxEventMapper());
                adapter.recordFailure(saved.getId(),
                                OutboxEventStatus.PENDING,
                                1L,
                                "Failed",
                                Instant.now().plusSeconds(3),
                                Instant.now());
                Optional<OutboxEventEntity> entity = repository.findById(saved.getId());
                assertEquals(OutboxEventStatus.PENDING, entity.orElseThrow().getStatus());
                assertEquals(1L, entity.orElseThrow().getRetryCount());
                assertEquals("Failed", entity.orElseThrow().getLastErrorMessage());
                assertNotNull(entity.orElseThrow().getNextRetryTime());
            assertNotNull(entity.orElseThrow().getFailedTimestamp());

        }

        @Test
        void recordFailure_dlqPersistsFinalFailureState() {
                OutboxEventEntity saved = repository.save(event(
                                UUID.randomUUID(), OutboxEventStatus.PENDING, Instant.now(), null));

                OutboxEventPersistenceAdapter adapter = new OutboxEventPersistenceAdapter(repository,
                                new OutboxEventMapper());
                adapter.recordFailure(saved.getId(),
                                OutboxEventStatus.DLQ,
                                5L,
                                "Failed",
                                null,
                                Instant.now());
                Optional<OutboxEventEntity> entity = repository.findById(saved.getId());
                assertEquals(OutboxEventStatus.DLQ, entity.orElseThrow().getStatus());
                assertEquals(5L, entity.orElseThrow().getRetryCount());
                assertEquals("Failed", entity.orElseThrow().getLastErrorMessage());
                assertNull(entity.orElseThrow().getNextRetryTime());
            assertNotNull(entity.orElseThrow().getFailedTimestamp());

        }

        @Test
        @Transactional
        void findDueBatch_doesNotReturnDlqEvents() {
                repository.save(event(UUID.randomUUID(), OutboxEventStatus.DLQ, Instant.now(), null));

                List<OutboxEventEntity> due = repository.findDueBatch(
                                OutboxEventStatus.PENDING,
                                Instant.now(),
                                PageRequest.of(0, 10));

                assertTrue(due.isEmpty());
        }

        @Test
        void claimNextPendingEvent_concurrentClaimsReturnEventOnlyOnce() throws Exception {
                repository.save(event(UUID.randomUUID(), OutboxEventStatus.PENDING, Instant.now(), null));
                ExecutorService executorService = Executors.newFixedThreadPool(2);
                try {
                        Future<Optional<OutboxEvent>> first = executorService.submit(
                                        () -> outboxEventRepository.claimNextPendingEvent(Instant.now()));
                        Future<Optional<OutboxEvent>> second = executorService.submit(
                                        () -> outboxEventRepository.claimNextPendingEvent(Instant.now()));

                        List<Optional<OutboxEvent>> results = List.of(first.get(), second.get());
                        assertEquals(1, results.stream().filter(Optional::isPresent).count());
                        assertEquals(1, results.stream().filter(Optional::isEmpty).count());
                } finally {
                        executorService.shutdownNow();
                }

                OutboxEventEntity claimed = repository.findAll().stream().findFirst().orElseThrow();
                assertEquals(OutboxEventStatus.PROCESSING, claimed.getStatus());
                assertNotNull(claimed.getProcessingStartedAt());
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

        private static OutboxEventEntity event(UUID eventId, OutboxEventStatus status,
                        Instant createdAt, Instant nextAttemptAt) {
                return event(eventId, status, createdAt, nextAttemptAt, null);
        }

        private static OutboxEventEntity event(UUID eventId, OutboxEventStatus status,
                        Instant createdAt, Instant nextAttemptAt, UUID originalEventId) {
                return new OutboxEventEntity(
                                null,
                                eventId,
                                "{}",
                                createdAt,
                                null,
                                null,
                                originalEventId,
                                OutboxEventType.MENU_ITEM_CHANGED,
                                "101",
                                101L,
                                0L,
                                status,
                                0L,
                                nextAttemptAt,
                                null);
        }

}
