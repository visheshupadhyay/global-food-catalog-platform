package com.vishesh.foodcatalog.catalog.event.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

/** Spring Data queries used to find and update outbox rows. */
/** Database queries used to claim, retry, publish, and replay outbox rows safely. */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
    Optional<OutboxEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    /**
     * Returns pending events that can be attempted now, oldest first.
     * Pageable supplies the batch limit so a worker does not load the whole table.
     */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
            FROM OutboxEventEntity event
            WHERE event.status = :status
              AND (event.nextRetryTime IS NULL OR event.nextRetryTime <= :now)
            ORDER BY event.createdTimestamp ASC
            """)
    List<OutboxEventEntity> findDueBatch(
            @Param("status") OutboxEventStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity event
            SET
                event.status = com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus.PENDING,
                event.processingStartedAt = NULL
            WHERE event.status = 'PROCESSING'
              AND event.processingStartedAt IS NOT NULL
              AND event.processingStartedAt <= :now
            """)
    int recoverStaleProcessingEvents(@Param("now") Instant now);

    @Query("""
        SELECT count(*)
        FROM OutboxEventEntity event
        WHERE event.status = :status
        """)
    long countByStatus(@Param("status") OutboxEventStatus status);

    @Query("SELECT MIN(event.createdTimestamp) FROM OutboxEventEntity event WHERE event.status = :status")
    Optional<Instant> findOldestCreatedTimestamp(@Param("status") OutboxEventStatus status);

    @Query("SELECT COALESCE(SUM(event.retryCount), 0) FROM OutboxEventEntity event")
    long sumRetryCount();

}
