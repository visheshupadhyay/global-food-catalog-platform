ALTER TABLE outbox_event
    ADD COLUMN event_id UUID,
    ADD COLUMN event_type VARCHAR(100),
    ADD COLUMN kafka_key VARCHAR(200),
    ADD COLUMN aggregate_id BIGINT,
    ADD COLUMN aggregate_version BIGINT,
    ADD COLUMN payload JSONB,
    ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN retry_count BIGINT DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_error TEXT,
    ADD COLUMN created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN failed_at TIMESTAMPTZ;

ALTER TABLE outbox_event
    ADD CONSTRAINT outbox_event_status_ck
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    ADD CONSTRAINT outbox_event_retry_count_ck
        CHECK (retry_count IS NULL OR retry_count >= 0),
    ADD CONSTRAINT outbox_event_aggregate_version_ck
        CHECK (aggregate_version IS NULL OR aggregate_version >= 0);

CREATE UNIQUE INDEX outbox_event_event_id_uk
    ON outbox_event (event_id)
    WHERE event_id IS NOT NULL;
