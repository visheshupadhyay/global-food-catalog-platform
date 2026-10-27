ALTER TABLE outbox_event
    DROP CONSTRAINT outbox_event_status_ck;

ALTER TABLE outbox_event
    ADD CONSTRAINT outbox_event_status_ck
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED', 'DLQ'));