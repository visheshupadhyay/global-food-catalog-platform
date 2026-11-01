ALTER TABLE outbox_event
    ADD COLUMN processing_started_at TIMESTAMPTZ;

ALTER TABLE outbox_event
    DROP CONSTRAINT outbox_event_status_ck;

ALTER TABLE outbox_event
    ADD CONSTRAINT outbox_event_status_ck
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DLQ'));
