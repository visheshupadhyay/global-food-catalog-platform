ALTER TABLE outbox_event
    ADD COLUMN original_event_id UUID;

CREATE INDEX outbox_event_original_event_id_idx
    ON outbox_event (original_event_id);
