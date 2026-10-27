package com.vishesh.foodcatalog.catalog.event.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;

/** Spring Data queries used to find and update outbox rows. */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
    Optional<OutboxEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    /**
     * Returns pending events that can be attempted now, oldest first.
     * Pageable supplies the batch limit so a worker does not load the whole table.
     */
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
}
