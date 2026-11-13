ALTER TABLE menu_item_schedule
    ADD COLUMN schedule_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE menu_item_schedule
    ADD CONSTRAINT menu_item_schedule_schedule_version_ck
        CHECK (schedule_version >= 0);
