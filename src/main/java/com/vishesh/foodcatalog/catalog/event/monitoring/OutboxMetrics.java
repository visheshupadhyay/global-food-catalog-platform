package com.vishesh.foodcatalog.catalog.event.monitoring;

/** Read-only summary of the current outbox backlog and retry state. */
public final class OutboxMetrics {
    private final Long pending;
    private final Long processing;
    private final Long published;
    private final Long dlq;
    private final Long retryCount;
    private final Long oldestPendingAgeSeconds;

    public OutboxMetrics(Long pending,
        Long processing,
        Long published,
        Long dlq,
            Long retryCount,
            Long oldestPendingAgeSeconds) {
        if (pending == null || processing == null || published == null || dlq == null || retryCount == null
                || oldestPendingAgeSeconds == null || pending < 0 || processing < 0 || published < 0 || dlq < 0
                || retryCount < 0 || oldestPendingAgeSeconds < 0) {
            throw new IllegalArgumentException("Metric values cannot be null or negative");
        }
        this.pending = pending;
        this.processing = processing;
        this.published = published;
        this.dlq = dlq;
        this.retryCount = retryCount;
        this.oldestPendingAgeSeconds = oldestPendingAgeSeconds;
    }

    public Long getPending() {
        return pending;
    }

    public Long getProcessing() {
        return processing;
    }

    public Long getPublished() {
        return published;
    }

    public Long getDlq() {
        return dlq;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public Long getOldestPendingAgeSeconds() {
        return oldestPendingAgeSeconds;
    }

}
